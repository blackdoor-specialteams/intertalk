package black.door.intertalk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kag0.oauth2.*;
import io.github.kag0.oauth2.password.PasswordTokenRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import javaslang.control.Try;
import lombok.val;
import org.bouncycastle.crypto.generators.BCrypt;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import spark.Request;
import spark.Response;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
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
public class UserController {

	private final DSLContext create;
	private final Key tokenKey;
	private final ObjectMapper mapper;

	public UserController(Connection connection, Key tokenKey, ObjectMapper mapper) {
		create = DSL.using(connection);
		this.tokenKey = tokenKey;
		this.mapper = mapper;
	}

	public Object createUser(Request req, Response res){
		val tryNewUser = Try.of(() -> mapper.readValue(req.bodyAsBytes(), NewUser.class));
		if(tryNewUser.isFailure()){
			res.status(400);
			return "bad user object";
		}
		val newUser = tryNewUser.get();
		byte[] salt = new byte[16];
		new SecureRandom().nextBytes(salt);
		User user = new User();
		user.handle = newUser.username();
		val buf = StandardCharsets.UTF_8.encode(CharBuffer.wrap(newUser.password()));
		val pw = new byte[buf.limit()];
		buf.get(pw);
		user.password = BCrypt.generate(pw, salt, 16);
		buf.clear();
		buf.put(new byte[pw.length]);
		Arrays.fill(newUser.password(), (char)0);

		res.status(201);
		return "created";
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
					.setHeaderParam("kid", "intertalk ref")
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
