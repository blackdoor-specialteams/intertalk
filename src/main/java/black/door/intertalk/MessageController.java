package black.door.intertalk;

import black.door.intertalk.jooq.tables.records.MessagesRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import javaslang.collection.HashSet;
import javaslang.control.Try;
import lombok.SneakyThrows;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import spark.Request;
import spark.Response;

import javax.mail.internet.AddressException;
import java.sql.Connection;
import java.sql.Timestamp;

import static black.door.intertalk.AuthController.CALLER_IS_PROVIDER;
import static black.door.intertalk.AuthController.CLAIMS;
import static black.door.intertalk.Main.domain;
import static java.time.OffsetDateTime.now;
import static spark.Spark.halt;

/**
 * Created by nfischer on 9/5/2016.
 */
public class MessageController {

	public static final String MESSAGE = "MESSAGE";

	private final ObjectMapper mapper;
	private final Connection connection;
	private final DSLContext create;

	public MessageController(ObjectMapper mapper, Connection connection) {
		this.mapper = mapper;
		this.connection = connection;
		create = DSL.using(connection);
	}

	@SneakyThrows(AddressException.class)
	public Object receiveMessage(Request req, Response res){
		boolean callerIsProvider = req.attribute(CALLER_IS_PROVIDER);
		val claims = (Claims) req.attribute(CLAIMS);
		Message message;

		val tryMessage = Try.of(() -> mapper.readValue(req.bodyAsBytes(), Message.class));
		if(tryMessage.isFailure()){
			res.status(400);
			return "bad message object";
		}
		message = tryMessage.get();

		if (req.attribute(CALLER_IS_PROVIDER)) {
			// sender domain must be iss
			// sender must be sub
			halt(501, "intertalk provider sending not yet supported");
		} else { // caller is user
			if (!message.from().equals(new MailAddress(claims.getSubject(), domain)))
				halt(403, "You can only send messages as yourself (" + claims.getSubject() + ")");
			if(! message.to().contains(new MailAddress(claims.getSubject(), domain)))
				halt(403, "You can't send messages to conversations you aren't in");
		}
		req.attribute(MessageController.MESSAGE, message);

		if(!callerIsProvider){
			message = ((ImmutableMessage)message)
					.withSentAt(now());
		}

		message = ((ImmutableMessage)message)
				.withReceivedAt(now());

		// persist message

		MessagesRecord record = new MessagesRecord(); // todo bind record mapper
		record.setTo(message.to().map(MailAddress::toString).toJavaArray(String.class));
		record.setFrom(message.from().toString());
		record.setSentAt(Timestamp.from(message.sentAt().toInstant()));
		record.setReceivedAt(Timestamp.from(message.receivedAt().get().toInstant()));
		record.setMessage(message.message());
		message.messageFormatted().ifPresent(record::setMessageFormatted);
		message.format().ifPresent(record::setFormat);

		create.executeInsert(record);


		SubscriberController.notifyUsers(HashSet.ofAll(message.to()), message);

		/*
		if(!callerIsProvider){
			// send message to other providers
		}
		*/

		res.status(201);
		return "Sent";
	}



}
