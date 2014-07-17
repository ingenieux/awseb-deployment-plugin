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


import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * AWS Elastic Beanstalk Deployment
 */
@SuppressWarnings({ "unchecked" })
public class AWSEBDeploymentBuilder extends Builder implements BuildStep {
	@DataBoundConstructor
	public AWSEBDeploymentBuilder(String awsAccessKeyId,
			String awsSecretSharedKey, String awsRegion,
			String applicationName, String environmentName, String bucketName,
			String keyPrefix, String versionLabelFormat, String rootObject,
			String includes, String excludes, boolean deployOptOut) {
		super();
		this.awsAccessKeyId = awsAccessKeyId;
		this.awsSecretSharedKey = awsSecretSharedKey;
		this.awsRegion = awsRegion;
		this.applicationName = applicationName;
		this.environmentName = environmentName;
		this.bucketName = bucketName;
		this.keyPrefix = keyPrefix;
		this.versionLabelFormat = versionLabelFormat;
		this.rootObject = rootObject;
		this.includes = includes;
		this.excludes = excludes;
		this.deployOptOut = deployOptOut;
	}

	/**
	 * Access Key Id
	 */
	private String awsAccessKeyId;

	public String getAwsAccessKeyId() {
		return awsAccessKeyId;
	}

	public void setAwsAccessKeyId(String awsAccessKeyId) {
		this.awsAccessKeyId = awsAccessKeyId;
	}

	/**
	 * Secret Shared Key
	 */
	private String awsSecretSharedKey;

	public String getAwsSecretSharedKey() {
		return awsSecretSharedKey;
	}

	public void setAwsSecretSharedKey(String awsSecretSharedKey) {
		this.awsSecretSharedKey = awsSecretSharedKey;
	}

	/**
	 * AWS Region
	 */
	private String awsRegion;

	public String getAwsRegion() {
		return awsRegion;
	}

	public void setAwsRegion(String awsRegion) {
		this.awsRegion = awsRegion;
	}

	/**
	 * Application Name
	 */
	private String applicationName;

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * Environment Name
	 */
	private String environmentName;

	public String getEnvironmentName() {
		return environmentName;
	}

	public void setEnvironmentName(String environmentName) {
		this.environmentName = environmentName;
	}

	/**
	 * Bucket Name
	 */
	private String bucketName;

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	/**
	 * Key Format
	 */
	private String keyPrefix;

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyFormat) {
		this.keyPrefix = keyFormat;
	}

	private String versionLabelFormat;

	public String getVersionLabelFormat() {
		return versionLabelFormat;
	}

	public void setVersionLabelFormat(String versionLabelFormat) {
		this.versionLabelFormat = versionLabelFormat;
	}

	private String rootObject;

	public String getRootObject() {
		return rootObject;
	}

	public void setRootObject(String rootDirectory) {
		this.rootObject = rootDirectory;
	}

	private String includes;

	public String getIncludes() {
		return includes;
	}

	public void setIncludes(String includes) {
		this.includes = includes;
	}

	private String excludes;

	public String getExcludes() {
		return excludes;
	}

	public void setExcludes(String excludes) {
		this.excludes = excludes;
	}

	private boolean deployOptOut;

    public boolean getDeployOptOut() {
    	return deployOptOut;
    }

    public void setDeployOptOut(boolean deployOptOut) {
    	this.deployOptOut = deployOptOut;
    }

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) {
		try {
			Deployer deployer = new Deployer(this, build, launcher, listener);

			deployer.perform();

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
    public BuildStepDescriptor getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link HelloWorldBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
    	/**
    	 * Access Key Id
    	 */
    	private String awsAccessKeyId;

    	public String getAwsAccessKeyId() {
    		return awsAccessKeyId;
    	}

    	public void setAwsAccessKeyId(String awsAccessKeyId) {
    		this.awsAccessKeyId = awsAccessKeyId;
    	}

    	/**
    	 * Secret Shared Key
    	 */
    	private String awsSecretSharedKey;

    	public String getAwsSecretSharedKey() {
    		return awsSecretSharedKey;
    	}

    	public void setAwsSecretSharedKey(String awsSecretSharedKey) {
    		this.awsSecretSharedKey = awsSecretSharedKey;
    	}

    	/**
    	 * AWS Region
    	 */
    	private String awsRegion;

    	public String getAwsRegion() {
    		return awsRegion;
    	}

    	public void setAwsRegion(String awsRegion) {
    		this.awsRegion = awsRegion;
    	}

    	/**
    	 * Application Name
    	 */
    	private String applicationName;

    	public String getApplicationName() {
    		return applicationName;
    	}

    	public void setApplicationName(String applicationName) {
    		this.applicationName = applicationName;
    	}

    	/**
    	 * Environment Name
    	 */
    	private String environmentName;

    	public String getEnvironmentName() {
    		return environmentName;
    	}

    	public void setEnvironmentName(String environmentName) {
    		this.environmentName = environmentName;
    	}

    	/**
    	 * Bucket Name
    	 */
    	private String bucketName;

    	public String getBucketName() {
    		return bucketName;
    	}

    	public void setBucketName(String bucketName) {
    		this.bucketName = bucketName;
    	}

    	/**
    	 * Key Format
    	 */
    	private String keyPrefix;

    	public String getKeyPrefix() {
    		return keyPrefix;
    	}

    	public void setKeyPrefix(String keyFormat) {
    		this.keyPrefix = keyFormat;
    	}

    	private String versionLabelFormat;

    	public String getVersionLabelFormat() {
    		return versionLabelFormat;
    	}

    	public void setVersionLabelFormat(String versionLabelFormat) {
    		this.versionLabelFormat = versionLabelFormat;
    	}

    	private String rootObject;

    	public String getRootObject() {
    		return rootObject;
    	}

    	public void setRootObject(String rootDirectory) {
    		this.rootObject = rootDirectory;
    	}

    	private String includes;

    	public String getIncludes() {
    		return includes;
    	}

    	public void setIncludes(String includes) {
    		this.includes = includes;
    	}

    	private String excludes;

    	public String getExcludes() {
    		return excludes;
    	}

    	public void setExcludes(String excludes) {
    		this.excludes = excludes;
    	}

		private boolean deployOptOut;

		public boolean getDeployOptOut() {
			return deployOptOut;
		}

		public void setDeployOptOut(boolean deployOptOut) {
			this.deployOptOut = deployOptOut;
		}

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
//            if (value.length() == 0)
//                return FormValidation.error("Please set a name");
//            if (value.length() < 4)
//                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        @SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Deploy into AWS Elastic Beanstalk";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            req.bindJSON(this, formData);

            save();

            return super.configure(req,formData);
        }
    }
}
