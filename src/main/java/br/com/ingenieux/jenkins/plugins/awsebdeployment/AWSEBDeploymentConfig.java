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

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import lombok.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

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
   * Credentials
   */
  private AmazonWebServicesCredentials credentials;

  private boolean createEnvironmentIfNotExist;

  private String environmentCNAMEPrefix;

  private String environmentTemplateName;

  private List<AWSEBRawConfigurationOptionSetting> environmentSettings;

  private ConfigurationOptionSetting[] environmentConfigurationOptionSettings;

  private boolean route53UpdateRecordSet;

  private String route53HostedZoneId;

  private String route53DomainName;

  private Long route53RecordTTL;

  private String route53RecordType;

  /**
   * Copy Factory
   *
   * @param r replacer
   * @return replaced copy
   */
  public AWSEBDeploymentConfig replacedCopy(Utils.Replacer r) throws MacroEvaluationException, IOException, InterruptedException {
    List<ConfigurationOptionSetting> list = new ArrayList<ConfigurationOptionSetting>();
    for(AWSEBRawConfigurationOptionSetting rawSetting :this.getEnvironmentSettings()){
        ConfigurationOptionSetting configurationOptionSetting = new ConfigurationOptionSetting(
            r.r(rawSetting.getNamespace()),
            r.r(rawSetting.getOptionName()),
            r.r(rawSetting.getValue())
        );
        list.add(configurationOptionSetting);
    }
    ConfigurationOptionSetting[] settings = list.toArray(new ConfigurationOptionSetting[list.size()]);

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
        this.credentials,
        this.isCreateEnvironmentIfNotExist(),
        r.r(this.getEnvironmentCNAMEPrefix()),
        r.r(this.getEnvironmentTemplateName()),
        this.environmentSettings,
        settings,
        this.isRoute53UpdateRecordSet(),
        r.r(this.getRoute53HostedZoneId()),
        r.r(this.getRoute53DomainName()),
        this.route53RecordTTL,
        r.r(this.getRoute53RecordType())
    );
  }
}
