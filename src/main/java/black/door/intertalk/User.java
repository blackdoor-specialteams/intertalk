package black.door.intertalk;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Created by nfischer on 9/6/2016.
 */
@ToString
@EqualsAndHashCode
public class User {
	public String handle;
	public byte[] password;
	public byte[] salt;
}
