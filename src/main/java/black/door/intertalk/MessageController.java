package black.door.intertalk;

import black.door.intertalk.jooq.tables.records.MessagesRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import spark.Request;
import spark.Response;

import java.sql.Connection;
import java.sql.Timestamp;

import static black.door.intertalk.AuthController.CALLER_IS_PROVIDER;
import static java.time.OffsetDateTime.now;

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

	public Object receiveMessage(Request req, Response res){
		Message message = req.attribute(MESSAGE);

		boolean callerIsProvider = req.attribute(CALLER_IS_PROVIDER);

		if(callerIsProvider){
			message = ((ImmutableMessage)message)
					.withReceivedAt(now());
		}else {
			message = ((ImmutableMessage)message)
					.withSentAt(now());
		}


		// persist message
		val finalMessage = message;
		create.transaction(configuration -> {
			MessagesRecord record = new MessagesRecord(); // todo bind record mapper
			record.setTo(finalMessage.to().toString()); // todo sort first
			record.setFrom(finalMessage.from());
			record.setSentAt(Timestamp.from(finalMessage.sentAt().toInstant()));
			record.setReceivedAt(Timestamp.from(finalMessage.receivedAt().get().toInstant()));
			record.setMessage(finalMessage.message());
			finalMessage.messageFormatted().ifPresent(record::setMessageFormatted);
			finalMessage.format().ifPresent(record::setFormat);

			DSL.using(configuration)
					.executeInsert(record);
					//.newRecord(Tables.MESSAGES, finalMessage)
					//.store();
		});

		SubscriberController.notifyUsers(message.to(), message);

		/*
		if(!callerIsProvider){
			// send message to other providers
		}
		*/

		res.status(201);
		return "Sent";
	}



}
