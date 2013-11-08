package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

/**
 * AWS Elastic Beanstalk Deployment
 */
@SuppressWarnings({ "unchecked" })
public class AWSEBDeploymentBuilder extends Recorder implements BuildStep {
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
}
