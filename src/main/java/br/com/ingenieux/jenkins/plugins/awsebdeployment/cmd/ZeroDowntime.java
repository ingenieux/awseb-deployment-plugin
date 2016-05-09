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

import com.amazonaws.services.elasticbeanstalk.model.*;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@SuppressWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class ZeroDowntime extends DeployerCommand {
    List<String> environmentNames;

    String environmentId;

    String templateName;

    @Override
    public boolean perform() throws Exception {
        environmentNames = generateEnvironmentNames();

        EnvironmentDescription environmentDescription = null;

        try {
            environmentDescription = lookupEnvironmentIds(environmentNames);

            environmentId = environmentDescription.getEnvironmentId();

        } catch (InvalidDeploymentTypeException exc) {
            log("Zero Downtime isn't valid for Worker Environments.");

            return true;
        } catch (InvalidEnvironmentsSizeException exc) {
            log("Unable to find any suitable environment. Aborting.");

            return true;
        }

        if (environmentDescription.getVersionLabel().equals(getVersionLabel())) {
            log("The version to deploy and currently used are the same. Even if you overwrite, AWSEB won't allow you to update." +
                    "Skipping.");

            return true;
        }

        templateName = createConfigurationTemplate(environmentId);

        String
                clonedEnvironmentId =
                createEnvironment(getVersionLabel(), templateName, environmentNames);

        setEnvironmentId(clonedEnvironmentId);

        log("From now on, we'll use '%s' as the environmentId, but once finished, we'll swap and replace with '%s'", getEnvironmentId(), environmentId);

        return false;
    }

    private List<String> generateEnvironmentNames() {
        List<String> newEnvironmentNames = Lists.newArrayList(getEnvironmentName());

        boolean lengthyP = getEnvironmentName().length() > (MAX_ENVIRONMENT_NAME_LENGTH - 2);

        String newEnvironmentName = getEnvironmentName();

        if (lengthyP)
            newEnvironmentName = getEnvironmentName().substring(0, getEnvironmentName().length() - 2);

        newEnvironmentNames.add(newEnvironmentName + "-2");

        return newEnvironmentNames;
    }

    private String createEnvironment(String versionLabel, String templateName,
                                     List<String> environmentNames) throws InvalidDeploymentTypeException {
        log("Creating environment based on application %s/%s from version %s and configuration template %s",
                getApplicationName(), getEnvironmentName(), versionLabel, templateName);

        String newEnvironmentName = environmentNames.get(0);

        for (String environmentName : environmentNames) {
            try {
                lookupEnvironmentIds(Collections.singletonList(environmentName));
            } catch (InvalidEnvironmentsSizeException e) {
                newEnvironmentName = environmentName;

                break;
            }
        }

        CreateEnvironmentRequest request = new CreateEnvironmentRequest()
                .withEnvironmentName(newEnvironmentName).withVersionLabel(versionLabel)
                .withApplicationName(getApplicationName()).withTemplateName(templateName);

        return getAwseb().createEnvironment(request).getEnvironmentId();
    }

    private void terminateEnvironment(String environmentId) {
        final DescribeEnvironmentsResult result = getAwseb().describeEnvironments(new DescribeEnvironmentsRequest().withEnvironmentIds(environmentId).withIncludeDeleted(false));

        if (result.getEnvironments().isEmpty()) {
            log("Environment environmentId '%s' was already finished.");
            return;
        }

        log("Terminating environment %s", environmentId);

        TerminateEnvironmentRequest
                request =
                new TerminateEnvironmentRequest().withEnvironmentId(environmentId);

        getAwseb().terminateEnvironment(request);
    }

    private void swapEnvironmentCnames(String environmentId, String clonedEnvironmentId)
            throws InterruptedException {
        log("Swapping CNAMEs from environment %s to %s", environmentId, clonedEnvironmentId);

        SwapEnvironmentCNAMEsRequest request = new SwapEnvironmentCNAMEsRequest()
                .withSourceEnvironmentId(environmentId).withDestinationEnvironmentId(clonedEnvironmentId);

        getAwseb().swapEnvironmentCNAMEs(request);

        Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_TIME / 6)); //So the CNAMEs will swap
    }

    private String createConfigurationTemplate(String environmentId) {
        log("Creating configuration template from application %s with label %s", getApplicationName(),
                getVersionLabel());

        CreateConfigurationTemplateRequest request = new CreateConfigurationTemplateRequest()
                .withApplicationName(getApplicationName())
                .withEnvironmentId(environmentId)
                .withTemplateName("tmp-" + getVersionLabel());

        return getAwseb().createConfigurationTemplate(request).getTemplateName();
    }

    private EnvironmentDescription lookupEnvironmentIds(List<String> environmentNames) throws InvalidEnvironmentsSizeException, InvalidDeploymentTypeException {
        DescribeEnvironmentsResult environments = getAwseb()
                .describeEnvironments(new DescribeEnvironmentsRequest()
                        .withApplicationName(getApplicationName())
                        .withIncludeDeleted(false));

        for (EnvironmentDescription env : environments.getEnvironments()) {
            if (environmentNames.contains(env.getEnvironmentName())) {
                if (WORKER_ENVIRONMENT_TYPE.equals(env.getTier().getName())) {
                    throw new InvalidDeploymentTypeException();
                }

                return env;
            }
        }

        throw new InvalidEnvironmentsSizeException(getApplicationName(), environmentNames.get(0), environments.getEnvironments().size());
    }

    @Override
    public boolean release() throws Exception {
        if (isSuccessfulP()) {
            swapEnvironmentCnames(environmentId, getEnvironmentId());

            deleteTemplateName(templateName);

            terminateEnvironment(environmentId);
        } else if (isNotBlank(getEnvironmentId())) {
            log("Rolling back on candidate environmentId '%s'", getEnvironmentId());

            terminateEnvironment(getEnvironmentId());
        }

        return false;
    }

    private void deleteTemplateName(String templateName) {
        log("Excluding template name '%s'", templateName);

        getAwseb().deleteConfigurationTemplate(new DeleteConfigurationTemplateRequest(getApplicationName(), templateName));
    }

    public static class InvalidDeploymentTypeException extends Exception {
        private static final long serialVersionUID = 1L;

        public InvalidDeploymentTypeException() {
            super("Invalid Deployment Type");
        }
    }

    public static class InvalidEnvironmentsSizeException extends Exception {

        private static final long serialVersionUID = 1L;

        private final String applicationName;

        private final String environmentName;

        private final int environmentCount;

        public InvalidEnvironmentsSizeException(String applicationName, String environmentName, int environmentCount) {
            super();
            this.applicationName = applicationName;
            this.environmentName = environmentName;
            this.environmentCount = environmentCount;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public String getEnvironmentName() {
            return environmentName;
        }

        public int getEnvironmentCount() {
            return this.environmentCount;
        }
    }
}
