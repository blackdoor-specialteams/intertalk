package black.door.intertalk;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.kag0.oauth2.*;
import io.github.kag0.oauth2.password.PasswordTokenRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.val;
import org.bouncycastle.crypto.generators.BCrypt;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import spark.Request;
import spark.Response;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static black.door.intertalk.jooq.Tables.USERS;

/**
 * Created by nfischer on 9/6/2016.
 */
public class LoginController {

	private final DSLContext create;
	private final Key tokenKey;

	public LoginController(Connection connection, Key tokenKey) {
		create = DSL.using(connection);
		this.tokenKey = tokenKey;
	}

	public JsonNode login(Request req, Response res){
		Optional<? extends TokenRequest> tokenRequestOption =
				TokenRequest.parseEncoded(req.body());
		final Optional<User> userOption;


		if(!tokenRequestOption.isPresent() || !(tokenRequestOption.get() instanceof TokenRequest)){
			return ImmutableErrorResponse
					.of(ErrorType.StdErrorType.invalid_request)
					.toJson();
		}

		val request = (PasswordTokenRequest) tokenRequestOption.get();
		userOption = create.select()
				.from(USERS)
				.where(USERS.HANDLE.equalIgnoreCase(request.username()))
				.fetchOptionalInto(User.class);

		if(! userOption.isPresent()) {
			BCrypt.generate(
					new byte[16]
					,new byte[16]
					,16
			);
			return ImmutableErrorResponse.of(ErrorType.StdErrorType.invalid_grant).toJson();
		}
		val user = userOption.get();

		if(Arrays.equals(
				user.password,
				BCrypt.generate(
						request.password().getBytes(StandardCharsets.UTF_8)
						,user.salt
						,16
				)
		)){
			val now = Instant.now();
			val _12HoursFromNow = now.plus(Duration.ofHours(12));
			val token = Jwts.builder()
					.setSubject(user.handle)
					.setNotBefore(Date.from(now))
					.setIssuedAt(Date.from(now))
					.setExpiration(Date.from(_12HoursFromNow))
					.signWith(SignatureAlgorithm.HS512, tokenKey)
					.compact();

			res.header("Cache-Control", "no-store");
			res.header("Pragma", "no-cache");

			return ImmutableTokenResponse.builder()
					.accessToken(token)
					.exipresIn(Duration.between(now, _12HoursFromNow).getSeconds())
					.tokenType(TokenType.StdTokenType.Bearer)
					.build().toJson();
		}else{
			return ImmutableErrorResponse.of(ErrorType.StdErrorType.invalid_grant).toJson();
		}
	}
}
