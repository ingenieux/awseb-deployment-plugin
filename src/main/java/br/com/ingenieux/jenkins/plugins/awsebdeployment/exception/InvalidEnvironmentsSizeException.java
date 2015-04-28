package br.com.ingenieux.jenkins.plugins.awsebdeployment.exception;

public class InvalidEnvironmentsSizeException extends Exception {

    public InvalidEnvironmentsSizeException() {
        super();
    }

    public InvalidEnvironmentsSizeException(String message) {
        super(message);
    }
}
