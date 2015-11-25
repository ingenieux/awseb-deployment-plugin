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


import br.com.ingenieux.jenkins.plugins.awsebdeployment.exception.InvalidEnvironmentsSizeException;
import br.com.ingenieux.jenkins.plugins.awsebdeployment.exception.InvalidParametersException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.util.DirScanner;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.StringUtils.isBlank;

public class Deployer {
    private static final int MAX_ATTEMPTS = 15;
    private static final int SLEEP_TIME = 90;
    private static final int MAX_ENVIRONMENT_NAME_LENGTH = 23;

    private static final String GREEN_HEALTH = "Green";

    private AWSEBDeploymentBuilder context;

	private PrintStream logger;

	private AmazonS3 s3;

	private AWSElasticBeanstalk awseb;

    private FilePath rootFileObject;

	private String keyPrefix;

	private String bucketName;

	private String applicationName;

	private String versionLabel;

	private String objectKey;

	private String s3ObjectPath;

	private EnvVars env;

	private String environmentName;

    private boolean zeroDowntime;

	private BuildListener listener;

	public Deployer(AWSEBDeploymentBuilder builder,
			AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
		this.context = builder;
		this.logger = listener.getLogger();
		this.env = build.getEnvironment(listener);
		this.listener = listener;

		this.rootFileObject = new FilePath(build.getWorkspace(), getValue(context.getRootObject()));
        this.zeroDowntime = builder.isZeroDowntime();
	}

	public void perform() throws Exception {
        try {
            validateParameters();
            initAWS();
            String[] environmentNames = generateEnvironmentNames();

            log("Running Version %s", getVersion());

            uploadArchive();
            ApplicationVersionDescription applicationVersion = createApplicationVersion();
            String environmentId = getEnvironmentId(environmentNames);

            if (zeroDowntime) {
                String templateName = createConfigurationTemplate(environmentId);

                String clonedEnvironmentId = createEnvironment(applicationVersion.getVersionLabel(), templateName, environmentNames);

                validateEnvironmentStatus(clonedEnvironmentId);

                swapEnvironmentCnames(environmentId, clonedEnvironmentId);

                terminateEnvironment(environmentId);
            } else {
                updateEnvironment(environmentId);

                validateEnvironmentStatus(environmentId);
            }

            log("q'Apla!");

            listener.finished(Result.SUCCESS);
        } catch (InvalidParametersException e) {
            log("Skipping update: %s", e.getMessage());

            listener.finished(Result.FAILURE);
        } catch (InvalidEnvironmentsSizeException e) {
            log("Environment %s/%s not found. Continuing", e.getApplicationName(), e.getEnvironmentName());
            listener.finished(Result.FAILURE);
        }

	}

    private void terminateEnvironment(String environmentId) {
        log("Terminating environment %s", environmentId);

        TerminateEnvironmentRequest request = new TerminateEnvironmentRequest().withEnvironmentId(environmentId);

        awseb.terminateEnvironment(request);
    }

    private void swapEnvironmentCnames(String environmentId, String clonedEnvironmentId) throws InterruptedException {
        log("Swapping CNAMEs from environment %s to %s", environmentId, clonedEnvironmentId);

        SwapEnvironmentCNAMEsRequest request = new SwapEnvironmentCNAMEsRequest()
                .withSourceEnvironmentId(environmentId).withDestinationEnvironmentId(clonedEnvironmentId);

        awseb.swapEnvironmentCNAMEs(request);

        Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_TIME)); //So the CNAMEs will swap
    }

    private String createEnvironment(String versionLabel, String templateName, String[] environmentNames) {
        log("Creating environment based on application %s/%s from version %s and configuration template %s",
                applicationName, environmentName, versionLabel, templateName);

        String newEnvironmentName = environmentNames[0];
        for (String environmentName : environmentNames) {
            try {
                getEnvironmentId(environmentName);
            } catch (InvalidEnvironmentsSizeException e) {
                newEnvironmentName = environmentName;

                break;
            }
        }

        CreateEnvironmentRequest request = new CreateEnvironmentRequest()
                .withEnvironmentName(newEnvironmentName).withVersionLabel(versionLabel)
                .withApplicationName(applicationName).withTemplateName(templateName);

        return awseb.createEnvironment(request).getEnvironmentId();
    }

	private UpdateEnvironmentResult updateEnvironment(String environmentId) throws Exception {
        for (int nAttempt = 1; nAttempt <= MAX_ATTEMPTS; nAttempt++) {
            log("Update attempt %d/%s", nAttempt, MAX_ATTEMPTS);

            log("Environment found (environment id=%s). Attempting to update environment to version label %s",
                    environmentId, versionLabel);

            UpdateEnvironmentRequest uavReq = new UpdateEnvironmentRequest()
                    .withEnvironmentName(environmentName).withVersionLabel(
                            versionLabel);

            try {
                UpdateEnvironmentResult result = awseb.updateEnvironment(uavReq);
                log("Environment updated (environment id=%s). Attempting to validate environment status.", environmentId);

                return result;
            } catch (Exception exc) {
                log("Problem: " + exc.getMessage());

                if (nAttempt == MAX_ATTEMPTS) {
                    log("Giving it up");

                    throw exc;
                }

                log("Reattempting in %ds, up to %d", SLEEP_TIME, MAX_ATTEMPTS);

                Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_TIME));
            }
        }

        throw new Exception();
	}

    private void validateEnvironmentStatus(String environmentId) throws Exception {
        for (int nAttempt = 1; nAttempt <= MAX_ATTEMPTS; nAttempt++) {
            log("Checking health of environment %s attempt %d/%s", environmentId, nAttempt, MAX_ATTEMPTS);

            List<EnvironmentDescription> environments = awseb.describeEnvironments(new DescribeEnvironmentsRequest()
                    .withEnvironmentIds(Arrays.asList(environmentId))).getEnvironments();

            if (environments.size() != 1) {
                throw new InvalidEnvironmentsSizeException(applicationName, environmentName);
            }

            EnvironmentDescription environmentDescription = environments.get(0);
            if (GREEN_HEALTH.equals(environmentDescription.getHealth())) {
                return;
            }

            Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_TIME));
        }
    }

    private String getEnvironmentId(String... environmentNames) throws InvalidEnvironmentsSizeException {
        DescribeEnvironmentsResult environments = awseb
                .describeEnvironments(new DescribeEnvironmentsRequest()
                        .withApplicationName(applicationName)
                        .withIncludeDeleted(false));

        for (EnvironmentDescription description : environments.getEnvironments()) {
            if (ArrayUtils.contains(environmentNames, description.getEnvironmentName())) {
                return description.getEnvironmentId();
            }
        }

        throw new InvalidEnvironmentsSizeException(applicationName, environmentNames[0]);
    }

    private String createConfigurationTemplate(String environmentId) {
        log("Creating configuration template from application %s with label %s", applicationName, versionLabel);

        CreateConfigurationTemplateRequest request = new CreateConfigurationTemplateRequest()
                .withEnvironmentId(environmentId).withApplicationName(applicationName)
                .withTemplateName(versionLabel);

        return awseb.createConfigurationTemplate(request).getTemplateName();
    }

	private ApplicationVersionDescription createApplicationVersion() {
		log("Creating application version %s for application %s for path %s",
				versionLabel, applicationName, s3ObjectPath);

		CreateApplicationVersionRequest cavRequest = new CreateApplicationVersionRequest()
				.withApplicationName(applicationName)
				.withAutoCreateApplication(true)
				.withSourceBundle(new S3Location(bucketName, objectKey))
				.withVersionLabel(versionLabel);

		return awseb.createApplicationVersion(cavRequest).getApplicationVersion();
	}

	private void uploadArchive() throws Exception {
        File localArchive = getLocalFileObject(rootFileObject);
		objectKey = formatPath("%s/%s-%s.zip", keyPrefix, applicationName,
				versionLabel);

		s3ObjectPath = "s3://" + formatPath("%s/%s", bucketName, objectKey);

		log("Uploading file %s as %s", localArchive.getName(), s3ObjectPath);

		s3.putObject(bucketName, objectKey, localArchive);
	}

    private void validateParameters() throws InvalidParametersException {
        this.keyPrefix = getValue(context.getKeyPrefix());
        this.bucketName = getValue(context.getBucketName());
        this.applicationName = getValue(context.getApplicationName());
        this.versionLabel = getValue(context.getVersionLabelFormat());
        this.environmentName = getValue(context.getEnvironmentName());

        if (isBlank(environmentName)) {
            throw new InvalidParametersException("Empty/blank environmentName parameter");
        }

        if (isBlank(applicationName)) {
            throw new InvalidParametersException("Empty/blank applicationName parameter");
        }

        if (isBlank(bucketName)) {
            throw new InvalidParametersException("Empty/blank bucketName parameter");
        }

        if (isBlank(versionLabel)) {
            throw new InvalidParametersException("Empty/blank versionLabel parameter");
        }
    }

    private String[] generateEnvironmentNames() {
        List<String> environmentNames = new ArrayList<String>(){{add(environmentName);}};
        if (zeroDowntime) {
            String newEnvironmentName = environmentName.length() <= MAX_ENVIRONMENT_NAME_LENGTH - 2 ?
                    environmentName : environmentName.substring(0, environmentName.length() - 2);

            environmentNames.add(newEnvironmentName + "-2");
        }

        return environmentNames.toArray(new String[environmentNames.size()]);
    }

	private void initAWS()
            throws InvocationTargetException, NoSuchMethodException, InstantiationException,
                   IllegalAccessException {
		log("Creating S3 and AWSEB Client (AWS Access Key Id: %s, region: %s)",
				context.getAwsAccessKeyId(),
				context.getAwsRegion());

		AWSCredentialsProvider credentials = new AWSCredentialsProviderChain(
				new StaticCredentialsProvider(new BasicAWSCredentials(
						context.getAwsAccessKeyId(),
						context.getAwsSecretSharedKey())));
		ClientConfiguration clientConfig = new ClientConfiguration();

		clientConfig.setUserAgent("ingenieux CloudButler/" + getVersion());

                final AWSClientFactory
                    awsClientFactory =
                    new AWSClientFactory(credentials, clientConfig, context.getAwsRegion());

                s3 = awsClientFactory.getService(AmazonS3Client.class);
                awseb = awsClientFactory.getService(AWSElasticBeanstalkClient.class);
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
