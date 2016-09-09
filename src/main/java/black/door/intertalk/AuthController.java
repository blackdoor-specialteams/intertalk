package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import javaslang.control.Try;
import lombok.val;
import spark.Request;
import spark.Response;

import java.security.Key;

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

	public void checkToken(Request req, Response res){
		val authzHeader = req.headers("Authorization");
		val token = authzHeader.replaceFirst("Bearer ", "");

		val jwt = Jwts.parser()
				.setSigningKeyResolver(new SigningKeyResolverAdapter() {
					public Key resolveSigningKey(JwsHeader header, Claims claims) {
						val alg = SignatureAlgorithm.forName(header.getAlgorithm());
						val kid = header.getKeyId();
						if("intertalk ref".equals(kid)) {
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

		if(req.attribute(CALLER_IS_PROVIDER)){
			// sender domain must be iss
			// sender must be sub
		}else{ // caller is user
			if(!message.from().equalsIgnoreCase(claims.getSubject()+ '@' + domain))
				halt(403, "You can only send messages as yourself ("+claims.getSubject()+")");
		}

		req.attribute(MessageController.MESSAGE, message);
	}
}
