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
	}


}
