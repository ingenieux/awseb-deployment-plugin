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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.apache.commons.lang.reflect.ConstructorUtils;

import java.util.List;
import java.util.ListIterator;

/**
 * Represents a chain of responsibility of deployment steps
 */
public class DeployerChain {
    private final Function<? super Class<? extends DeployerCommand>, ? extends DeployerCommand> CONSTRUCT_COMMAND = new Function<Class<? extends DeployerCommand>, DeployerCommand>() {
        @Override
        public DeployerCommand apply(Class<? extends DeployerCommand> input) {
            try {
                DeployerCommand cmd = (DeployerCommand) input.newInstance();

                cmd.setDeployerContext(c);

                return cmd;
            } catch (Exception e) {
                throw new RuntimeException("Unable to construct: ", e);
            }
        }
    };

    List<DeployerCommand> commandList;

    final DeployerContext c;

    public DeployerChain(DeployerContext deployerContext) {
        this.c = deployerContext;
    }

    public boolean perform() throws Exception {
        buildCommandList();

        ListIterator<DeployerCommand> itCommand = commandList.listIterator();

        boolean done = false;
        boolean abortedOnPerform = false, abortedOnRelease = false;

        while (itCommand.hasNext()) {
            DeployerCommand nextCommand = itCommand.next();

            boolean mustAbort = nextCommand.perform();

            if (mustAbort) {
                abortedOnPerform = true;
                break;
            }
        }

        while (itCommand.hasPrevious()) {
            DeployerCommand prevCommand = itCommand.previous();

            boolean mustAbort = prevCommand.release();

            if (mustAbort) {
                abortedOnRelease = true;
                break;
            }
        }

        return (abortedOnPerform || abortedOnRelease);
    }

    @SuppressWarnings({"unchecked"})
    private void buildCommandList() {
        List<Class<? extends DeployerCommand>> commandList = Lists.newArrayList(
                DeployerCommand.InitLogger.class,
                DeployerCommand.ValidateParameters.class,
                DeployerCommand.InitAWS.class,
                BuildAndUploadArchive.class,
                DeployerCommand.CreateApplicationVersion.class);

        if (c.deployerConfig.isZeroDowntime()) {
            commandList.add(ZeroDowntime.class);
        } else {
            commandList.add(DeployerCommand.LookupEnvironmentId.class);
            commandList.add(DeployerCommand.UpdateApplicationVersion.class);
        }

        commandList.add(DeployerCommand.ValidateEnvironmentStatus.class);

        this.commandList = Lists.newArrayList(Lists.transform(commandList, CONSTRUCT_COMMAND));
    }
}
