package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariDataSource;
import io.jsonwebtoken.impl.crypto.MacProvider;
import javaslang.Function3;
import javaslang.jackson.datatype.JavaslangModule;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.sql.DataSource;
import java.security.Key;
import java.sql.Connection;
import java.util.function.Supplier;

import static spark.Spark.*;

/**
 * Created by nfischer on 9/5/2016.
 */
public class Main {

	static String domain;
	static HikariDataSource hikari;

	public static final ObjectMapper mapper = new ObjectMapper()
			.registerModule(new Jdk8Module())
			.registerModule(new JavaslangModule())
			.registerModule(new JavaTimeModule());

	public static void main(String[] args){
		Logger logger = LoggerFactory.getLogger(black.door.intertalk.Main.class);
		val conf = ConfigFactory.load();
		val jdbcUrl = conf.getString("intertalk.db.jdbc");
		val jdbcUser = conf.getString("intertalk.db.user");
		val jdbcPass = conf.getString("intertalk.db.password");
		val tokenKey = MacProvider.generateKey();
		domain = conf.getString("intertalk.domain");
		SubscriberController.tokenKey = tokenKey;

		logger.info("Starting server for domain " + domain + " with database at " + jdbcUrl);

		hikari = new HikariDataSource();
		hikari.setJdbcUrl(jdbcUrl);
		hikari.setUsername(jdbcUser);
		hikari.setPassword(jdbcPass);

		Supplier<KeyController> keyControllerSupplier = KeyController::new;
		Supplier<AuthController> authControllerSupplier = () -> new AuthController(mapper, tokenKey, domain);

		webSocket("/messages", SubscriberController.class);
		before("/messages", (req, res) -> authControllerSupplier.get().checkToken(req, res));
		post("/messages", buildMessageController(mapper, hikari, MessageController::receiveMessage));

		post("/users", buildLoginController(hikari, tokenKey, UserController::createUser));

		post("/token", buildLoginController(hikari, tokenKey, UserController::login));

		get("/keys", keyControllerSupplier.get().listKeys);
		get("/keys/:kid", keyControllerSupplier.get().retrieveKey);

		exception(RuntimeException.class, (exception, request, response) -> {
			logger.error("exception in controller", exception);
			response.status(500);
		});

	}

	private static Route buildLoginController(DataSource ds,
	                                          Key tokenKey,
	                                          Function3<
			                                          UserController,
			                                          Request,
			                                          Response,
			                                          Object>
			                                          method){
		return (req, res) -> {
			try(Connection connection = ds.getConnection()){
				return method.apply(new UserController(connection, tokenKey, mapper), req, res);
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
