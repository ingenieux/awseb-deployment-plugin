/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;

import java.io.Serializable;

public class AWSEBDeploymentConfig implements Serializable {

  private static final long serialVersionUID = 1L;
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
  private AmazonWebServicesCredentials credentials;

  public AWSEBDeploymentConfig() {
  }

  public AWSEBDeploymentConfig(String credentialId, String awsRegion, String applicationName,
                               String environmentName, String bucketName, String keyPrefix,
                               String versionLabelFormat, String rootObject, String includes,
                               String excludes, boolean zeroDowntime,
                               AmazonWebServicesCredentials credentials) {
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
    this.credentials = credentials;
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
        this.isZeroDowntime(),
        this.credentials
    );
  }

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

  public AmazonWebServicesCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(
      AmazonWebServicesCredentials credentials) {
    this.credentials = credentials;
  }
}
