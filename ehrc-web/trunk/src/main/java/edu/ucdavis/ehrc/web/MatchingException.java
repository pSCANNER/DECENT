package edu.ucdavis.ehrc.web;

public class MatchingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public MatchingException(String message, Throwable cause) {
		super(message, cause);
	}

	public MatchingException(String message) {
		super(message);
	}

}
