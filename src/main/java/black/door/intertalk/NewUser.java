package black.door.intertalk;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Created by nfischer on 9/7/2016.
 */
@Value.Immutable
@JsonDeserialize(as = ImmutableNewUser.class)
public interface NewUser {
	String username();
	char[] password();
}
