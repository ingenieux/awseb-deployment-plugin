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

package br.com.ingenieux.jenkins.plugins.awsebdeployment.cmd;

import br.com.ingenieux.jenkins.plugins.awsebdeployment.AWSEBDeploymentConfig;
import br.com.ingenieux.jenkins.plugins.awsebdeployment.Constants;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.s3.AmazonS3;
import hudson.FilePath;
import hudson.model.TaskListener;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.PrintStream;
import java.io.Serializable;

@RequiredArgsConstructor
@Data
public class DeployerContext implements Constants, Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * Deployer Config
     */
    final AWSEBDeploymentConfig deployerConfig;

    /**
     * Root File Object
     */
    final FilePath rootFileObject;

    /**
     * Listener
     */
    final TaskListener listener;

    /**
     * S3 Client
     */
    transient AmazonS3 s3;

    /**
     * Elastic Beanstalk Client
     */
    transient AWSElasticBeanstalk awseb;

    /**
     * Logger Object
     */
    transient PrintStream logger;

    /**
     * Key Prefix
     */
    String keyPrefix;

    /**
     * Bucket Name
     */
    String bucketName;

    /**
     * Application Name
     */
    String applicationName;

    /**
     * Version Label
     */
    String versionLabel;

    /**
     * Version Description
     */
    String versionDescription;

    /**
     * Object Key
     */
    String objectKey;

    /**
     * S3 Object Path
     */
    String s3ObjectPath;

    /**
     * Environment Name
     */
    String environmentName;

    /**
     * Environment Id
     */
    String environmentId;

    /**
     * SuccessfulP
     */
    boolean successfulP;
}
