package black.door.intertalk;

import com.fasterxml.jackson.databind.ObjectMapper;
import javaslang.control.Try;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by nfischer on 9/11/2016.
 */
public class MailAddressTest {
	@Test
	public void parse() throws Exception {
		Try<MailAddress> basic = MailAddress.parse("bob@gmail.com");
		assertTrue(basic.isSuccess());
		assertEquals("bob", basic.get().local);
		assertEquals("gmail.com", basic.get().domain);
		assertEquals("bob@gmail.com", basic.get().toString());

		assertTrue(MailAddress.parse("@gmail.com").isFailure());
		assertTrue(MailAddress.parse("jim@").isFailure());

		assertTrue(MailAddress.parse("bob@bob@bobby.bob").isFailure());

		System.out.println(MailAddress.parse("\"very.unusual.@.unusual.com\"@example.com"));
		System.out.println(MailAddress.parse("(comment)nfischer@gmail.com"));

		ObjectMapper mapper = new ObjectMapper();
		assertEquals(basic.get(), mapper.readValue(mapper.writeValueAsString(basic.get()), MailAddress.class));
	}

}