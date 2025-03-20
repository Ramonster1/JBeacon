package com.jbeacon.poll;

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpTestServer implements AutoCloseable {
	private static final int TIMEOUT = 1_000;
	@Getter
	private final DatagramSocket socket;
	@Getter
	@Setter
	private byte[] data;
	@Getter
	@Setter
	private String date;
	private volatile boolean running = true;

	public UdpTestServer() throws SocketException {
		this.socket = new DatagramSocket(0);
		socket.setSoTimeout(TIMEOUT);
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
				if (!running) {
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
		running = false; // Signal the server to stop
		socket.close();
	}
}

