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
package br.com.ingenieux.jenkins.plugins.awsebdeployment.steps;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;


/**
 * Deployment to AWS EB
 */
public class AWSEBDeploymentStep extends AbstractStepImpl
{
  private String credentialId;

  public String getCredentialId() {
    return this.credentialId;
  }
  /**
   * AWS Region
   */
  private String awsRegion;

  public String getAwsRegion() {
    return this.awsRegion;
  }

  /**
   * Application Name
   */
  private String applicationName;

  public String getApplicationName() {
    return this.applicationName;
  }

  /**
   * Environment Name
   */
  private String environmentName;

  public String getEnvironmentName() {
    return this.environmentName;
  }

  /**
   * Bucket Name
   */
  private String bucketName;

  public String getBucketName() {
    return this.bucketName;
  }

  /**
   * Key Format
   */
  private String keyPrefix;

  public String getKeyPrefix() {
    return this.keyPrefix;
  }

  /**
   * Version Label
   */
  private String versionLabelFormat;

  public String getVersionLabelFormat() {
    return this.versionLabelFormat;
  }

  /**
   * Version Description
   */
  private String versionDescriptionFormat;

  public String getVersionDescriptionFormat() {
    return this.versionDescriptionFormat;
  }

  /**
   * Root Object
   */
  private String rootObject;

  public String getRootObject() {
    return this.rootObject;
  }

  /**
   * Includes
   */
  private String includes;

  public String getIncludes() {
    return this.includes;
  }

  /**
   * Excludes
   */
  private String excludes;

  public String getExcludes() {
    return this.excludes;
  }

  /**
   * Zero Downtime
   */
  private boolean zeroDowntime;

  public boolean isZeroDowntime() {
    return this.zeroDowntime;
  }

  /**
   * Deploy Sleep Time
   */
  private Integer sleepTime;

  public Integer getSleepTime() {
    return sleepTime;
  }

  /**
   * Check Health
   */
  private boolean checkHealth;

  public boolean isCheckHealth() {
    return checkHealth;
  }

  /**
   * Max Number Of Attempts
   */
  private Integer maxAttempts;

  public Integer getMaxAttempts() {
    return maxAttempts;
  }

  @DataBoundConstructor
  public AWSEBDeploymentStep(String credentialId, String awsRegion, String applicationName,
                             String environmentName, String bucketName, String keyPrefix,
                             String versionLabelFormat, String versionDescriptionFormat,
                             String rootObject, String includes, String excludes,
                             boolean zeroDowntime, Integer sleepTime, boolean checkHealth,
                             Integer maxAttempts)
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
  }

  @Extension
  public static class DescriptorImpl extends AbstractStepDescriptorImpl {
    public DescriptorImpl() { super(AWSEBDeploymentStepExecution.class);}

    @Override
    public String getFunctionName() {
      return "awsEbDeployment";
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return "Deploy to AWS Elastic Beanstalk";
    }
  }
}
