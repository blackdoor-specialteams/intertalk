# intertalk

A simple standard for allowing users of different messaging services to talk to 
one another while still allowing the service providers to offer rich features and 
clients the way they want.

## Standard

### Namespace

#### Providers

Providers are identified by a domain name which they own and control. Providers 
should have a TLS certificate for this domain.

#### Users

Users are identified by a standard internet mail address (`<local part>@<domain part>`) 
where the domain part of the address is a provider's domain.

### Message Format

```json
{
  "to": ["alice@ecorp.com", "bob@chat.allsafe.io"],
  "from": "alice@ecorp.com",
  "sentAt": "2016-09-05T15:45:39Z",
  "message": "Where are you?",
  "messageFormatted": "Where _are_ you?",
  "format": "text/markdown"
}
```

#### `to`

A set of user IDs identifying the participants of the chat. Intertalk doesn't 
differentiate between a one-on-one chat and a group chat. 

#### `from`

The sender of the message. `from`∈`to`

#### `sentAt`

An ISO 8601 timestamp indicating when the message was sent. The receiving provider 
should interpret this to mean the time at which the sender sent the message (as 
opposed to when the provider received the message). The sending provider may 
interpret this however is suitable for their service (when the user hit the send 
button, when the provider received the message from the user, when the provider 
sent the message to the receiving provider, etc.).

#### `message`

The plain message with no special formatting (except as needed by JSON).

#### `messageFormatted`

An optional field offering a duplicate of the message but with additional formatting 
or styling which a user client can use to display the message.

#### `format`

A MIME type for the formatting of `messageFormatted`, such as `text/markdown` or 
`text/html`. Binary types should have Base 64 encoded values in the `messageFormatted`
field.

### Endpoints

To support intertalk, a provider needs to support only 3 endpoints (which must 
be reachable from the providers identifying domain).

#### `POST /messages`

The endpoint where other providers will send new messages.

#### `GET /keys`

An endpoint which returns a [JWK set](https://tools.ietf.org/html/rfc7517#section-5) 
of all valid keys for this provider.

#### `GET /keys{/kid}`

An endpoint which returns a JWK with matching `kid` field if the provider has such a key,
otherwise 404.

### Sending messages between providers

Send messages to the `/messages` endpoint of the recieving provider. Only send one message per recieving provider (regardless of how many recipeients the conversation has). So in a conversation `["alice@ecorp.com", "bob@chat.allsafe.io", "jane@ecorp.com"]`, when bob sends a message, the `chat.allsafe.io` provider should only send one message to `ecorp.com`.

### Authentication

#### Server Authentication

Server authentication is used by a sending provider to confirm the validity of a 
receiving provider.
Providers send messages to the domain of a user's id (eg. `chat.allsafe.io/messages`).
Receiving providers SHOULD provide HTTPS and a server certificate on their domain.
If a receiving provider offers a certificate, the sending provider MUST validate it.
If a receiving provider does not have a certificate the sending provider SHOULD 
notify the user before the message is sent.

#### Client Authentication

Client authentication is used by a receiving provider to confirm the validity of a 
sending provider. 
When sending a message, the sender constructs a JWT which is sent in the 
`Authorization` header of the request as a `Bearer` token.
The JWT will have claims like this:

```json
{
  "aud": "chat.allsafe.io",
  "sub": "alice@ecorp.com",
  "iss": "ecorp.com",
  "exp": 1473099681
}
```

where `aud` is the domain of the receiving provider, `sub` is the sending user, 
and `iss` is the domain of the sending provider. Senders may also include other 
jwt fields like `exp`, `nbf`, `iat`, and `jti`. Receiving providers must verify 
that their domain is the one listed in `aud`, that `iss` matches the domain of `sub`,
and that `sub` matches the `from` field in the message. Receiving providers must 
also confirm the signing key on the JWT. This is done by placing a `GET` request to 
`{iss}{/kid}` where `iss` is the `iss` field from the JWT claim, and `kid` is the 
`kid` field from the JWT header. If a key is returned, and it is valid for the 
signature on the JWT then the request is authenticated. If the request to `{iss}{/kid}`
is not done over authenticated HTTPS, the recieving provider SHOULD notify the 
user when displaying the message to them.

## Reference Implementation

### Login

Log in to the refrerence provider with OAuth2 password grant (`POST` to `\token`).

### Sending Messages

Send messages with `POST` to `\messages`. The body should be a message object like the one in the intertalk standard. Include the token from the login in your request (according to bearer token usage, in the `Authorization` header).

### Recieving Messages

Establish a websocket connection to `\messages`. As soon as you connect send the token from the login. New messages in the conversation will be sent to you as they arrive. All messages will come over this socket (including ones you send).
