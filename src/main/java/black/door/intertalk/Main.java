package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariDataSource;
import io.jsonwebtoken.impl.crypto.MacProvider;
import javaslang.Function3;
import javaslang.jackson.datatype.JavaslangModule;
import lombok.val;
import org.flywaydb.core.Flyway;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import javax.sql.DataSource;
import java.security.Key;
import java.sql.Connection;
import java.util.function.Function;
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
			.registerModule(new JavaTimeModule())
			.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

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

		port(conf.getInt("intertalk.port"));
		if(conf.hasPath("intertalk.keystore.password"))
			secure("keystore.jks", conf.getString("intertalk.keystore.password"), null, null);

		webSocket("/messageStream", SubscriberController.class);
		authIt(authControllerSupplier,
				"/messages"
				,"/messages/*"
				,"/conversations"
		);
		get("/conversations", buildMessageController(mapper, hikari, MessageController::listConversations));
		get("/messages/:convo", buildMessageController(mapper, hikari, MessageController::listConversationMessages));
		post("/messages", buildMessageController(mapper, hikari, MessageController::receiveMessage));

		post("/users", buildUserController(hikari, tokenKey, UserController::createUser));

		post("/token", buildUserController(hikari, tokenKey, UserController::login));

		get("/keys", keyControllerSupplier.get().listKeys);
		get("/keys/:kid", keyControllerSupplier.get().retrieveKey);

		get("/hello", (q, r) -> "hello");

		enableCORS("*", "POST, GET, OPTIONS", "*");

		exception(RuntimeException.class, (exception, request, response) -> {
			logger.error("exception in controller", exception);
			response.status(500);
			response.body("o0pz");
		});

		pool(conf);
		if(conf.getBoolean("intertalk.automigrate"))
			migrate();
	}

	private static void authIt(Supplier<AuthController>authControllerSupplier, String... paths){
		for(String path: paths)
			before(path, (req, res) -> authControllerSupplier.get().checkToken(req, res));
	}

	private static void migrate(){
		Flyway f = new Flyway();
		f.setDataSource(hikari);
		f.migrate();
	}

	private static Route buildUserController(DataSource ds,
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
				return method.apply(new MessageController(mapper, DSL.using(connection, SQLDialect.POSTGRES)), req, res);
			}
		};
	}

	private static <C extends AutoCloseable, R> R autoClosing(C closeable, Function<C, R> fn){
		try {
			try(C c  = closeable){
				return fn.apply(c);
			}
		} catch (Exception e) {
			if(e instanceof RuntimeException)
				throw (RuntimeException) e;
			throw new RuntimeException(e);
		}
	}

	private static void pool(Config conf){
		if(conf.hasPath("intertalk.threadmult")) {
			int mult = conf.getInt("intertalk.threadmult");
			int cores = Runtime.getRuntime().availableProcessors();
			threadPool(cores * mult, cores, 60000);
		}
	}

	// Enables CORS on requests. This method is an initialization method and should be called once.
	private static void enableCORS(final String origin, final String methods, final String headers) {

		options("/*", (request, response) -> {

			String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
			if (accessControlRequestHeaders != null) {
				response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
			}
			String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
			if (accessControlRequestMethod != null) {
				response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
			}

			response.header("Access-Control-Max-Age", "86400");

			return "OK";
		});

		before((request, response) -> {
			response.header("Access-Control-Allow-Origin", origin);
			response.header("Access-Control-Request-Method", methods);
			response.header("Access-Control-Allow-Headers", headers);
			// Note: this may or may not be necessary in your particular application
			response.type("application/json");
		});
	}
}
