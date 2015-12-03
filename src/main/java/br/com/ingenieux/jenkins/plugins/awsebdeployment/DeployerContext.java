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

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.s3.AmazonS3;

import java.io.PrintWriter;
import java.io.Serializable;

import hudson.FilePath;
import hudson.remoting.Pipe;

public class DeployerContext implements Constants, Serializable {

    final AWSEBDeploymentConfig deployerConfig;

    final FilePath rootFileObject;

    transient AmazonS3 s3;

    transient AWSElasticBeanstalk awseb;

    transient PrintWriter logger;

    Pipe loggerOut;

    String keyPrefix;

    String bucketName;

    String applicationName;

    String versionLabel;

    String objectKey;

    String s3ObjectPath;

    String environmentName;

    public DeployerContext(AWSEBDeploymentConfig deployerConfig, FilePath rootFileObject,
                           Pipe loggerOut) {
        this.deployerConfig = deployerConfig;
        this.rootFileObject = rootFileObject;
        this.loggerOut = loggerOut;
    }
}
