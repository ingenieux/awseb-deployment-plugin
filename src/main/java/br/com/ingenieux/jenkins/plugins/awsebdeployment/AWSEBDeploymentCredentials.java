package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import lombok.Data;

import java.io.Serializable;

@Data
public class AWSEBDeploymentCredentials implements Serializable {
    private static final long serialVersionUID = 1L;

    public AWSEBDeploymentCredentials(String awsAccessKeyId, String awsSecretKey) {
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretKey = awsSecretKey;
    }

    /**
     * Access Key ID of credential
     */
    String awsAccessKeyId;

    /**
     * Secret Key of credential
     */
    String awsSecretKey;

    public AWSCredentials toAWSCredentials() {
        return new BasicAWSCredentials(awsAccessKeyId, awsSecretKey);
    }
}
