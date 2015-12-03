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

package br.com.ingenieux.jenkins.plugins.awsebdeployment.cmd;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.ListIterator;

/**
 * Represents a chain of responsibility of deployment steps
 */
public class DeployerChain {
    List<DeployerCommand> commandList;

    final DeployerContext c;

    public DeployerChain(DeployerContext deployerContext) {
        this.c = deployerContext;
    }

    public boolean perform() throws Exception {
        buildCommandList();

        ListIterator<DeployerCommand> itCommand = commandList.listIterator();

        boolean abortedOnPerform = false;
        boolean abortedOnRelease = false;
        boolean mustAbort;
        Exception resultingException = null;

        while (itCommand.hasNext()) {
            DeployerCommand nextCommand = itCommand.next();

            nextCommand.setDeployerContext(c);

            try {
                mustAbort = nextCommand.perform();
            } catch (Exception exc) {
                mustAbort = true;
                resultingException = exc;
            }

            if (mustAbort) {
                abortedOnPerform = true;
                break;
            }
        }

        while (itCommand.hasPrevious()) {
            DeployerCommand prevCommand = itCommand.previous();

            try {
                mustAbort = prevCommand.release();
            } catch (Exception exc) {
                mustAbort = true;
                resultingException = exc;
            }

            if (mustAbort) {
                abortedOnRelease = true;
                break;
            }
        }

        if (null != resultingException)
            throw resultingException;

        return (abortedOnPerform || abortedOnRelease);
    }

    @SuppressWarnings({"unchecked"})
    private void buildCommandList() {
        this.commandList = Lists.newArrayList(
                new DeployerCommand.InitLogger(),
                new DeployerCommand.ValidateParameters(),
                new DeployerCommand.InitAWS(),
                new BuildAndUploadArchive(),
                new DeployerCommand.CreateApplicationVersion()
        );

        commandList.add(new DeployerCommand.WaitForEnvironment(WaitFor.Status));

        if (c.deployerConfig.isZeroDowntime()) {
            commandList.add(new ZeroDowntime());
        } else {
            commandList.add(new DeployerCommand.LookupEnvironmentId());

            commandList.add(new DeployerCommand.AbortPendingUpdates());

            commandList.add(new DeployerCommand.UpdateApplicationVersion());
        }

        commandList.add(new DeployerCommand.MarkAsSuccessful());
    }
}
