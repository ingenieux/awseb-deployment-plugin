/*
 * Copyright 2011 ingenieux Labs
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package br.com.ingenieux.jenkins.plugins.awsebdeployment.steps;

import br.com.ingenieux.jenkins.plugins.awsebdeployment.AWSEBDeploymentBuilder;
import com.google.inject.Inject;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousStepExecution;

public class AWSEBDeploymentStepExecution extends AbstractSynchronousStepExecution<Void> {

  @Inject
  private transient AWSEBDeploymentStep step;

  @StepContextParameter
  private transient TaskListener listener;

  @StepContextParameter
  private transient FilePath ws;

  @StepContextParameter
  private transient Run build;

  @StepContextParameter
  private transient Launcher launcher;

  @Override
  protected Void run() throws Exception {
    listener.getLogger().println("Running AWSEBDeployment step.");

    AWSEBDeploymentBuilder builder = new AWSEBDeploymentBuilder(step.getCredentialId(),
        step.getAwsRegion(),
        step.getApplicationName(),
        step.getEnvironmentName(),
        step.getBucketName(),
        step.getKeyPrefix(),
        step.getVersionLabelFormat(),
        step.getVersionDescriptionFormat(),
        step.getRootObject(),
        step.getIncludes(),
        step.getExcludes(),
        step.isZeroDowntime(),
        step.getSleepTime(),
        step.isCheckHealth(),
        step.getMaxAttempts());

    builder.perform(build, ws, launcher, listener);

    return null;
  }

  private static final long serialVersionUID = 1L;
}