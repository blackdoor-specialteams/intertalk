# intertalk

[![Docker Automated build](https://img.shields.io/docker/automated/blackdoor/intertalk.svg?maxAge=2592000)](https://hub.docker.com/r/blackdoor/intertalk/) [![](https://images.microbadger.com/badges/image/blackdoor/intertalk.svg)](https://microbadger.com/images/blackdoor/intertalk "Get your own image badge on microbadger.com")

A simple standard for allowing users of different messaging services to talk to 
one another while still allowing the service providers to offer rich features and 
clients the way they want.

## Standard

### Terminology

#### Providers

Providers are identified by a domain name which they own and control. Providers 
should have a TLS certificate for this domain.

#### Users

Users are identified by a standard internet mail address (`<local part>@<domain part>`) 
where the domain part of the address is a provider's domain.

#### Rooms

Unlike direct messages between a fixed set of users, rooms are persistant conversations that users can join or leave. User joining rooms may be able to see the conversation before they joined. Rooms are identified like `<room name>#<domain>` (as such it is forbidden for a room name to contain a `#`). 

### Message Format

```json
{
  "to": ["alice@ecorp.com", "bob@chat.allsafe.io"],
  "from": "alice@ecorp.com",
  "sentAt": "2016-09-05T15:45:39Z",
  "message": "Where are you?",
  "messageFormatted": "Where _are_ you?",
  "format": "text/markdown",
  "id": "9uZIuRrbPRl71PiRAmUID9xd"
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

#### `id`

An optionally included universally unique string (a [ruid](https://github.com/kag0/ruid) 
might be a good choice) to be used as an idempotent id. 
This is used to prevent double sending, delivery, and display of a message.

#### `room`

The above message format is for direct messages. When a message is intended for a room rather than a set of users there are some slight changes. In a room message, the `to` field is absent, and a `room` field is required. The `room` field specifies which room this message was meant to be sent to. All other fields are the same.

### Endpoints

To support intertalk, a provider needs to support only 3 endpoints (which must 
be reachable from the providers identifying domain).

#### `POST /messages`

The endpoint where other providers will send messages destined for users.

#### `POST /rooms/messages` _should use the room name? like /rooms{/roomName}/messages or something?_

The endpoint where other providers will send room messages (where the sending provider is one who hosts a room's member, and the recieving provider hosts the room itself).

#### `GET /keys`

An endpoint which returns a [JWK set](https://tools.ietf.org/html/rfc7517#section-5) 
of all valid keys for this provider.

### Sending messages between providers

Send messages to the `/messages` endpoint of the recieving provider. Only send one message per recieving provider (regardless of how many recipeients the conversation has). So in a conversation `["alice@ecorp.com", "bob@chat.allsafe.io", "jane@ecorp.com"]`, when bob sends a message, the `chat.allsafe.io` provider should only make one call to `ecorp.com`.

### Room behavior

#### Messages 

Sending messages with rooms is a two-step process. When a provider (let's say `chat.allsafe.io`) identifies that one of their users (let's say `bob@chat.allsafe.io`) wants to send a message to a room (let's say `execs#ecorp.com`), then `chat.allsafe.io` composes and signs a room message, and sends it to `ecorp.com/roomMessages`. This is step one. `ecorp.com` then looks up all members of `execs#ecorp.com` (which it is responsible for maintaining) and sends messages to the providers for each of those members. This is step two, member's providers can now notify users that there is a new message in the room.

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
`{iss}{/keys}` where `iss` is the `iss` field from the JWT claim. If the returned JKS contains a valid key for the 
signature on the JWT then the request is authenticated. If the request to `{iss}{/keys}`
is not done over authenticated HTTPS, the recieving provider SHOULD notify the 
user when displaying the message to them (the recieving provider MAY choose not to accept the message at all).

## Reference Implementation

### User creation
```json
POST /users
{
  "username": "jim",
  "password": "pass"
}
```

### Login

Log in to the refrerence provider with [OAuth2 password grant](https://tools.ietf.org/html/rfc6749#section-4.3.2) (`POST` to `/token`).

### Sending Messages

Send messages with `POST` to `/messages`. The body should be a message object like the one in the intertalk standard. Include the token from the login in your request (according to bearer token usage, in the `Authorization` header).

### Recieving Messages

Establish a websocket connection to `/messageStream`. As soon as you connect send the token from the login. New messages in the conversation will be sent to you as they arrive. All messages will come over this socket (including ones you send).
 
