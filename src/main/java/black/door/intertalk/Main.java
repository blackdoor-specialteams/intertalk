package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariDataSource;
import javaslang.jackson.datatype.JavaslangModule;
import lombok.val;
import spark.Route;

import javax.mail.internet.AddressException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.function.Function;
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

		val hikari = new HikariDataSource();
		hikari.setJdbcUrl(jdbcUrl);
		hikari.setUsername(jdbcUser);
		hikari.setPassword(jdbcPass);

		Function<Function<MessageController, Route>, Route> messageControllerFun = fn ->
			(req, res) -> {
				try(Connection connection = hikari.getConnection()){
					return fn.apply(new MessageController(mapper, connection));
				}
			};

		Supplier<KeyController> keyControllerSupplier = KeyController::new;
		Supplier<AuthController> authControllerSupplier = () -> new AuthController(mapper);

		webSocket("/messages", SubscriberController.class);
		before("/messages", (req, res) -> authControllerSupplier.get().checkToken(req, res));
		post("/messages", buildMessageController(mapper, hikari));

		get("/keys", keyControllerSupplier.get().listKeys);
		get("/keys/:kid", keyControllerSupplier.get().retrieveKey);

	}

	private static Route buildMessageController(ObjectMapper mapper, DataSource hikari){
		return (req, res) -> {
			try(Connection connection = hikari.getConnection()){
				return new MessageController(mapper, connection).receiveMessage(req, res);
			}
		};
	}

}
