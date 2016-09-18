package black.door.intertalk;

import black.door.intertalk.jooq.tables.records.MessagesRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import javaslang.collection.HashSet;
import javaslang.collection.Stream;
import javaslang.collection.TreeSet;
import javaslang.control.Try;
import lombok.SneakyThrows;
import lombok.val;
import org.jooq.DSLContext;
import spark.Request;
import spark.Response;

import javax.mail.internet.AddressException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static black.door.intertalk.AuthController.CALLER_IS_PROVIDER;
import static black.door.intertalk.AuthController.CLAIMS;
import static black.door.intertalk.Main.domain;
import static black.door.intertalk.jooq.Tables.MESSAGES;
import static java.time.OffsetDateTime.now;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javaslang.API.*;
import static javaslang.Patterns.Failure;
import static javaslang.Patterns.Success;
import static spark.Spark.halt;

/**
 * Created by nfischer on 9/5/2016.
 */
public class MessageController {

	public static final String MESSAGE = "MESSAGE";

	private final ObjectMapper mapper;
	private final DSLContext create;

	public MessageController(ObjectMapper mapper, DSLContext create) {
		this.mapper = mapper;
		this.create = create;
	}

	@SneakyThrows({AddressException.class, JsonProcessingException.class})
	public Object listConversations(Request req, Response _){
		if(req.attribute(CALLER_IS_PROVIDER))
			halt(403, "why is a provider trying to get conversations?");
		val caller = new MailAddress(((Claims) req.attribute(CLAIMS)).getSubject(), domain);

		return mapper.writeValueAsString(create
				.selectDistinct(MESSAGES.TO)
				.from(MESSAGES)
				.where(MESSAGES.TO.contains(new String[]{caller.toString()}))
				.orderBy(MESSAGES.RECEIVED_AT.desc())
				.stream()
				.map(r -> HashSet.of(r.value1()))
				.collect(toSet()));
	}

	@SneakyThrows(AddressException.class)
	public Object listConversationMessages(Request req, Response res){
		if(req.attribute(CALLER_IS_PROVIDER))
			halt(403, "why is a provider trying to get conversations?");
		val caller = new MailAddress(((Claims) req.attribute(CLAIMS)).getSubject(), domain);
		val since = Try.of(() -> OffsetDateTime.parse(req.queryParams("before")).toInstant()).getOrElse(Instant.now());
		val limit = Try.of(() -> Integer.valueOf(req.params("limit"))).getOrElse(200);

		Try<TreeSet<MailAddress>> tryConvo = Try.of(() -> mapper.readValue(
				new String(
						Base64.getUrlDecoder().decode(req.params(":convo")),
						StandardCharsets.UTF_8),
				new TypeReference<TreeSet<MailAddress>>() {
				}
		));

		return Match(tryConvo).of(
			Case(Success($()), participants -> {

				if(!participants.contains(caller))
					halt(403, "you're not in that conversation");

				return (Object) mapper.valueToTree(create.select()
						.from(MESSAGES)
						.where(MESSAGES.TO.eq(
								participants.map(MailAddress::toString).toJavaArray(String.class)
						))
						.and(MESSAGES.RECEIVED_AT.lessThan(Timestamp.from(since)))
						.limit(limit)
						.stream()
						.map(r -> r.into(MessagesRecord.class))
						.map(r -> ImmutableMessage.builder()
								.to(TreeSet.ofAll(Stream.of(r.getTo()).map(p -> MailAddress.parse(p).get())))
								.from(MailAddress.parse(r.getFrom()).get())
								.sentAt(OffsetDateTime.ofInstant(r.getSentAt().toInstant(), ZoneOffset.UTC))
								.receivedAt(Optional.ofNullable(r.getReceivedAt()).map(Timestamp::toInstant).map(i -> OffsetDateTime.ofInstant(i, ZoneOffset.UTC)))
								.message(r.getMessage())
								.messageFormatted(Optional.ofNullable(r.getMessageFormatted()))
								.format(Optional.ofNullable(r.getFormat()))
								.build()
						)
						.collect(toList()));
			}),
			Case(Failure($()), error -> {
				res.status(400);
				return (Object) "bad convo id";
			})
		);
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
