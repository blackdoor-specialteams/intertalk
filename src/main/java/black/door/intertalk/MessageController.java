package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import spark.Route;

import static black.door.intertalk.AuthController.CALLER_IS_PROVIDER;
import static java.time.OffsetDateTime.now;

/**
 * Created by nfischer on 9/5/2016.
 */
public class MessageController {

	public static final String MESSAGE = "MESSAGE";

	private ObjectMapper mapper;

	public MessageController(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public Route receiveMessage = (req, res) -> {
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

		if(! callerIsProvider){
			// send message to other providers
		}

		SubscriberController.notifyUsers(message.to(), message);

		res.status(201);
		return "Sent";
	};


}
