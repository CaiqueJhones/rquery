package br.com.caiquejh.rquery.exception;

public class RQueryException extends RuntimeException {

    public RQueryException(String message) {
        super(message);
    }

    public RQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
