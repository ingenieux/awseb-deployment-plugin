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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString(exclude={"credentials"})
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
  private List<String> environmentNames;

  /**
   * Bucket Name
   */
  private String bucketName;

  /**
   * Key Format
   */
  private String keyPrefix;

  /**
   * Version Label Format
   */
  private String versionLabelFormat;

  /**
   * Root Object
   */
  private String rootObject;

  /**
   * Includes
   */
  private String includes;

  /**
   * Excludes
   */
  private String excludes;

  /**
   * Zero Downtime
   */
  private boolean zeroDowntime;

  /**
   * Deployment Sleep Time
   */
  private Integer sleepTime;

  /**
   * Check Health
   */
  private boolean checkHealth;

  /**
   * Max Number Of Attempts
   */
  private Integer maxAttempts;

  /**
   * Credentials
   */
  private AmazonWebServicesCredentials credentials;

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
        r.rs(this.getEnvironmentNames()),
        r.r(this.getBucketName()),
        r.r(this.getKeyPrefix()),
        r.r(this.getVersionLabelFormat()),
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
