package com.jbeacon.core.poll;

import java.io.IOException;

public interface PollingService {
	void poll() throws IOException;
}
