package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kag0.oauth2.TokenResponse;
import com.github.kag0.oauth2.TokenType;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import io.github.kag0.oauth2.password.ImmutablePasswordTokenRequest;
import javaslang.collection.HashSet;
import lombok.SneakyThrows;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketTextListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.flywaydb.core.Flyway;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static spark.Spark.stop;

/**
 * Created by nfischer on 9/11/16.
 */
public class MessageControllerTest {

	@Before
	public void setUp() throws Exception {

			Flyway f = new Flyway();

			Main.main(new String[0]);
			f.setDataSource(Main.hikari);
			f.clean();
			f.migrate();

	}

	@Test
	public void test() throws UnirestException, IOException, ExecutionException, InterruptedException {
		Unirest.post("http://localhost:4567/users")
				.body("{\n" +
						"  \"username\": \"jim\",\n" +
						"  \"password\": \"pass\"\n" +
						"}")
				.asString();

		// login
		HttpResponse<String> loginResponse = Unirest.post("http://localhost:4567/token")
				.body(ImmutablePasswordTokenRequest.builder().password("pass").username("jim").build().toFormEncoded())
				.asString();

		TokenResponse tokenResponse = TokenResponse.fromJson(new ObjectMapper().readTree(loginResponse.getBody()));

		List<Message> messages = new LinkedList<>();

		DefaultAsyncHttpClient c = new DefaultAsyncHttpClient();
		WebSocket websocket = c.prepareGet("ws://localhost:4567/messageStream")
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
				.from("jim@localhost")
				.to(HashSet.of("alice@ecorp.com", "jim@localhost"))
				.message("hello world")
				.sentAt(OffsetDateTime.now())
				.build();

		HttpResponse<String> messageSendResponse = Unirest.post("http://localhost:4567/messages")
				.header("Authorization", "Bearer " + tokenResponse.accessToken())
				.body(Main.mapper.writeValueAsString(m))
				.asString();
		System.out.println(messageSendResponse.getBody());
		assertEquals(201, messageSendResponse.getStatus());

		Thread.sleep(200);

		System.out.println(messages);
		assertTrue(messages.stream().anyMatch(ms -> ms.message().equals(m.message())));
	}

	@After
	public void tearDown() throws Exception {
		stop();
	}
}