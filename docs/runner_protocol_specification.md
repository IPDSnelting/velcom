# Specification for the runner communication protocol

This protocol is used for communication between a backend and a runner.
It is based on sending JSON packets and `.tar` files across a websocket connection.
Packets can be either commands or command replies.

## Packet format

Packets follow the format
```
{
    "type": <type>,
    "data": <data>
}
```
* `<type>` is the type of the packet (as string).
* `<data>` is a JSON object containing the rest of the packet data.

## Error handling

If any side detects invalid behaviour, it must close the connection.
If the runner loses connection, it may attempt to reconnect.
If the runner hasn't reconnected successfully within 10 minutes, the backend should consider it permanently disconnected.

## Initiating a connection

The client initiates the connection with the backend using the normal websocket handshake.
This initial request contains three special required headers:

| Name | Content | Description |
|------|---------|-------------|
| `Runner-Token` | the runner's token | If the token is invalid, the backend will deny the connection attempt. |
| `Runner-Name` | the runner's unique name | If the name is not unique, the backend will deny the connection attempt. |
| `Runner-Info` | the runner's system information | |

If the server denied the connection attempt, its reply will include the following header:

| Name | Content | Description |
|------|---------|-------------|
| `Runner-Deny` | either `TOKEN` or `NAME` | The reason why the connection was denied. |

There may be situations where a runner has lost connection and is trying to reconnect, but the backend has not yet realized that the connection has been closed.
In those situations, the runner's connection attempt may be rejected because its name is not unique.
The affected runner may need to wait for a bit and try connecting again later.

Performing authentication this early allows the backend to easily block invalid runners without wasting
any resources allocating a persistent websocket connection and waiting for the client to authenticate itself.
It also simplifies runner handling, as *every* connected runner is already properly authorized to execute
benchmarks.
The HTTP headers are already encrypted by virtue of using wss/https.

## Ping

Ping messages are quite special as websockets send their packets in-order. If we were to send pings as
normal packets, a large TAR file would block the ping packets for multiple seconds to minutes. This might
cause the server or runner to detect a timeout and close the connection.

To avoid this the websocket protocol has a special frame type for ping and pong messages. These are
sent out-of-band and therefore do not conflict with normal frames, *if the other frames are not too big*.
A large single frame may still block the ping from going through, so the backend is *required* to chunk their
tar files appropriately.

## Commands

Command and reply names follow a fixed schema:
If the command is named `name`, then the reply is named `name_reply`.

Commands are exclusive to either the runner or the backend.
There exist *no* commands that can be sent by both sides.

The timeout for replies is 10 seconds.
If no reply has been received within 10 seconds, this constitutes invalid behaviour and the connection must be closed.

## Backend commands

The following schemata depict only the `<data>` part of the packet.

### `get_status`

This command should be sent frequently (e. g. every 10 seconds) by the backend.

**Command data:** none

**Reply data:**
| Name | Type | Description |
|------|------|-------------|
| `bench_hash` | string | (Optional) The current hash of the benchmark repo version the runner is using |
| `result_available` | bool | The result of the last run is available to download |
| `status` | string | One of `"RUN"`, `"ABORT"` or `"IDLE"` according to the runner's current status |
| `run_id` | string | (Optional) If the runner is executing a run for this backend, contains the corresponding run id |

### `get_result`

**Command data:** none

**Reply data:**
| Name | Type | Description |
|------|------|-------------|
| `run_id` | string | The id of the run the data is for |
| `success` | bool | Whether the runner successfully executed the bench script and could parse its output |
| `result` | object | If `success` is `true`, the bench script's output (see below) |
| `error` | string | If `success` is `false`, a string explaining what went wrong |

If no result was available, this constitutes invalid behaviour and the connection should be closed.

The `result` field does *not* follow the format from the [benchmark script output specification](Benchmark-Repo-Specification).
Instead, it uses a format that is easier to serialize and deserialize.

### `clear_result`

**Command data:** none

**Reply data:** none

If no result was available, this constitutes invalid behaviour and the connection should be closed.

### `abort_run`

**Command data:** none

**Reply data:** none

This command is only effective if it was sent from the same backend that started the currently running run.
If it was sent from a different backend or no run is currently active, this command should be ignored.
The command should also be ignored if the server is still transmitting the next task's `tar` file.

## Runner commands

### `request_run`

When the backend receives this command, it first replies normally.
Depending on the reply, it then sends 0, 1 or 2 binary websocket packets containing uncompressed `.tar` files.

**Command data:** none

**Reply data:**
| Name | Type | Description |
|------|------|-------------|
| `bench` | bool | Whether the backend will send an updated version of the benchmark repo |
| `bench_hash` | string | If `bench` is `true`, the hash of the benchmark repo version |
| `run` | bool | Whether the backend will send a run |
| `run_id` | string | If `run` is `true`, the id of the run |

If both `bench` and `run` are `true`, the benchmark repo will be sent before the run.
