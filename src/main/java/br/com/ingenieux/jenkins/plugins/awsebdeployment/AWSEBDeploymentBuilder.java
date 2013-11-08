package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.Serializable;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * AWS Elastic Beanstalk Deployment
 */
@SuppressWarnings({ "unchecked", "serial" })
public class AWSEBDeploymentBuilder extends Recorder implements BuildStep, Serializable {
	private DescriptorImpl descriptorImpl;

	public AWSEBDeploymentBuilder(DescriptorImpl impl) {
		this.descriptorImpl = impl;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) {
		try {
			Deployer deployer = new Deployer(this.descriptorImpl);

			deployer.perform(build, launcher, listener);
			return true;
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	/**
	 * Descriptor for {@link AWSEBDeploymentBuilder}. Used as a singleton. The
	 * class is marked as public so that it can be accessed from views.
	 * 
	 * <p>
	 * See
	 * <tt>src/main/resources/hudson/plugins/hello_world/AWSEBDeploymentBuilder/*.jelly</tt>
	 * for the actual HTML fragment for the configuration screen.
	 */
	@Extension
	// This indicates to Jenkins that this is an implementation of an extension
	// point.
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Publisher> implements Serializable {
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

		/**
		 * This human readable name is used in the configuration screen.
		 */
		public String getDisplayName() {
			return "Deploy into AWS Elastic Beanstalk";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData)
				throws FormException {
			req.bindJSON(this, formData);

			save();

			return super.configure(req, formData);
		}

		@Override
		public AWSEBDeploymentBuilder newInstance(StaplerRequest req,
				JSONObject formData)
				throws hudson.model.Descriptor.FormException {
			configure(req, formData);

			return new AWSEBDeploymentBuilder(this);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}
	}
}
