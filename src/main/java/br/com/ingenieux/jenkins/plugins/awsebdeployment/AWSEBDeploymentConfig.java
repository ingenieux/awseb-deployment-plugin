package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import java.io.Serializable;

public class AWSEBDeploymentConfig implements Serializable {
	private static final long serialVersionUID = 1L;

	public AWSEBDeploymentConfig() {
    }

    public AWSEBDeploymentConfig(String credentialId, String awsRegion, String applicationName, String environmentName, String bucketName, String keyPrefix, String versionLabelFormat, String rootObject, String includes, String excludes, boolean zeroDowntime) {
        this.credentialId = credentialId;
        this.awsRegion = awsRegion;
        this.applicationName = applicationName;
        this.environmentName = environmentName;
        this.bucketName = bucketName;
        this.keyPrefix = keyPrefix;
        this.versionLabelFormat = versionLabelFormat;
        this.rootObject = rootObject;
        this.includes = includes;
        this.excludes = excludes;
        this.zeroDowntime = zeroDowntime;
    }

    /**
     * Copy Factory
     *
     * @param r replacer
     * @return replaced copy
     */
    public AWSEBDeploymentConfig replacedCopy(Utils.Replacer r) {
        return new AWSEBDeploymentConfig(
                r.r(this.getCredentialId()),
                r.r(this.getAwsRegion()),
                r.r(this.getApplicationName()),
                r.r(this.getEnvironmentName()),
                r.r(this.getBucketName()),
                r.r(this.getKeyPrefix()),
                r.r(this.getVersionLabelFormat()),
                r.r(this.getRootObject()),
                r.r(this.getIncludes()),
                r.r(this.getExcludes()),
                this.isZeroDowntime()
        );
    }


    /**
     * Credentials name
     */
    private String credentialId;

    /**
     * AWS Region
     */
    private String awsRegion;

    /**
     * Application Name
     */
    private String applicationName;

    /**
     * Environment Name
     */
    private String environmentName;

    /**
     * Bucket Name
     */
    private String bucketName;

    /**
     * Key Format
     */
    private String keyPrefix;

    private String versionLabelFormat;

    private String rootObject;

    private String includes;

    private String excludes;

    private boolean zeroDowntime;

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public void setAwsRegion(String awsRegion) {
        this.awsRegion = awsRegion;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public void setEnvironmentName(String environmentName) {
        this.environmentName = environmentName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String getVersionLabelFormat() {
        return versionLabelFormat;
    }

    public void setVersionLabelFormat(String versionLabelFormat) {
        this.versionLabelFormat = versionLabelFormat;
    }

    public String getRootObject() {
        return rootObject;
    }

    public void setRootObject(String rootObject) {
        this.rootObject = rootObject;
    }

    public String getIncludes() {
        return includes;
    }

    public void setIncludes(String includes) {
        this.includes = includes;
    }

    public String getExcludes() {
        return excludes;
    }

    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }

    public boolean isZeroDowntime() {
        return zeroDowntime;
    }

    public void setZeroDowntime(boolean zeroDowntime) {
        this.zeroDowntime = zeroDowntime;
    }
}
