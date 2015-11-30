package br.com.ingenieux.jenkins.plugins.awsebdeployment.exception;

public class InvalidParametersException extends Exception {
	private static final long serialVersionUID = 1L;

    public InvalidParametersException() {
        super();
    }

    public InvalidParametersException(String message) {
        super(message);
    }
}
