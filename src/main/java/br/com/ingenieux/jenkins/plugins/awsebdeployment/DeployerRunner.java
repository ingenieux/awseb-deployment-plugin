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
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Future;
import hudson.remoting.VirtualChannel;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;

import java.io.IOException;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class DeployerRunner {
    private final Launcher launcher;

    private final TaskListener listener;

    private final FilePath workspace;

    private final AWSEBDeploymentConfig config;

    DeployerRunner(Run<?, ?> build, FilePath ws, Launcher launcher, TaskListener listener, AWSEBDeploymentBuilder deploymentBuilder) throws InterruptedException, IOException, MacroEvaluationException {
        this.launcher = launcher;
        this.listener = listener;
        this.workspace = ws;
        this.config = deploymentBuilder.getConfig().replacedCopy(new Utils.Replacer(build, workspace, listener));
    }

    public boolean perform() throws Exception {
        FilePath rootFileObject = new FilePath(this.workspace, config.getRootObject());

        AWSCredentialsProvider provider = new DefaultAWSCredentialsProviderChain();

        String credentialsId = config.getCredentialId();
        if (isNotBlank(credentialsId)) {
            provider = AWSClientFactory.lookupNamedCredential(credentialsId);
        }

        AWSCredentials awsCredentials = provider.getCredentials();
        if(awsCredentials == null) {
            throw new IllegalStateException("Could not determine AWS credentials.");
        }
        AWSEBDeploymentCredentials credentials = new AWSEBDeploymentCredentials(awsCredentials.getAWSAccessKeyId(), awsCredentials.getAWSSecretKey());

        ProxyConfiguration proxy = Jenkins.get().getProxy();
        DeployerContext deployerContext = new DeployerContext(config, rootFileObject, listener, credentials, proxy);

        final VirtualChannel channel = launcher.getChannel();

        if (null == channel)
            throw new IllegalStateException("Null Channel (?)");

        final Future<Boolean>
                booleanFuture =
                channel.callAsync(new SlaveDeployerCallable(deployerContext));

        return booleanFuture.get();
    }
}
