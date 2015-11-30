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
import com.amazonaws.services.elasticbeanstalk.model.ListAvailableSolutionStacksResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.remoting.Channel;
import hudson.security.ACL;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.security.MasterToSlaveCallable;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

/**
 * AWS Elastic Beanstalk Deployment
 */
@SuppressWarnings({"unchecked"})
public class AWSEBDeploymentBuilder extends Builder implements BuildStep {
    @DataBoundConstructor
    public AWSEBDeploymentBuilder(String credentialsId, String awsRegion, String applicationName, String environmentName, String bucketName, String keyPrefix, String versionLabelFormat, String rootObject, String includes, String excludes, boolean zeroDowntime) {
        this.credentialsId = credentialsId;
        this.awsRegion = awsRegion;
        this.applicationName = applicationName;
        this.environmentName = environmentName;
        this.bucketName = bucketName;
        this.keyPrefix = keyPrefix;
        this.versionLabelFormat = versionLabelFormat;
        this.rootObject = rootObject;
        this.includes = includes;
        this.excludes = excludes;
        this.zeroDowntime = zeroDowntime;
    }

    /**
     * Credentials name
     */
    private String credentialsId;

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

    private String versionLabelFormat;

    private String rootObject;

    private String includes;

    private String excludes;

    private boolean zeroDowntime;

    public String getCredentialsId() {
        return credentialsId;
    }

    public String getAwsRegion() {
        return awsRegion;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getVersionLabelFormat() {
        return versionLabelFormat;
    }

    public String getRootObject() {
        return rootObject;
    }

    public String getIncludes() {
        return includes;
    }

    public String getExcludes() {
        return excludes;
    }

    public boolean isZeroDowntime() {
        return zeroDowntime;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
                           BuildListener listener) {
        try {
            DeployerContext deployerContext = new DeployerContext(this, build, launcher, listener);

            DeployerChain deployerChain = new DeployerChain(deployerContext);

            deployerChain.perform();

            return true;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
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

        public AbstractIdCredentialsListBoxModel<?, ?> doFillCredentialsIdItems(@AncestorInPath Item owner) {
            if (owner == null || !owner.hasPermission(Item.CONFIGURE)) {
                return new AWSCredentialsListBoxModel();
            }

            List<AmazonWebServicesCredentials> creds = CredentialsProvider.lookupCredentials(AmazonWebServicesCredentials.class, owner, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());

            return new AWSCredentialsListBoxModel()
                    .withEmptySelection()
                    .withAll(creds);
        }

        @Override
        public boolean configure(StaplerRequest req, net.sf.json.JSONObject json) throws FormException {
            save();

            return true;
        }

        public FormValidation doValidateCredentials(@QueryParameter("credentialsId") String credentialsId, @QueryParameter("awsRegion") String awsRegion) throws Exception {
            CommandResult result = Channel.current().call(new ObtainCredentialsCommand(credentialsId, awsRegion));

            if (result.isSuccessful()) {
                return FormValidation.ok(result.getResult());
            } else {
                return FormValidation.error(result.getException(), result.getResult());
            }
        }
    }


    public static class CommandResult implements Serializable {
        final boolean successful;

        final Exception exception;

        final String result;

        public CommandResult(boolean successful, Exception exception, String result) {
            this.successful = successful;
            this.exception = exception;
            this.result = result;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public Exception getException() {
            return exception;
        }

        public String getResult() {
            return result;
        }
    }

    public static class ObtainCredentialsCommand extends MasterToSlaveCallable<CommandResult, Exception> {
        String credentialsId;

        String awsRegion;

        PrintWriter out;

        StringWriter result;

        public ObtainCredentialsCommand(String credentialsId, String awsRegion) {
            this.credentialsId = credentialsId;
            this.awsRegion = awsRegion;
            this.result = new StringWriter();
            this.out = new PrintWriter(result, true);
        }

        protected void log(String message, Object... args) {
            out.println(format(message, args));
        }

        @Override
        public CommandResult call() throws Exception {
            try {
                log("Creating AWS Client with credentialsId '%s' on region '%s'");

                AWSClientFactory clientFactory = AWSClientFactory.getClientFactory(this.credentialsId, awsRegion);

                log("Instantiating AWS Elastic Beanstalk Client");

                AWSElasticBeanstalk ebService = clientFactory.getService(AWSElasticBeanstalk.class);

                log("Instantiating Amazon S3 Client");

                AmazonS3 s3Service = clientFactory.getService(AmazonS3.class);

                log("Attempting to List Solution Stacks- Failure is ok");

                for (String stack : ebService.listAvailableSolutionStacks().getSolutionStacks()) {
                    log(" * Found Stack: %s", stack);
                }

                log("Attempting to List S3 Buckets - Failure is ok");

                for (Bucket b : s3Service.listBuckets()) {
                    log(" * Found Bucket: %s", b.getName());
                }

                log("Finished");

                return new CommandResult(true, null, result.toString());
            } catch (Exception exc) {
                return new CommandResult(false, exc, result.toString());
            }
        }
    }

    public static class AWSCredentialsListBoxModel extends AbstractIdCredentialsListBoxModel<AWSCredentialsListBoxModel, AmazonWebServicesCredentials> {
        /**
         * {@inheritDoc}
         */
        @NonNull
        protected String describe(@NonNull AmazonWebServicesCredentials c) {
            return CredentialsNameProvider.name(c);
        }
    }

}
