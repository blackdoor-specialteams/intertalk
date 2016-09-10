package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.mail.internet.AddressException;
import java.util.function.Supplier;

import static spark.Spark.*;

/**
 * Created by nfischer on 9/5/2016.
 */
public class Main {

	public static final ObjectMapper mapper = new ObjectMapper();

	public static void main(String[] args) throws AddressException {
		Supplier<MessageController> messageControllerSupplier =
				() -> new MessageController(mapper);
		Supplier<KeyController> keyControllerSupplier = KeyController::new;
		Supplier<AuthController> authControllerSupplier = AuthController::new;

		webSocket("/messages", SubscriberController.class);
		before("/messages", authControllerSupplier.get().checkToken);
		post("/messages", messageControllerSupplier.get().receiveMessage);

		get("/keys", keyControllerSupplier.get().listKeys);
		get("/keys/:kid", keyControllerSupplier.get().retrieveKey);

		enableCORS("*", "POST, GET, OPTIONS", "*");
	}

	// Enables CORS on requests. This method is an initialization method and should be called once.
	private static void enableCORS(final String origin, final String methods, final String headers) {

		options("/*", (request, response) -> {

			String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
			if (accessControlRequestHeaders != null) {
				response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
			}

			String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
			if (accessControlRequestMethod != null) {
				response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
			}

			return "OK";
		});

		before((request, response) -> {
			response.header("Access-Control-Allow-Origin", origin);
			response.header("Access-Control-Request-Method", methods);
			response.header("Access-Control-Allow-Headers", headers);
			// Note: this may or may not be necessary in your particular application
			response.type("application/json");
		});
	}
}
