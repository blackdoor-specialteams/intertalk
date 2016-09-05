package black.door.intertalk;

import javaslang.collection.Set;
import org.immutables.value.Value;

import javax.mail.internet.InternetAddress;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Created by nfischer on 9/5/2016.
 */
@Value.Immutable
public interface Message {
	Set<InternetAddress> to();
	InternetAddress from();
	OffsetDateTime sentAt();
	Optional<OffsetDateTime> receivedAt();
	String message();
	Optional<String> messageFormatted();
	Optional<String> format();
}
