package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import spark.Request;
import spark.Response;

import java.io.IOException;

/**
 * Created by nfischer on 9/5/2016.
 */
public class AuthController {
	public static final String CALLER_IS_PROVIDER = "CALLER_IS_PROVIDER";

	private final ObjectMapper mapper;

	public AuthController(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	public void checkToken(Request req, Response res){
		try {
			req.attribute(MessageController.MESSAGE, mapper.readValue(req.bodyAsBytes(), Message.class));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
