CREATE TABLE messages (
	id                  BIGSERIAL   PRIMARY KEY,
	to                  VARCHAR     NOT NULL,
	from                VARCHAR     NOT NULL,
	sent_at             TIMESTAMPTZ NOT NULL,
	received_at         TIMESTAMPTZ NOT NULL,
	message             TEXT        NOT NULL,
	message_formatted   TEXT,
	format              VARCHAR
);

CREATE TABLE users (
	handle      VARCHAR PRIMARY KEY,
	password	bytea   NOT NULL,
	salt        bytea   NOT NULL
);