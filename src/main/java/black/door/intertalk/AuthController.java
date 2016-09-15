package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import javaslang.control.Try;
import lombok.val;
import spark.Request;
import spark.Response;

import javax.mail.internet.AddressException;
import java.security.Key;
import java.util.Optional;

import static spark.Spark.halt;

/**
 * Created by nfischer on 9/5/2016.
 */
public class AuthController {
	public static final String CALLER_IS_PROVIDER = "CALLER_IS_PROVIDER";

	private final ObjectMapper mapper;
	private final Key tokenKey;
	private final String domain;

	public AuthController(ObjectMapper mapper, Key tokenKey, String domain) {
		this.mapper = mapper;
		this.tokenKey = tokenKey;
		this.domain = domain;
	}

	public void checkToken(Request req, Response res) throws AddressException {
		if(req.requestMethod().equalsIgnoreCase("OPTIONS"))
			return;

		val tokenOption = Optional.ofNullable(req.headers("Authorization"))
				.map(authzHeader -> authzHeader.replaceFirst("Bearer ", ""));
		if(!tokenOption.isPresent()){
			halt(401, "log in");
		}

		val token = tokenOption.get();
		try {
			val jwt = Jwts.parser()
					.setSigningKeyResolver(new SigningKeyResolverAdapter() {
						public Key resolveSigningKey(JwsHeader header, Claims claims) {
							val alg = SignatureAlgorithm.forName(header.getAlgorithm());
							val kid = header.getKeyId();
							if ("intertalk ref".equals(kid)) {
								req.attribute(CALLER_IS_PROVIDER, false);
								return tokenKey;
							}
							throw new JwtException("can't find key");
						}
					})
					// we must be the audience
					.parseClaimsJws(token);
			val claims = jwt.getBody();

			val tryMessage = Try.of(() -> mapper.readValue(req.bodyAsBytes(), Message.class));
			val message = tryMessage.get();

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
		}catch (JwtException e){
			halt(401);
		}
	}
}
