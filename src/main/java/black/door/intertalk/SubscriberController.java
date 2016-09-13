package black.door.intertalk;

import io.jsonwebtoken.ClaimJwtException;
import io.jsonwebtoken.Jwts;
import javaslang.control.Try;
import lombok.val;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.security.Key;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static black.door.intertalk.Main.mapper;

/**
 * Created by nfischer on 9/5/2016.
 */
@WebSocket
public class SubscriberController {

	static Key tokenKey;
	public static final Map<String, Set<Session>> SUBSCRIBERS =
			new ConcurrentHashMap<>();

	private String user;

	@OnWebSocketConnect
	public void connected(Session session) throws IOException {
		//session.getRemote().sendString("hello");
	}

	@OnWebSocketClose
	public void closed(Session session, int statusCode, String reason){
		SUBSCRIBERS.get(user).remove(session);
	}

	@OnWebSocketMessage
	public void message(Session session, String message) throws IOException {
		try {
			val claims = Jwts.parser()
					.setSigningKey(tokenKey)
					.parseClaimsJws(message)
					.getBody();
			user = claims.getSubject();

			Set<Session> sessions = SUBSCRIBERS.get(user);
			if(sessions == null) {
				sessions = Collections.synchronizedSet(new HashSet<>());
				val sz = SUBSCRIBERS.putIfAbsent(user, sessions);
				if(sz != null)
					sessions = sz;
			}
			sessions.add(session);
		}catch (ClaimJwtException e){
			session.close(4001, "bad token");
		}
	}

	public static javaslang.collection.Set<Try<Void>> notifyUsers(
			javaslang.collection.Set<MailAddress> users,
			Message message){
		return users/*todo filter out other providers, map mail address to local part*/
				.filter(u -> u.domain.equalsIgnoreCase(Main.domain))
				.map(u -> u.local)
				.flatMap(h -> SUBSCRIBERS.getOrDefault(h, new HashSet<>()))
				.filter(s -> s != null)
				.filter(Session::isOpen).map(s ->
			Try.run(() -> s.getRemote().sendString(mapper.writeValueAsString(message)))
		);
	}
}
