/*
 * Copyright 2011 ingenieux Labs
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import lombok.*;

import java.io.IOException;
import java.io.Serializable;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

@NoArgsConstructor
@Data
@ToString(exclude={"credentials"})
public class AWSEBDeploymentConfig implements Serializable {
  public AWSEBDeploymentConfig(String credentialId, String awsRegion, String applicationName,
                               String environmentName, String bucketName, String keyPrefix,
                               String versionLabelFormat, String versionDescriptionFormat,
                               String rootObject, String includes, String excludes,
                               boolean zeroDowntime, Integer sleepTime, boolean checkHealth,
                               Integer maxAttempts, AmazonWebServicesCredentials credentials)
  {
    this.credentialId = credentialId;
    this.awsRegion = awsRegion;
    this.applicationName = applicationName;
    this.environmentName = environmentName;
    this.bucketName = bucketName;
    this.keyPrefix = keyPrefix;
    this.versionLabelFormat = versionLabelFormat;
    this.versionDescriptionFormat = versionDescriptionFormat;
    this.rootObject = rootObject;
    this.includes = includes;
    this.excludes = excludes;
    this.zeroDowntime = zeroDowntime;
    this.sleepTime = sleepTime;
    this.checkHealth = checkHealth;
    this.maxAttempts = maxAttempts;
    this.credentials = credentials;
  }

  private static final long serialVersionUID = 1L;

  private String credentialId;

  public String getCredentialId() {
    return this.credentialId;
  }

  public void setCredentialsId(final String credentialId) {
    this.credentialId = credentialId;
  }

  /**
   * AWS Region
   */
  private String awsRegion;

  public String getAwsRegion() {
    return this.awsRegion;
  }

  public void setAwsRegion(final String awsRegion) {
    this.awsRegion = awsRegion;
  }

  /**
   * Application Name
   */
  private String applicationName;

  public String getApplicationName() {
    return this.applicationName;
  }

  public void setApplicationName(final String applicationName) {
    this.applicationName = applicationName;
  }

  /**
   * Environment Name
   */
  private String environmentName;

  public String getEnvironmentName() {
    return this.environmentName;
  }

  public void setEnvironmentName(final String environmentName) {
    this.environmentName = environmentName;
  }

  /**
   * Bucket Name
   */
  private String bucketName;

  public String getBucketName() {
    return this.bucketName;
  }

  public void setBucketName(final String bucketName) {
    this.bucketName = bucketName;
  }

  /**
   * Key Format
   */
  private String keyPrefix;

  public String getKeyPrefix() {
    return this.keyPrefix;
  }

  public void setKeyPrefix(final String keyPrefix) {
    this.keyPrefix = keyPrefix;
  }

  /**
   * Version Label
   */
  private String versionLabelFormat;

  public String getVersionLabelFormat() {
    return this.versionLabelFormat;
  }

  public void setVersionLabelFormat(final String versionLabelFormat) {
    this.versionLabelFormat = versionLabelFormat;
  }

  /**
   * Version Description
   */
  private String versionDescriptionFormat;

  public String getVersionDescriptionFormat() {
    return this.versionDescriptionFormat;
  }

  public void setVersionDescriptionFormat(final String versionDescriptionFormat) {
    this.versionDescriptionFormat = versionDescriptionFormat;
  }

  /**
   * Root Object
   */
  private String rootObject;

  public String getRootObject() {
    return this.rootObject;
  }

  public void setRootObject(final String rootObject) {
    this.rootObject = rootObject;
  }

  /**
   * Includes
   */
  private String includes;

  public String getIncludes() {
    return this.includes;
  }

  public void setIncludes(final String includes) {
    this.includes = includes;
  }

  /**
   * Excludes
   */
  private String excludes;

  public String getExcludes() {
    return this.excludes;
  }

  public void setExcludes(final String excludes) {
    this.excludes = excludes;
  }

  /**
   * Zero Downtime
   */
  private boolean zeroDowntime;

  public boolean isZeroDowntime() {
    return this.zeroDowntime;
  }

  public void setZeroDowntime(final boolean zeroDowntime) {
    this.zeroDowntime = zeroDowntime;
  }

  /**
   * Deploy Sleep Time
   */
  private Integer sleepTime;

  public Integer getSleepTime() {
    return sleepTime;
  }

  public void setSleepTime(final Integer sleepTime) {
    this.sleepTime = sleepTime;
  }

  /**
   * Check Health
   */
  private boolean checkHealth;

  public boolean isCheckHealth() {
    return checkHealth;
  }

  public void setCheckHealth(final boolean checkHealth) {
    this.checkHealth = checkHealth;
  }

  /**
   * Max Number Of Attempts
   */
  private Integer maxAttempts;

  public Integer getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(final Integer maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  /**
   * Credentials
   */
  private AmazonWebServicesCredentials credentials;

  public AmazonWebServicesCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(final AmazonWebServicesCredentials credentials) {
    this.credentials = credentials;
  }

  /**
   * Copy Factory
   *
   * @param r replacer
   * @return replaced copy
   */
  public AWSEBDeploymentConfig replacedCopy(Utils.Replacer r) throws MacroEvaluationException, IOException, InterruptedException {
    return new AWSEBDeploymentConfig(
        r.r(this.getCredentialId()),
        r.r(this.getAwsRegion()),
        r.r(this.getApplicationName()),
        r.r(this.getEnvironmentName()),
        r.r(this.getBucketName()),
        r.r(this.getKeyPrefix()),
        r.r(this.getVersionLabelFormat()),
        r.r(this.getVersionDescriptionFormat()),
        r.r(this.getRootObject()),
        r.r(this.getIncludes()),
        r.r(this.getExcludes()),
        this.isZeroDowntime(),
        this.getSleepTime(),
        this.isCheckHealth(),
        this.getMaxAttempts(),
        this.credentials
    );
  }
}
