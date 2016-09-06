/**
 * This class is generated by jOOQ
 */
package black.door.intertalk.jooq.tables;


import black.door.intertalk.jooq.Keys;
import black.door.intertalk.jooq.Public;
import black.door.intertalk.jooq.tables.records.MessagesRecord;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.Identity;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.8.4"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Messages extends TableImpl<MessagesRecord> {

    private static final long serialVersionUID = -1514059396;

    /**
     * The reference instance of <code>public.messages</code>
     */
    public static final Messages MESSAGES = new Messages();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<MessagesRecord> getRecordType() {
        return MessagesRecord.class;
    }

    /**
     * The column <code>public.messages.id</code>.
     */
    public final TableField<MessagesRecord, Long> ID = createField("id", org.jooq.impl.SQLDataType.BIGINT.nullable(false).defaultValue(org.jooq.impl.DSL.field("nextval('messages_id_seq'::regclass)", org.jooq.impl.SQLDataType.BIGINT)), this, "");

    /**
     * The column <code>public.messages.to</code>.
     */
    public final TableField<MessagesRecord, String> TO = createField("to", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>public.messages.from</code>.
     */
    public final TableField<MessagesRecord, String> FROM = createField("from", org.jooq.impl.SQLDataType.VARCHAR.nullable(false), this, "");

    /**
     * The column <code>public.messages.sent_at</code>.
     */
    public final TableField<MessagesRecord, Timestamp> SENT_AT = createField("sent_at", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

    /**
     * The column <code>public.messages.received_at</code>.
     */
    public final TableField<MessagesRecord, Timestamp> RECEIVED_AT = createField("received_at", org.jooq.impl.SQLDataType.TIMESTAMP.nullable(false), this, "");

    /**
     * The column <code>public.messages.message</code>.
     */
    public final TableField<MessagesRecord, String> MESSAGE = createField("message", org.jooq.impl.SQLDataType.CLOB.nullable(false), this, "");

    /**
     * The column <code>public.messages.message_formatted</code>.
     */
    public final TableField<MessagesRecord, String> MESSAGE_FORMATTED = createField("message_formatted", org.jooq.impl.SQLDataType.CLOB, this, "");

    /**
     * The column <code>public.messages.format</code>.
     */
    public final TableField<MessagesRecord, String> FORMAT = createField("format", org.jooq.impl.SQLDataType.VARCHAR, this, "");

    /**
     * Create a <code>public.messages</code> table reference
     */
    public Messages() {
        this("messages", null);
    }

    /**
     * Create an aliased <code>public.messages</code> table reference
     */
    public Messages(String alias) {
        this(alias, MESSAGES);
    }

    private Messages(String alias, Table<MessagesRecord> aliased) {
        this(alias, aliased, null);
    }

    private Messages(String alias, Table<MessagesRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Public.PUBLIC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<MessagesRecord, Long> getIdentity() {
        return Keys.IDENTITY_MESSAGES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<MessagesRecord> getPrimaryKey() {
        return Keys.MESSAGES_PKEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<MessagesRecord>> getKeys() {
        return Arrays.<UniqueKey<MessagesRecord>>asList(Keys.MESSAGES_PKEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Messages as(String alias) {
        return new Messages(alias, this);
    }

    /**
     * Rename this table
     */
    public Messages rename(String name) {
        return new Messages(name, null);
    }
}
