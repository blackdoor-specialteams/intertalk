package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import lombok.val;
import spark.Request;
import spark.Response;

import java.security.Key;
import java.util.Optional;

import static spark.Spark.halt;

/**
 * Created by nfischer on 9/5/2016.
 */
public class AuthController {
	public static final String CALLER_IS_PROVIDER = "CALLER_IS_PROVIDER";
	public static final String CLAIMS = "INTERTALK_CLAIMS";

	private final ObjectMapper mapper;
	private final Key tokenKey;
	private final String domain;

	public AuthController(ObjectMapper mapper, Key tokenKey, String domain) {
		this.mapper = mapper;
		this.tokenKey = tokenKey;
		this.domain = domain;
	}

	public void checkToken(Request req, Response res){
		if (req.requestMethod().equalsIgnoreCase("OPTIONS"))
			return;

		val tokenOption = Optional.ofNullable(req.headers("Authorization"))
				.map(authzHeader -> authzHeader.replaceFirst("Bearer ", ""));
		if (!tokenOption.isPresent()) {
			halt(401, "log in");
		}
		try {
			Jws<Claims> jwt = Jwts.parser()
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
					.parseClaimsJws(tokenOption.get());
			req.attribute(CLAIMS, jwt.getBody());
		} catch (JwtException e) {
			halt(401);
		}
	}
}
