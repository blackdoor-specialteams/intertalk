package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariDataSource;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import javaslang.Function3;
import javaslang.jackson.datatype.JavaslangModule;
import lombok.val;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.mail.internet.AddressException;
import javax.sql.DataSource;
import java.security.Key;
import java.sql.Connection;
import java.util.function.Supplier;

import static spark.Spark.*;

/**
 * Created by nfischer on 9/5/2016.
 */
public class Main {

	public static final ObjectMapper mapper = new ObjectMapper()
			.registerModule(new Jdk8Module())
			.registerModule(new JavaslangModule())
			.registerModule(new JavaTimeModule());

	public static void main(String[] args) throws AddressException {
		val conf = ConfigFactory.load();
		val jdbcUrl = conf.getString("intertalk.db.jdbc");
		val jdbcUser = conf.getString("intertalk.db.user");
		val jdbcPass = conf.getString("intertalk.db.password");
		val tokenKey = MacProvider.generateKey(SignatureAlgorithm.ES512);

		val hikari = new HikariDataSource();
		hikari.setJdbcUrl(jdbcUrl);
		hikari.setUsername(jdbcUser);
		hikari.setPassword(jdbcPass);

		Supplier<KeyController> keyControllerSupplier = KeyController::new;
		Supplier<AuthController> authControllerSupplier = () -> new AuthController(mapper);

		webSocket("/messages", SubscriberController.class);
		before("/messages", (req, res) -> authControllerSupplier.get().checkToken(req, res));
		post("/messages", buildMessageController(mapper, hikari, MessageController::receiveMessage));

		post("/token", (req, res) -> buildLoginController(hikari, tokenKey, LoginController::login));

		get("/keys", keyControllerSupplier.get().listKeys);
		get("/keys/:kid", keyControllerSupplier.get().retrieveKey);

	}

	private static Route buildLoginController(DataSource ds,
	                                          Key tokenKey,
	                                          Function3<
			                                          LoginController,
			                                          Request,
			                                          Response,
			                                          Object>
			                                          method){
		return (req, res) -> {
			try(Connection connection = ds.getConnection()){
				return method.apply(new LoginController(connection, tokenKey), req, res);
			}
		};
	}

	private static Route buildMessageController(ObjectMapper mapper,
	                                            DataSource hikari,
	                                            Function3<
			                                            MessageController,
			                                            Request,
			                                            Response,
			                                            Object>
			                                            method){
		return (req, res) -> {
			try(Connection connection = hikari.getConnection()){
				return method.apply(new MessageController(mapper, connection), req, res);
			}
		};
	}

}
