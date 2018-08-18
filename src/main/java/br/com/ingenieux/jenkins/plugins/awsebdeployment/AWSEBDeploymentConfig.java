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

import lombok.*;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;
import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ToString
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

  /**
   * Version Label Format
   */
  private String versionLabelFormat;

  /**
   * Version Description Format
   */
  private String versionDescriptionFormat;

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
   * Skip Environment Updates?
   */
  private boolean skipEnvironmentUpdates;

  /**
   * Copy Factory
   *
   * @param r replacer
   * @return replaced copy
   */
  AWSEBDeploymentConfig replacedCopy(Utils.Replacer r) throws MacroEvaluationException, IOException, InterruptedException {
    // FIELDLIST CHECK
    return AWSEBDeploymentConfig.builder()
            .credentialId(r.r(this.credentialId))
            .awsRegion(r.r(this.awsRegion))
            .applicationName(r.r(this.applicationName))
            .environmentName(r.r(this.environmentName))
            .bucketName(r.r(this.bucketName))
            .keyPrefix(r.r(this.keyPrefix))
            .versionLabelFormat(r.r(this.versionLabelFormat))
            .versionDescriptionFormat(r.r(this.versionDescriptionFormat))
            .rootObject(r.r(this.rootObject))
            .includes(r.r(this.includes))
            .excludes(r.r(this.excludes))
            .zeroDowntime(this.zeroDowntime)
            .sleepTime(this.sleepTime)
            .checkHealth(this.checkHealth)
            .skipEnvironmentUpdates(this.skipEnvironmentUpdates)
            .build();
  }
}
