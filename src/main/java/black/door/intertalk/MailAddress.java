package black.door.intertalk;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import javaslang.control.Try;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Created by nfischer on 9/11/2016.
 */
@JsonSerialize(using = ToStringSerializer.class)
@JsonDeserialize(using = MailAddress.MailAddressDeserializer.class)
public class MailAddress implements Comparable{
	final String local;
	final String domain;

	public MailAddress(String local, String domain) throws AddressException {
		this.local = requireNonNull(local).toLowerCase();
		this.domain = requireNonNull(domain).toLowerCase();
		new InternetAddress(local + '@' + domain, true);
	}

	public static Try<MailAddress> parse(String addr){
		requireNonNull(addr);
		int index = addr.lastIndexOf('@');
		return Try.of(() -> new MailAddress(addr.substring(0, index), addr.substring(index+1, addr.length())));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof MailAddress)) return false;

		MailAddress that = (MailAddress) o;

		if (!local.equalsIgnoreCase(that.local)) return false;
		return domain.equalsIgnoreCase(that.domain);

	}

	@Override
	public int hashCode() {
		int result = local.hashCode();
		result = 31 * result + domain.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return local + '@' + domain;
	}

	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(String.valueOf(o));
	}

	public static class MailAddressDeserializer extends StdDeserializer<MailAddress>{

		protected MailAddressDeserializer() {
			super(MailAddress.class);
		}

		@Override
		public MailAddress deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
			return parse(p.getText()).get();
		}
	}
}
