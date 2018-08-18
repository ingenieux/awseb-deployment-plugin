/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.ApplicationDescription;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsRequest;
import com.amazonaws.services.elasticbeanstalk.model.DescribeEnvironmentsResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

/**
 * AWS Elastic Beanstalk Deployment
 */
@SuppressWarnings({"unchecked", "deprecation"})
public class AWSEBDeploymentBuilder extends Builder implements SimpleBuildStep {
    private static final Logger LOGGER = LoggerFactory.getLogger(AWSEBDeploymentBuilder.class);

    @Getter
    private AWSEBDeploymentConfig config;

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return super.perform(build, launcher, listener);
    }

    @DataBoundConstructor
    public AWSEBDeploymentBuilder(AWSEBDeploymentConfig config) {
        if (null == config) {
            config = new AWSEBDeploymentConfig();
        }

        this.config = config;
    }

    /*
     // FIELDLIST CHECK
     */

    @DataBoundSetter
    public void setSkipEnvironmentUpdates(boolean skipEnvironmentUpdates) {
        config.setSkipEnvironmentUpdates(skipEnvironmentUpdates);
    }

    @DataBoundSetter
    public void setMaxAttempts(Integer maxAttempts) {
        config.setMaxAttempts(maxAttempts);
    }

    @DataBoundSetter
    public void setCheckHealth(boolean checkHealth) {
        config.setCheckHealth(checkHealth);
    }

    @DataBoundSetter
    public void setSleepTime(Integer sleepTime) {
        config.setSleepTime(sleepTime);
    }

    @DataBoundSetter
    public void setZeroDowntime(boolean zeroDowntime) {
        config.setZeroDowntime(zeroDowntime);
    }

    @DataBoundSetter
    public void setExcludes(String excludes) {
        config.setExcludes(excludes);
    }

    @DataBoundSetter
    public void setIncludes(String includes) {
        config.setIncludes(includes);
    }

    @DataBoundSetter
    public void setRootObject(String rootObject) {
        config.setRootObject(rootObject);
    }

    @DataBoundSetter
    public void setVersionDescriptionFormat(String versionDescriptionFormat) {
        config.setVersionDescriptionFormat(versionDescriptionFormat);
    }

    @DataBoundSetter
    public void setVersionLabelFormat(String versionLabelFormat) {
        config.setVersionLabelFormat(versionLabelFormat);
    }

    @DataBoundSetter
    public void setKeyPrefix(String keyPrefix) {
        config.setKeyPrefix(keyPrefix);
    }

    @DataBoundSetter
    public void setBucketName(String bucketName) {
        config.setBucketName(bucketName);
    }

    @DataBoundSetter
    public void setEnvironmentName(String environmentName) {
        config.setEnvironmentName(environmentName);
    }

    @DataBoundSetter
    public void setApplicationName(String applicationName) {
        config.setApplicationName(applicationName);
    }

    @DataBoundSetter
    public void setAwsRegion(String awsRegion) {
        config.setAwsRegion(awsRegion);
    }

    @DataBoundSetter
    public void setCredentialId(String credentialId) {
        config.setCredentialId(credentialId);
    }

    public boolean isSkipEnvironmentUpdates() {
        return config.isSkipEnvironmentUpdates();
    }

    public Integer getMaxAttempts() {
        return config.getMaxAttempts();
    }

    public boolean isCheckHealth() {
        return config.isCheckHealth();
    }

    public Integer getSleepTime() {
        return config.getSleepTime();
    }

    public boolean isZeroDowntime() {
        return config.isZeroDowntime();
    }

    public String getExcludes() {
        return config.getExcludes();
    }

    public String getIncludes() {
        return config.getIncludes();
    }

    public String getRootObject() {
        return config.getRootObject();
    }

    public String getVersionDescriptionFormat() {
        return config.getVersionDescriptionFormat();
    }

    public String getVersionLabelFormat() {
        return config.getVersionLabelFormat();
    }

    public String getKeyPrefix() {
        return config.getKeyPrefix();
    }

    public String getBucketName() {
        return config.getBucketName();
    }

    public String getEnvironmentName() {
        return config.getEnvironmentName();
    }

    public String getApplicationName() {
        return config.getApplicationName();
    }

    public String getAwsRegion() {
        return config.getAwsRegion();
    }

    public String getCredentialId() {
        return config.getCredentialId();
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath ws, @Nonnull Launcher launcher,
                        @Nonnull TaskListener listener) throws IOException {
        try {
            new DeployerRunner(build, ws, launcher, listener, this).perform();
        } catch (Exception exc) {
            throw new IOException("Deployment Failure", exc);
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @SuppressWarnings("rawtypes")
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "AWS Elastic Beanstalk";
        }

        public AbstractIdCredentialsListBoxModel<?, ?> doFillCredentialIdItems(
                @AncestorInPath Item owner) {
            if (owner == null || !owner.hasPermission(Item.CONFIGURE)) {
                return new AWSCredentialsListBoxModel();
            }

            List<AmazonWebServicesCredentials>
                    creds =
                    CredentialsProvider
                            .lookupCredentials(AmazonWebServicesCredentials.class, owner, ACL.SYSTEM,
                                    Collections.<DomainRequirement>emptyList());

            return new AWSCredentialsListBoxModel()
                    .withEmptySelection()
                    .withAll(creds);
        }

        public FormValidation doCheckAwsRegion(@QueryParameter String value) {
            if (value.contains("$")) {
                return FormValidation.warning("Validation skipped due to parameter usage ('$')");
            }

            if (!value.matches("^\\p{Alpha}{2}-(?:gov-)?\\p{Alpha}{4,}-\\d$")) {
                return FormValidation.error("Doesn't look like a region, like {place}-{cardinal}-{number}");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckApplicationName(@QueryParameter String value) {
            if (value.contains("$")) {
                return FormValidation.warning("Validation skipped due to parameter usage ('$')");
            }

            int valueLen = value.length();
            if (valueLen == 0 || valueLen > 100) {
                return FormValidation.error("Application Names must have between 1-100 characters");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckEnvironmentName(@QueryParameter String value, @QueryParameter boolean skipEnvironmentUpdates) {
            if (skipEnvironmentUpdates) {
                return FormValidation.ok("Environment Updates Skipped");
            }

            if (value.contains("$")) {
                return FormValidation.warning("Validation skipped due to parameter usage ('$')");
            }

            if (value.contains(",")) {
                if (!value.matches("^[\\p{Alpha}[\\p{Alnum}\\-]{0,39}]+(,\\p{Space}*[\\p{Alpha}[\\p{Alnum}\\-]{0,39}]+)*$") || value.endsWith("-")) {
                    return FormValidation.error(
                            "Doesn't look like properly comma separated environment names. Each must be from 4 to 40 characters in length. The name can contain only letters, numbers, and hyphens. It cannot start or end with a hyphen");
                }
            } else {
                if (!value.matches("^\\p{Alpha}[\\p{Alnum}\\-]{0,39}$") || value.endsWith("-")) {
                    return FormValidation.error(
                            "Doesn't look like an environment name. Must be from 4 to 40 characters in length. The name can contain only letters, numbers, and hyphens. It cannot start or end with a hyphen");
                }
            }

            return FormValidation.ok();
        }

        public FormValidation doValidateCredentials(
                @QueryParameter("credentialId") final String credentialId,
                @QueryParameter final String awsRegion) {
            for (String value : Arrays.asList(credentialId, awsRegion)) {
                if (value.contains("$")) {
                    return FormValidation.warning("Validation skipped due to parameter usage ('$')");
                }
            }

            StringWriter stringWriter = new StringWriter();
            PrintWriter w = new PrintWriter(stringWriter, true);

            try {
                w.printf("<ul>%n");

                w.printf("<li>Building Client (credentialId: '%s', region: '%s')</li>%n", Util.escape(credentialId),
                        Util.escape(awsRegion));

                AWSClientFactory factory = AWSClientFactory.getClientFactory(credentialId, awsRegion);

                AmazonS3Client amazonS3 = factory.getService(AmazonS3Client.class);

                String s3Endpoint = factory.getEndpointFor(amazonS3);

                w.printf("<li>Testing Amazon S3 Service (endpoint: %s)</li>%n", Util.escape(s3Endpoint));

                w.printf("<li>Buckets Found: %d</li>%n", amazonS3.listBuckets().size());

                AWSElasticBeanstalkClient
                        awsElasticBeanstalk =
                        factory.getService(AWSElasticBeanstalkClient.class);

                String
                        awsEBEndpoint =
                        factory.getEndpointFor(awsElasticBeanstalk);

                w.printf("<li>Testing AWS Elastic Beanstalk Service (endpoint: %s)</li>%n",
                        Util.escape(awsEBEndpoint));

                List<String>
                        applicationList =
                        Lists.transform(awsElasticBeanstalk.describeApplications().getApplications(),
                                new Function<ApplicationDescription, String>() {
                                    @Override
                                    public String apply(ApplicationDescription input) {
                                        return input.getApplicationName();
                                    }
                                });

                w.printf("<li>Applications Found: %d (%s)</li>%n", applicationList.size(),
                        Util.escape(StringUtils.join(applicationList, ", ")));

                w.printf("</ul>%n");

                return FormValidation.okWithMarkup(stringWriter.toString());
            } catch (Exception exc) {
                return FormValidation.error(exc, "Failure");
            }
        }

        public FormValidation doValidateCoordinates(@QueryParameter("credentialId") String credentialId,
                                                    @QueryParameter("awsRegion") String awsRegion,
                                                    @QueryParameter("applicationName") String applicationName,
                                                    @QueryParameter("environmentName") String environmentName,
                                                    @QueryParameter("skipEnvironmentUpdates") boolean skipEnvironmentUpdates
        )
                throws Exception {
            if (skipEnvironmentUpdates) {
                return FormValidation.ok("Skipping Environment Updates");
            }


            for (String value : Arrays
                    .asList(credentialId, awsRegion, applicationName, environmentName)) {
                if (value.contains("$")) {
                    return FormValidation.warning("Validation skipped due to parameter usage ('$')");
                }
            }

            List<String> environmentNames = Lists.newArrayList(environmentName.replaceAll("\\s", "").split(","));

            AWSClientFactory clientFactory = AWSClientFactory.getClientFactory(credentialId, awsRegion);

            AWSElasticBeanstalk
                    awsElasticBeanstalk =
                    clientFactory.getService(AWSElasticBeanstalkClient.class);

            DescribeEnvironmentsResult
                    describeEnvironmentsResult =
                    awsElasticBeanstalk.describeEnvironments(
                            new DescribeEnvironmentsRequest().withApplicationName(applicationName)
                                    .withIncludeDeleted(false)
                                    .withEnvironmentNames(environmentNames));

            // Validate multiple environments & display IDs
            if (describeEnvironmentsResult.getEnvironments().size() > 1) {
                List<String> environmentIds = Lists.newArrayList();
                Integer size = describeEnvironmentsResult.getEnvironments().size();
                for (Integer i = 0; i < size; i++) {
                    environmentIds.add(describeEnvironmentsResult.getEnvironments().get(i).getEnvironmentId());
                }

                return FormValidation.ok("Multiple environments found (environmentIDs: %s)", StringUtils.join(environmentIds, ", "));
            }

            // Validate single environment & display ID
            if (describeEnvironmentsResult.getEnvironments().size() == 1) {
                String
                        environmentId =
                        describeEnvironmentsResult.getEnvironments().get(0).getEnvironmentId();
                return FormValidation.ok("Environment found (environmentId: %s)", environmentId);
            }

            return FormValidation.error("Environment not found");
        }

        public FormValidation doValidateUpload(@QueryParameter("applicationName") String applicationName,
                                               @QueryParameter("bucketName") String bucketName,
                                               @QueryParameter("keyPrefix") String keyPrefix,
                                               @QueryParameter("versionLabelFormat") String versionLabelFormat) {

            String objectKey = Utils.formatPath("%s/%s-%s.zip",
                    defaultIfBlank(keyPrefix, "<ERROR: MISSING KEY PREFIX>"),
                    defaultIfBlank(applicationName, "<ERROR: MISSING APPLICATION NAME>"),
                    defaultIfBlank(versionLabelFormat, "<ERROR: MISSING VERSION LABEL FORMAT>"));

            String targetPath = Util.escape(String.format("s3://%s/%s",
                    defaultIfBlank(bucketName, "[default account bucket for region]"),
                    objectKey));

            final String resultingMessage = format("Your object will be uploaded to S3 as: <code>%s</code> (<i>note replacements will apply</i>)", targetPath);

            return FormValidation.okWithMarkup(resultingMessage);
        }
    }

    public static class AWSCredentialsListBoxModel extends
            AbstractIdCredentialsListBoxModel<AWSCredentialsListBoxModel, AmazonWebServicesCredentials> {

        /**
         * {@inheritDoc}
         */
        @NonNull
        protected String describe(@NonNull AmazonWebServicesCredentials c) {
            return CredentialsNameProvider.name(c);
        }
    }

}
