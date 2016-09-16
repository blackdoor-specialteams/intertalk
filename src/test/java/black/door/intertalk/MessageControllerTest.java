package black.door.intertalk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kag0.oauth2.TokenResponse;
import com.github.kag0.oauth2.password.ImmutablePasswordTokenRequest;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import javaslang.collection.TreeSet;
import lombok.SneakyThrows;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketTextListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.flywaydb.core.Flyway;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.mail.internet.AddressException;
import java.io.IOException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;

/**
 * Created by nfischer on 9/11/16.
 */
public class MessageControllerTest {

	int port = 9869;

	@BeforeClass
	public static void beforeClass(){
		Main.main(new String[0]);
	}

	@Before
	public void setUp() throws Exception {

			Flyway f = new Flyway();

			//Main.main(new String[0]);
			f.setDataSource(Main.hikari);
			f.clean();
			f.migrate();

	}

	@Test
	public void test() throws UnirestException, IOException, ExecutionException, InterruptedException {
		Unirest.post("http://localhost:9869/users")
				.body("{\n" +
						"  \"username\": \"jim\",\n" +
						"  \"password\": \"pass\"\n" +
						"}")
				.asString();

		// login
		HttpResponse<String> loginResponse = Unirest.post("http://localhost:9869/token")
				.body(ImmutablePasswordTokenRequest.builder().password("pass").username("jim").build().toFormEncoded())
				.asString();

		TokenResponse tokenResponse = TokenResponse.fromJson(new ObjectMapper().readTree(loginResponse.getBody()));

		List<Message> messages = new LinkedList<>();

		DefaultAsyncHttpClient c = new DefaultAsyncHttpClient();
		WebSocket websocket = c.prepareGet("ws://localhost:9869/messageStream")
				.execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(
						new WebSocketTextListener() {

							@Override
							@SneakyThrows
							public void onMessage(String message) {
								messages.add(Main.mapper.readValue(message, Message.class));
							}

							@Override
							public void onOpen(WebSocket websocket) {
								websocket.sendMessage(tokenResponse.accessToken());
							}

							@Override
							public void onClose(WebSocket websocket) {
								fail();
							}

							@Override
							public void onError(Throwable t) {
								fail(String.valueOf(t));
							}
						}).build()).get();

		Message m = ImmutableMessage.builder()
				.from(MailAddress.parse("jim@localhost").get())
				.to(TreeSet.of(MailAddress.parse("alice@ecorp.com").get(), MailAddress.parse("jim@localhost").get()))
				.message("hello world")
				.sentAt(OffsetDateTime.now())
				.build();

		HttpResponse<String> messageSendResponse = Unirest.post("http://localhost:9869/messages")
				.header("Authorization", "Bearer " + tokenResponse.accessToken())
				.body(Main.mapper.writeValueAsString(m))
				.asString();
		System.out.println(messageSendResponse.getBody());
		assertEquals(201, messageSendResponse.getStatus());

		Thread.sleep(200);

		System.out.println(messages);
		assertTrue(messages.stream().anyMatch(ms -> ms.message().equals(m.message())));
	}

	@Test
	public void testMessageHistory() throws SQLException, UnirestException, IOException, AddressException {
		Unirest.post(String.format("http://localhost:%d/users", port))
				.body("{\n" +
						"  \"username\": \"jim\",\n" +
						"  \"password\": \"pass\"\n" +
						"}")
				.asString();
		Unirest.post(String.format("http://localhost:%d/users", port))
				.body("{\n" +
						"  \"username\": \"bob\",\n" +
						"  \"password\": \"pass\"\n" +
						"}")
				.asString();

		HttpResponse<String> loginResponse = Unirest.post(String.format("http://localhost:%d/token", port))
				.body(ImmutablePasswordTokenRequest.builder().password("pass").username("jim").build().toFormEncoded())
				.asString();

		MailAddress jim = new MailAddress("jim", "localhost");
		MailAddress bob = new MailAddress("bob", "localhost");

		TokenResponse tokenResponse = TokenResponse.fromJson(new ObjectMapper().readTree(loginResponse.getBody()));
		Set<Message> messages = Stream.generate(() -> ImmutableMessage.builder()
				.from(jim)
				.to(TreeSet.of(jim, bob))
				.sentAt(OffsetDateTime.now())
				.message("hihi" + Math.random())
				.build()
		).limit(10).collect(toSet());

		for(Message m : messages){
			HttpResponse<String> r = Unirest.post(String.format("http://localhost:%d/messages", port))
					.header("Authorization", tokenResponse.accessToken())
					.body(Main.mapper.writeValueAsString(m))
					.asString();
			assertEquals(201, r.getStatus());
		}


		loginResponse = Unirest.post(String.format("http://localhost:%d/token", port))
				.body(ImmutablePasswordTokenRequest.builder().password("pass").username("bob").build().toFormEncoded())
				.asString();
		TokenResponse tokenResponse2 = TokenResponse.fromJson(new ObjectMapper().readTree(loginResponse.getBody()));


		HttpResponse<String> historyResponse = Unirest.get(String.format("http://localhost:%d/messages/%s", port, Base64.getUrlEncoder().encodeToString(Main.mapper.writeValueAsBytes(new MailAddress[]{jim, bob}))))
				.header("Authorization", tokenResponse2.accessToken())
				.asString();
		System.out.println(historyResponse.getBody());
		Set<Message> parsedMessages = Main.mapper.readValue(historyResponse.getBody(), new TypeReference<Set<Message>>() {});

		messages.forEach(m -> assertTrue(parsedMessages.stream().anyMatch(pm -> pm.message().equals(m.message()))));
	}

	@After
	public void tearDown() throws Exception {
		//stop();
	}
}