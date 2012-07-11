package com.davidparry.magnifying;

public class DebugException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 98111125548353985L;

	public DebugException() {
	}

	public DebugException(String detailMessage) {
		super(detailMessage);
	}

	public DebugException(Throwable throwable) {
		super(throwable);
	}

	public DebugException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
