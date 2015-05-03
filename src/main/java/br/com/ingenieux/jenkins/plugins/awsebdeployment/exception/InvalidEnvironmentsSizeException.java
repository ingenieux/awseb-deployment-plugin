package br.com.ingenieux.jenkins.plugins.awsebdeployment.exception;

public class InvalidEnvironmentsSizeException extends Exception {

    private final String applicationName;
    private final String environmentName;

    public InvalidEnvironmentsSizeException(String applicationName, String environmentName) {
        super();
        this.applicationName = applicationName;
        this.environmentName = environmentName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getEnvironmentName() {
        return environmentName;
    }
}
