# JBeacon

## Overview
JBeacon is a <b>Java network polling library</b> designed to simplify the complexities of network polling while delivering enhanced performance through the Java NIO package. This library supports both blocking and non-blocking network polling, and is equipped with Aeron messaging and scheduling capabilities. Currently, JBeacon focuses on UDP polling, with additional features anticipated in future updates.

In the realm of network applications, polling is a crucial mechanism, particularly for tasks such as live price feeds and status monitoring.

The Java NIO package, known for its superior performance compared to traditional IO methods, introduces a level of complexity that JBeacon seeks to mitigate. While the library significantly streamlines network polling, familiarity with NIO, especially concerning ByteBuffers and Selectors, remains advantageous for developers. The library user will need to pass their own ByteBuffer objects to the library, so knowledge of ByteBuffers is required to correctly allocate the ByteBuffer capacity, and whether to use a direct or indirect buffer. However, JBeacon does manage the filling and draining of the buffer.

Moreover, JBeacon seamlessly integrates with the <b>Aeron library</b>, enabling the transmission of response data to an Aeron publication. The Aeron library is well-regarded for its <b>low-latency</b> and <b>reliable message transport</b> capabilities across UDP unicast, UDP multicast, and IPC channels.

Utilising JDK 21, JBeacon is positioned as a robust tool for developers seeking an efficient and effective approach to network polling. The library not only enhances performance but also reduces the inherent complexities, thus empowering developers to focus on building innovative applications.

---
## Declare JBeacon as a dependency
JBeacon is publicly hosted on the Maven Central Repository. Follow [this link](https://central.sonatype.com/artifact/io.github.ramonster1/jbeacon/overview) for more details on how to use JBeacon in your project.

Apache Maven snippet:
```maven
<dependency>
   <groupId>io.github.ramonster1</groupId>
      <artifactId>jbeacon</artifactId>
   <version>1.0.0</version>
</dependency>
```


---

## Features

- **Scheduling**: Uses `ScheduledExecutorService` to execute polling after a given delay, or to execute periodically.
- **UDP Communication**: Uses `DatagramChannel` to send and receive messages via UDP.
- **Blocking and Non-blocking Modes**: Configurable behavior to execute polling in either blocking or non-blocking mode.
- **ByteBuffer management**: JBeacon automatically prepares ByteBuffers for filling and draining (writing and reading), so you don't have to.  
- **Pluggability**: Leverages the `PollResponseCommand` functional interface to execute custom logic when receiving network responses (which implements the Command behavioral design pattern).
- **Logging**: Utilizes Apache Log4j2 for comprehensive logging at various levels. Currently logs in async mode by default.
- **Aeron integration**: Uses the AeronOnPollResponse commands to either send response data to a publication using the Publication <i>offer()</i> method or the lower-latency <i>tryClaim()</i> method.

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
### Aeron Integration

Use the `AeronOnPollResponseOfferCommand` & `AeronOnPollResponseTryClaimCommand` to send response data to an Aeron Publication:
```java
UdpPollingService pollingService = UdpPollingService.builder()
        .serverSocketAddress(new InetSocketAddress("example.com", 5000))
        .inBuffer(ByteBuffer.allocate(1024)) // To store the response data
        .outBuffer(ByteBuffer.wrap("Poll Request".getBytes())) // To store the request data
        .pollResponseCommand(new AeronOnPollResponseTryClaimCommand(publication, bufferClaim))
        .blocks(true)
        .build();
```

## Planned features

* TCP support using AsynchronousSocketChannel
* Retry UDP request if response is lost in transmission
* Provide Aeron backpressure strategies


---

## License

This module is free to use and extend. Ensure dependencies comply with their respective licenses.

---

Happy Polling! 🚀
