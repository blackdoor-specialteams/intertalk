package black.door.intertalk;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javaslang.collection.TreeSet;
import org.immutables.value.Value;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Created by nfischer on 9/5/2016.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMessage.class)
@JsonDeserialize(as = ImmutableMessage.class)
public interface Message {
	//Optional<RUID> id();
	TreeSet<MailAddress> to();
	MailAddress from();
	OffsetDateTime sentAt();
	Optional<OffsetDateTime> receivedAt();
	String message();
	Optional<String> messageFormatted();
	Optional<String> format();
}
