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
import hudson.security.ACL;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Collections;
import java.util.List;

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

        public FormValidation doValidateCredentials(@QueryParameter("credentialsId") String credentialName, @QueryParameter("awsRegion") String region) throws Exception {
            return FormValidation.ok("Meh");
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
