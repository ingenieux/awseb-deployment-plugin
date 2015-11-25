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

public class DeployerContext {
    static final int MAX_ATTEMPTS = 15;

    static final int SLEEP_TIME = 90;

    static final int MAX_ENVIRONMENT_NAME_LENGTH = 23;

    static final String GREEN_HEALTH = "Green";

    final AWSEBDeploymentBuilder deployerConfig;

	final PrintStream logger;

    AmazonS3 s3;

    AWSElasticBeanstalk awseb;

    final FilePath rootFileObject;

    String keyPrefix;

    String bucketName;

    String applicationName;

    String versionLabel;

    String objectKey;

    String s3ObjectPath;

    final EnvVars env;

    String environmentName;

    final BuildListener listener;

	public DeployerContext(AWSEBDeploymentBuilder builder,
                           AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		this.deployerConfig = builder;
		this.logger = listener.getLogger();
		this.env = build.getEnvironment(listener);
		this.listener = listener;

		this.rootFileObject = new FilePath(build.getWorkspace(), Utils.getValue(deployerConfig.getRootObject(), this.env));
	}
}
