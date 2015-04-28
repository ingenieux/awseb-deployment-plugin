package br.com.ingenieux.jenkins.plugins.awsebdeployment.exception;

public class InvalidParametersException extends Exception {

    public InvalidParametersException() {
        super();
    }

    public InvalidParametersException(String message) {
        super(message);
    }
}
