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

package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import br.com.ingenieux.jenkins.plugins.awsebdeployment.cmd.DeployerContext;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.Future;

import static org.apache.commons.lang.StringUtils.isBlank;

public class DeployerRunner {
    final AbstractBuild<?, ?> build;

    final Launcher launcher;

    final BuildListener listener;

    final AWSEBDeploymentBuilder deploymentBuilder;

    public DeployerRunner(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener, AWSEBDeploymentBuilder deploymentBuilder) {
        this.build = build;
        this.launcher = launcher;
        this.listener = listener;
        this.deploymentBuilder = deploymentBuilder;
    }

    public boolean perform() throws Exception {
        EnvVars environment = build.getEnvironment(listener);

        AWSEBDeploymentConfig
                deploymentConfig =
                deploymentBuilder.asConfig().replacedCopy(new Utils.Replacer(environment));

        FilePath
                rootFileObject =
                new FilePath(build.getWorkspace(), deploymentConfig.getRootObject());

        final DeployerContext
                deployerContext =
                new DeployerContext(deploymentConfig, rootFileObject, listener);

        if (!isBlank(deploymentConfig.getCredentialId())) {
            deploymentConfig.setCredentials(
                    AWSClientFactory.lookupNamedCredential(deploymentConfig.getCredentialId()));
        }

        final Future<Boolean>
                booleanFuture =
                launcher.getChannel().callAsync(new SlaveDeployerCallable(deployerContext));

        return booleanFuture.get();
    }
}
