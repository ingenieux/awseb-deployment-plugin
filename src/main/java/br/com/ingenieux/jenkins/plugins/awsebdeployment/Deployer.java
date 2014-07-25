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


import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.util.DirScanner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.CreateApplicationVersionRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.elasticbeanstalk.model.S3Location;
import com.amazonaws.services.elasticbeanstalk.model.UpdateEnvironmentRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

public class Deployer {
	private static final int MAX_ATTEMPTS = 15;

	private AWSEBDeploymentBuilder context;
	
	private PrintStream logger;

	private AmazonS3 s3;

	private AWSElasticBeanstalk awseb;

	private File localArchive;

	private FilePath rootFileObject;

	private String keyPrefix;

	private String bucketName;

	private String applicationName;

	private String versionLabel;

	private String objectKey;

	private String s3ObjectPath;

	private EnvVars env;

	private String environmentName;

	private BuildListener listener;

	public Deployer(AWSEBDeploymentBuilder builder,
			AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		this.context = builder;
		this.logger = listener.getLogger();
		this.env = build.getEnvironment(listener);
		this.listener = listener;
		
		this.rootFileObject = new FilePath(build.getWorkspace(),
				getValue(context.getRootObject()));
	}

	public void perform() throws Exception {
		initAWS();
		
		log("Running Version %s", getVersion());

		localArchive = getLocalFileObject(rootFileObject);

		uploadArchive();

		createApplicationVersion();

		updateEnvironments();

		listener.finished(Result.SUCCESS);

	}

	private void updateEnvironments() throws Exception {
		DescribeEnvironmentsResult environments = awseb
				.describeEnvironments(new DescribeEnvironmentsRequest()
						.withApplicationName(applicationName)
						.withEnvironmentNames(environmentName));

		boolean found = (1 == environments.getEnvironments().size());

		if (found) {
			for (int nAttempt = 1; nAttempt <= MAX_ATTEMPTS; nAttempt++) {
				String environmentId = environments.getEnvironments().get(0)
						.getEnvironmentId();

				log("Attempt %d/%s", nAttempt, MAX_ATTEMPTS);

				log("Environment found (environment id=%s). Attempting to update environment to version label %s",
						environmentId, versionLabel);

				UpdateEnvironmentRequest uavReq = new UpdateEnvironmentRequest()
						.withEnvironmentName(environmentName).withVersionLabel(
								versionLabel);

				try {
					awseb.updateEnvironment(uavReq);

					log("q'Apla!");
					
					return;
				} catch (Exception exc) {
					log("Problem: " + exc.getMessage());

					if (nAttempt == MAX_ATTEMPTS) {
						log("Giving it up");

						throw exc;
					}

					log("Reattempting in 90s, up to %d", MAX_ATTEMPTS);

					Thread.sleep(TimeUnit.SECONDS.toMillis(90));
				}
			}
		} else {
			log("Environment not found. Continuing");
		}
	}

	private void createApplicationVersion() {
		log("Creating application version %s for application %s for path %s",
				versionLabel, applicationName, s3ObjectPath);

		CreateApplicationVersionRequest cavRequest = new CreateApplicationVersionRequest()
				.withApplicationName(applicationName)
				.withAutoCreateApplication(true)
				.withSourceBundle(new S3Location(bucketName, objectKey))
				.withVersionLabel(versionLabel);

		awseb.createApplicationVersion(cavRequest);
	}

	private void uploadArchive() {
		this.keyPrefix = getValue(context.getKeyPrefix());
		this.bucketName = getValue(context.getBucketName());
		this.applicationName = getValue(context.getApplicationName());
		this.versionLabel = getValue(context.getVersionLabelFormat());
		this.environmentName = getValue(context.getEnvironmentName());

		objectKey = formatPath("%s/%s-%s.zip", keyPrefix, applicationName,
				versionLabel);

		s3ObjectPath = "s3://" + formatPath("%s/%s", bucketName, objectKey);

		log("Uploading file %s as %s", localArchive.getName(), s3ObjectPath);

		s3.putObject(bucketName, objectKey, localArchive);
	}

	private void initAWS() {
		log("Creating S3 and AWSEB Client (AWS Access Key Id: %s, region: %s)",
				context.getAwsAccessKeyId(),
				context.getAwsRegion());

		AWSCredentialsProvider credentials = new AWSCredentialsProviderChain(
				new StaticCredentialsProvider(new BasicAWSCredentials(
						context.getAwsAccessKeyId(),
						context.getAwsSecretSharedKey())));
		Region region = Region.getRegion(context.getAwsRegion());
		ClientConfiguration clientConfig = new ClientConfiguration();

		clientConfig.setUserAgent("ingenieux CloudButler/" + getVersion());

		s3 = region.createClient(AmazonS3Client.class, credentials,
				clientConfig);
		awseb = region.createClient(AWSElasticBeanstalkClient.class,
				credentials, clientConfig);
	}
	
	private static String VERSION = "UNKNOWN";

	private static String getVersion() {
		if ("UNKNOWN".equals(VERSION)) {
			try {
				Properties p = new Properties();
				
				p.load(Deployer.class.getResourceAsStream("version.properties"));
				
				VERSION = p.getProperty("awseb-deployer-plugin.version");
				
			} catch (Exception exc) {
				throw new RuntimeException(exc);
			}
		}

		return VERSION;
	}

	void log(String mask, Object... args) {
		logger.println(String.format(mask, args));
	}

	private File getLocalFileObject(FilePath rootFileObject) throws Exception {
		File resultFile = File.createTempFile("awseb-", ".zip");

		if (!rootFileObject.isDirectory()) {
			log("Root File Object is a file. We assume its a zip file, which is okay.");

			rootFileObject.copyTo(new FileOutputStream(resultFile));
		} else {
			log("Zipping contents of Root File Object (%s) into tmp file %s (includes=%s, excludes=%s)",
					rootFileObject.getName(), resultFile.getName(),
					context.getIncludes(), context.getExcludes());

			rootFileObject.zip(new FileOutputStream(resultFile),
					new DirScanner.Glob(context.getIncludes(),
							context.getExcludes()));
		}

		return resultFile;
	}

	private String formatPath(String mask, Object... args) {
		return strip(String.format(mask, args).replaceAll("/{2,}", ""));
	}

	private String getValue(String value) {
		return strip(Util.replaceMacro(value, env));
	}

	private static String strip(String str) {
		return StringUtils.strip(str, "/ ");
	}
}
