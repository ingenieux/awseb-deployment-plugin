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

public class ZeroDowntime extends DeployerCommand {
    List<String> environmentNames;

    String environmentId;
    private String templateName;

    @Override
    public boolean perform() throws Exception {
        environmentNames = generateEnvironmentNames();

        environmentId = lookupEnvironmentIds(environmentNames);

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
                                     List<String> environmentNames) {
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
                .withEnvironmentId(environmentId).withApplicationName(getApplicationName())
                .withTemplateName("tmp-" + getVersionLabel());

        return getAwseb().createConfigurationTemplate(request).getTemplateName();
    }

    private String lookupEnvironmentIds(List<String> environmentNames) throws InvalidEnvironmentsSizeException {
        DescribeEnvironmentsResult environments = getAwseb()
                .describeEnvironments(new DescribeEnvironmentsRequest()
                        .withApplicationName(getApplicationName())
                        .withIncludeDeleted(false));

        for (EnvironmentDescription env : environments.getEnvironments()) {
            if (environmentNames.contains(env.getEnvironmentName())) {
                return env.getEnvironmentId();
            }
        }

        throw new InvalidEnvironmentsSizeException(getApplicationName(), environmentNames.get(0));
    }

    @Override
    public boolean release() throws Exception {
        if (isSuccessfulP()) {
            swapEnvironmentCnames(environmentId, getEnvironmentId());

            deleteTemplateName(templateName);

            terminateEnvironment(environmentId);
        } else {
            log("Rolling back on candidate environmentId '%s'", getEnvironmentId());

            terminateEnvironment(getEnvironmentId());
        }

        return false;
    }

    private void deleteTemplateName(String templateName) {
        log("Excluding template name '%s'", templateName);

        getAwseb().deleteConfigurationTemplate(new DeleteConfigurationTemplateRequest(getApplicationName(), templateName));
    }

    public static class InvalidEnvironmentsSizeException extends Exception {

        private static final long serialVersionUID = 1L;

        private final String applicationName;

        private final String environmentName;

        public InvalidEnvironmentsSizeException(String applicationName, String environmentName) {
            super();
            this.applicationName = applicationName;
            this.environmentName = environmentName;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public String getEnvironmentName() {
            return environmentName;
        }
    }
}
