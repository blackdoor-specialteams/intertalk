package black.door.intertalk;

import javaslang.control.Try;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static black.door.intertalk.Main.mapper;

/**
 * Created by nfischer on 9/5/2016.
 */
@WebSocket
public class SubscriberController {

	public static final Map<InternetAddress, Set<Session>> SUBSCRIBERS =
			new ConcurrentHashMap<>();

	private InternetAddress user;

	@OnWebSocketConnect
	public void connected(Session session){
	}

	@OnWebSocketClose
	public void closed(Session session, int statusCode, String reason){
		SUBSCRIBERS.get(user).remove(session);
	}

	@OnWebSocketMessage
	public void message(Session session, String message) throws IOException {
		Set<Session> sessions = SUBSCRIBERS.get(user);
		if(sessions == null) {
			sessions = new ConcurrentSkipListSet<>();
			SUBSCRIBERS.put(user, sessions);
		}
		sessions.add(session);
	}

	public static javaslang.collection.Set<Try<Void>> notifyUsers(javaslang.collection.Set<InternetAddress> users,
	                                                              Message message){
		return users.flatMap(SUBSCRIBERS::get).filter(Session::isOpen).map(s ->
			Try.run(() -> s.getRemote().sendString(mapper.writeValueAsString(message)))
		);
	}
}
