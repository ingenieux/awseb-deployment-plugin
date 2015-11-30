package br.com.ingenieux.jenkins.plugins.awsebdeployment;

/*
 * #%L
 * AWS Elastic Beanstalk Deployment Plugin
 * %%
 * Copyright (C) 2013 ingenieux Labs
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.s3.AmazonS3;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Serializable;

public class DeployerContext implements Constants, Serializable {
    final AWSEBDeploymentConfig deployerConfig;

    final FilePath rootFileObject;

    transient AmazonS3 s3;

    transient AWSElasticBeanstalk awseb;

    transient PrintWriter logger;

    String keyPrefix;

    String bucketName;

    String applicationName;

    String versionLabel;

    String objectKey;

    String s3ObjectPath;

    String environmentName;

    public DeployerContext(AWSEBDeploymentConfig deployerConfig, FilePath rootFileObject) {
        this.deployerConfig = deployerConfig;
        this.rootFileObject = rootFileObject;
    }
}
