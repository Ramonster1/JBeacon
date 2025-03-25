package com.jbeacon.poll.util;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UdpTestServer implements AutoCloseable {
	public static final Charset CHARSET = Charset.defaultCharset();
	private static final int TIMEOUT = Math.toIntExact(TimeUnit.SECONDS.toMillis(10));
	@Getter
	private final DatagramSocket socket;
	@Getter
	@Setter
	private byte[] data;
	@Getter
	@Setter
	private String date;
	private final AtomicBoolean running = new AtomicBoolean(true);

	public UdpTestServer() throws SocketException {
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
		running.set(false); // Signal the server to stop
		socket.close();
	}

	public void updateTestServerResponse() {
		var now = new Date();
		setData(now.toString().getBytes(CHARSET));
	}
}

