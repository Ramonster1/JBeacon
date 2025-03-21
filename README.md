# JBeacon

## Overview
JBeacon is a <b>Java network polling library</b> implemented using the Java NIO package. JBeacon supports blocking & non-blocking network polling at a consistent rate using the ScheduledExecutorService.
Currently, only UDP polling is supported with further features coming soon.

The Java NIO package is a modern, feature-rich networking library that offers better performance than the traditional IO package. However, it also has some increased complexity. The goal of JBeacon is take away some of the complexity of network polling whilst keeping the performance gains. However, some knowledge of NIO, especially ByteBuffers, will still be helpful.

JBeacon uses JDK 21.

---

## Features

- **Scheduling**: Uses `ScheduledExecutorService` to execute polling to run after a given delay, or to execute periodically.
- **UDP Communication**: Uses `DatagramChannel` to send and receive messages via UDP.
- **Blocking and Non-blocking Modes**: Configurable behavior to execute polling in either blocking or non-blocking mode.
- **ByteBuffer management**: JBeacon automatically prepares ByteBuffers for filling and draining (reading and writing), so you don't have to.  
- **Pluggability**: Leverages the `PollResponseCommand` interface to execute custom logic when receiving network responses (which implements the Command behavioral design pattern).
- **Logging**: Utilizes Apache Log4j2 for comprehensive logging at various levels. Currently logs in async mode by default.

---

## Usage

### UdpPollingService

`UdpPollingService` is a Java-based implementation of the `PollingService` interface, designed to handle UDP-based polling operations. This service can operate in both blocking and non-blocking modes using Java NIO and the `DatagramChannel` for efficient network communication. It supports sending requests and processing responses dynamically. This service implements the Builder creational design pattern using Lombok for ease of use and flexibility.

#### Building the Polling Service

Use the builder pattern to construct an instance of `UdpPollingService`.
Example:
```java
UdpPollingService pollingService = UdpPollingService.builder()
    .serverSocketAddress(new InetSocketAddress("example.com", 5000))
    .inBuffer(ByteBuffer.allocate(1024)) // To store the response data
    .outBuffer(ByteBuffer.wrap("Poll Request".getBytes())) // To store the request data
    .pollResponseCommand(new CustomPollResponseCommand()) // Your custom implementation here to process the response data
    .blocks(true) // Blocking mode enabled
    .build();
```
#### Configuration
Below are the key fields and configurations:

| Field Name              | Description                                                                | Required     | Default Value |
|-------------------------|----------------------------------------------------------------------------|--------------|---------------|
| `serverSocketAddress`   | Target UDP server's address and port.                                     | **Yes**      | N/A           |
| `inBuffer`              | ByteBuffer for incoming data.                                             | **Yes**      | N/A           |
| `outBuffer`             | ByteBuffer for outgoing data.                                             | **Yes**      | N/A           |
| `blocks`                | Determines blocking (`true`) or non-blocking (`false`) mode.              | **No**       | `true`        |
| `pollResponseCommand`   | Defines the custom logic to process the received data.                    | **Yes**      | N/A           |
| `pollSelector`          | Required for non-blocking mode for handling selector operations.          | **Required** | `null`        |
| `scheduledExecutor`     | Optional; useful for scheduling tasks in the background.                  | No           | `null`        |

---

#### Polling

Call the `poll()` method to initiate polling:
```java
pollingService.poll();
```

This method will:
1. Open a UDP channel and optionally configure it for non-blocking I/O.
2. Send a request to the configured server using a `ByteBuffer`.
3. Depending on the mode:
    - **Blocking mode**: Block the thread while waiting for a response synchronously.
    - **Non-blocking mode**: Register the channel with a `Selector` and rely on `pollSelector.process()` for asynchronous response handling.
4. Invoke the `PollResponseCommand.execute(ByteBuffer)` method to process the incoming data.

---

### Scheduling a polling service

Use the builder pattern to construct an instance of `PollSchedulingService`
Example:
```java
try (PollSchedulingService schedulingService = PollSchedulingService.builder() // implements AutoClosable
		.period(10L)
		.timeUnit(TimeUnit.SECONDS)
		.pollingService(pollingService)
		.build()) {

		schedulingService.executePeriodically();

		Thread.sleep(1_000_000L);
}
```

---

### Custom Response Command

Implement `PollResponseCommand` to define your custom processing logic for incoming responses:
```java
public class CustomPollResponseCommand implements PollResponseCommand {
    @Override
    public void execute(ByteBuffer buffer) {
        String response = new String(buffer.array(), StandardCharsets.UTF_8);
        System.out.println("Response received: " + response);
    }
}
```

---

## License

This module is free to use and extend. Ensure dependencies comply with their respective licenses.

---

Happy Polling! 🚀
