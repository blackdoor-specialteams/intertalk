package black.door.intertalk;

import black.door.intertalk.jooq.tables.records.MessagesRecord;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.kag0.ruid.RUID;
import javaslang.collection.Stream;
import javaslang.collection.TreeSet;
import javaslang.control.Try;
import org.immutables.value.Value;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Created by nfischer on 9/5/2016.
 */
@Value.Immutable
@JsonSerialize(as = ImmutableMessage.class)
@JsonDeserialize(as = ImmutableMessage.class)
public interface Message {
	Optional<RUID> id();
	TreeSet<MailAddress> to();
	MailAddress from();
	OffsetDateTime sentAt();
	Optional<OffsetDateTime> receivedAt();
	String message();
	Optional<String> messageFormatted();
	Optional<String> format();

	default MessagesRecord toRecord(){
		MessagesRecord record = new MessagesRecord();
		record.setTo(this.to().map(MailAddress::toString).toJavaArray(String.class));
		record.setFrom(this.from().toString());
		record.setSentAt(Timestamp.from(this.sentAt().toInstant()));
		this.receivedAt()
				.map(OffsetDateTime::toInstant)
				.map(Timestamp::from)
				.ifPresent(record::setReceivedAt);
		record.setMessage(this.message());
		this.messageFormatted().ifPresent(record::setMessageFormatted);
		this.format().ifPresent(record::setFormat);
		this.id().map(RUID::rawBytes).ifPresent(record::setId);
		return record;
	}

	static ImmutableMessage fromRecord(MessagesRecord record){
		return ImmutableMessage.builder()
				.to(TreeSet.ofAll(Stream.of(record.getTo()).map(MailAddress::parse).<MailAddress>map(Try::get)))
				.from(MailAddress.parse(record.getFrom()).get())
				.sentAt(OffsetDateTime.ofInstant(record.getSentAt().toInstant(), ZoneOffset.UTC))
				.receivedAt(Optional.ofNullable(record.getReceivedAt()).map(t -> OffsetDateTime.ofInstant(t.toInstant(), ZoneOffset.UTC)))
				.message(record.getMessage())
				.messageFormatted(Optional.ofNullable(record.getMessageFormatted()))
				.format(Optional.ofNullable(record.getFormat()))
				.id(new RUID(record.getId()))
				.build();
	}
}
