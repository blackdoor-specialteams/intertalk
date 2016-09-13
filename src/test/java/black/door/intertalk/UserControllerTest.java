package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kag0.oauth2.TokenResponse;
import com.github.kag0.oauth2.TokenType;
import com.github.kag0.oauth2.password.ImmutablePasswordTokenRequest;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.flywaydb.core.Flyway;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static spark.Spark.stop;

/**
 * Created by nfischer on 9/10/16.
 */
public class UserControllerTest {

	@Before
	public void setUp(){
		Flyway f = new Flyway();

		Main.main(new String[0]);
		f.setDataSource(Main.hikari);
		f.clean();
		f.migrate();
	}

	@Test
	public void test() throws Exception {
		// create user
		HttpResponse<String> createResponse = Unirest.post("http://localhost:4567/users")
				.body("{\n" +
						"  \"username\": \"jim\",\n" +
						"  \"password\": \"pass\"\n" +
						"}")
				.asString();
		assertEquals(201, createResponse.getStatus());

		// login
		HttpResponse<String> loginResponse = Unirest.post("http://localhost:4567/token")
				.body(ImmutablePasswordTokenRequest.builder().password("pass").username("jim").build().toFormEncoded())
				.asString();

		TokenResponse tokenResponse = TokenResponse.fromJson(new ObjectMapper().readTree(loginResponse.getBody()));
		assertEquals(TokenType.StdTokenType.Bearer, tokenResponse.tokenType());
	}

	@After
	public void cleanUp(){
		stop();
	}
}
