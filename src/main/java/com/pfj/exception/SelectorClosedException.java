package com.pfj.exception;

import java.io.IOException;

public class SelectorClosedException extends IOException {
	public SelectorClosedException(String msg) {
		super(msg);
	}
}
