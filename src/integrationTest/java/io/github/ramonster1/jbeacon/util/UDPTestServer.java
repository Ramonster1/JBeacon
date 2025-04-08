package io.github.ramonster1.jbeacon.util;

import lombok.Getter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPTestServer implements AutoCloseable {
	public static final Charset CHARSET = Charset.defaultCharset();
	private static final int TIMEOUT = Math.toIntExact(TimeUnit.SECONDS.toMillis(10));
	@Getter
	private final DatagramSocket socket;
	private final AtomicBoolean running = new AtomicBoolean(true);
	@Getter
	private byte[] data;
	@Getter
	private String date;

	public UDPTestServer() throws SocketException {
		this.socket = new DatagramSocket(0);
		socket.setSoTimeout(TIMEOUT);
		updateTestServerResponse();
	}

	public void startServer() {
		while (!Thread.currentThread().isInterrupted()) {
			var request = new DatagramPacket(new byte[1], 1);

			try {
				// Block thread until datagram is received
				socket.receive(request);

				DatagramPacket packet = new DatagramPacket(data, data.length, request.getAddress(), request.getPort());
				socket.send(packet);
			} catch (SocketException e) {
				if (!running.get()) {
					// SocketException is expected on socket close, break the loop
					break;
				} else {
					// Re-throw unexpected SocketExceptions
					throw new RuntimeException(e);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

		}
	}

	@Override
	public void close() {
		// Signal the server to stop
		running.set(false);
		socket.close();
	}

	/**
	 * This method is synchronized to ensure that updates to the `date` and `data` fields
	 * are thread-safe in a multithreaded environment. Since multiple threads might
	 * attempt to call this method concurrently (e.g., while the server is running and
	 * processing requests), synchronization guarantees that only one thread at a time
	 * can modify these shared fields, preventing race conditions and ensuring data consistency.
	 */
	public synchronized void updateTestServerResponse() {
		var now = new Date();
		this.date = now.toString();
		this.data = now.toString().getBytes(CHARSET);
	}
}

