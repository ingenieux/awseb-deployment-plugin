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

import br.com.ingenieux.jenkins.plugins.awsebdeployment.Constants;
import br.com.ingenieux.jenkins.plugins.awsebdeployment.Utils;
import br.com.ingenieux.jenkins.plugins.awsebdeployment.AWSClientFactory;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.amazonaws.services.s3.AmazonS3Client;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import lombok.Data;
import lombok.experimental.Delegate;
import org.apache.commons.lang.Validate;

import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

@Data
public class DeployerCommand implements Constants {
    @Delegate
    protected DeployerContext c;

    public void setDeployerContext(DeployerContext c) {
        this.c = c;
    }

    /**
     * Called on start of chain
     *
     * @return false if successful, true if chain must be aborted
     * @throws Exception Returned Exception
     */
    public boolean perform() throws Exception {
        return false;
    }

    /**
     * Called on end of chain
     *
     * @return false if successful, true if chain must be aborted
     * @throws Exception Returned Exception
     */
    public boolean release() throws Exception {
        return false;
    }

    /**
     * Logger Helper
     *
     * @param message message
     * @param args    args
     */
    protected void log(String message, Object... args) {
        getLogger().printf(message, args);
        getLogger().println();
    }

    /**
     * Represents the logger setup
     */
    public static class InitLogger extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            /**
             * When Running Remotely, we use loggerOut pipe.
             *
             * Locally, we already set logger instance
             */
            if (null == getLogger())
                setLogger(getListener().getLogger());

            log("AWSEB Deployment Plugin Version %s", Utils.getVersion());

            return false;
        }
    }

    /**
     * Represents the initial validation
     */
    public static class ValidateParameters extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            setKeyPrefix(getDeployerConfig().getKeyPrefix());
            setBucketName(getDeployerConfig().getBucketName());
            setApplicationName(getDeployerConfig().getApplicationName());
            setVersionLabel(getDeployerConfig().getVersionLabelFormat());
            setEnvironmentName(getDeployerConfig().getEnvironmentName());

            Validate.notEmpty(getEnvironmentName(), "Empty/blank environmentName parameter");
            Validate.notEmpty(getApplicationName(), "Empty/blank applicationName parameter");

            Validate.notEmpty(getVersionLabel(), "Empty/blank versionLabel parameter");

            Validate.isTrue(getRootFileObject().exists(), "Root Object doesn't exist");

            return false;
        }
    }

    /**
     * Builds the AWS Clients altogether
     */
    public static class InitAWS extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            AWSClientFactory factory;

            if (null != getDeployerConfig().getCredentials()) {
                factory = AWSClientFactory
                        .getClientFactory(getDeployerConfig().getCredentials(), getDeployerConfig().getAwsRegion());
            } else {
                factory = AWSClientFactory.getClientFactory("", getDeployerConfig().getAwsRegion());
            }

            setS3(factory.getService(AmazonS3Client.class));
            setAwseb(factory.getService(AWSElasticBeanstalkClient.class));

            return false;
        }
    }

    /**
     * Creates an Application Version
     */
    public static class CreateApplicationVersion extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            log("Creating application version %s for application %s for path %s",
                    getVersionLabel(), getApplicationName(), getS3ObjectPath());

            CreateApplicationVersionRequest cavRequest = new CreateApplicationVersionRequest()
                    .withApplicationName(getApplicationName())
                    .withAutoCreateApplication(true)
                    .withSourceBundle(new S3Location(getBucketName(), getObjectKey()))
                    .withVersionLabel(getVersionLabel());

            final CreateApplicationVersionResult result = getAwseb().createApplicationVersion(cavRequest);

            log("Created version: %s", result.getApplicationVersion().getVersionLabel());

            return false;
        }
    }

    /**
     * Lookups Environment Id - Aborting Deployment if not found.
     */
    public static class LookupEnvironmentId extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest().
                    withApplicationName(getApplicationName()).
                    withEnvironmentNames(Lists.<String>newArrayList(getEnvironmentName().split(","))).
                    withIncludeDeleted(false);

            DescribeEnvironmentsResult result = getAwseb().describeEnvironments(req);


            if (result.getEnvironments().size() < 1) {
                log("Unable to lookup environmentId. Skipping Update.");

                return true;
            }
            StringBuilder csEnvironmentIds = new StringBuilder();
            for (ListIterator<EnvironmentDescription> i = result.getEnvironments().listIterator(); i.hasNext(); ) {
                EnvironmentDescription element = i.next();
                final String environmentLabel = element.getVersionLabel();
                if (null != environmentLabel && environmentLabel.equals(getVersionLabel())) {
                    log("The version to deploy and currently used are the same. Even if you overwrite, AWSEB won't allow you to update." +
                            "Skipping.");
                    csEnvironmentIds.append(element.getEnvironmentId());
                    if (i.hasNext()) {
                        csEnvironmentIds.append(",");
                    }
                    continue;
                }

                final String environmentId = element.getEnvironmentId();

                log("Using environmentId '%s'", environmentId);
                csEnvironmentIds.append(environmentId);
                if (i.hasNext()) {
                    csEnvironmentIds.append(",");
                }
            }

            setEnvironmentId(csEnvironmentIds.toString());
            return false;
        }
    }

    /**
     * Updates de Application Version
     */
    public static class UpdateApplicationVersion extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            List<String> environmentIds = Lists.newArrayList(getEnvironmentId().split(","));

            for (ListIterator<String> i = environmentIds.listIterator(); i.hasNext(); ) {
                String element = i.next();
                UpdateEnvironmentRequest req = new UpdateEnvironmentRequest().
                        withEnvironmentId(element).
                        withVersionLabel(getVersionLabel());

                log("Updating environmentId '%s' with Version Label set to '%s'", element, getVersionLabel());

                getAwseb().updateEnvironment(req);
            }

            return false;
        }
    }

    /**
     * Waits for the Environment to be Green and Available
     */
    @SuppressWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
    public static class WaitForEnvironment extends DeployerCommand {
        final WaitFor waitFor;

        boolean versionCheck;

        public WaitForEnvironment(WaitFor waitFor) {
            this.waitFor = waitFor;
            this.versionCheck = true;
        }

        public WaitForEnvironment withoutVersionCheck() {
            this.versionCheck = false;

            return this;
        }

        @Override
        public boolean perform() throws Exception {

            List<String> environmentIds = Lists.newArrayList(getEnvironmentId().split(","));

            if (environmentIds.size() < 1) {
                log("Environment not found. Aborting");

                return true;
            }

            boolean moveOn = false;

            for (Integer i = 0; i < environmentIds.size(); i++) {

                Integer maxAttempts = (getDeployerConfig().getMaxAttempts() != null) ? getDeployerConfig().getMaxAttempts() : MAX_ATTEMPTS;

                moveOn = false;


                for (int nAttempt = 1; nAttempt <= maxAttempts; nAttempt++) {

                    List<EnvironmentDescription> environments = getAwseb().describeEnvironments(new DescribeEnvironmentsRequest()
                            .withEnvironmentIds(Lists.<String>newArrayList(getEnvironmentId().split(",")))
                            .withIncludeDeleted(false)
                    ).getEnvironments();

                    String currentEnvironmentId = environments.get(i).getEnvironmentId();

                    EnvironmentDescription environmentDescription = environments.get(i);

                    Long lastMessageTimestamp = System.currentTimeMillis();

                    if (moveOn)
                        break;
                    {
                        final DescribeEventsResult describeEventsResult = getAwseb().describeEvents(
                                new DescribeEventsRequest()
                                        .withEnvironmentId(currentEnvironmentId)
                                        .withStartTime(new Date(lastMessageTimestamp))
                        );

                        for (EventDescription eventDescription : describeEventsResult.getEvents()) {
                            log("%s [%s] %s", eventDescription.getEventDate(), eventDescription.getSeverity(), eventDescription.getMessage());

                            lastMessageTimestamp = Math.max(eventDescription.getEventDate().getTime(), lastMessageTimestamp);
                        }
                    }

                    Integer sleepTime = (getDeployerConfig().getSleepTime() != null) ? getDeployerConfig().getSleepTime() : SLEEP_TIME;
                    Thread.sleep(TimeUnit.SECONDS.toMillis(sleepTime));

                    log("Checking health/status of environmentId %s attempt %d/%s", currentEnvironmentId, nAttempt,
                            maxAttempts);

                    // Before checking status and readiness, we must check version unless told otherwise
                    {
                        final boolean bHasDifferentVersion = !getVersionLabel().equals(getVersionLabel());

                        if (versionCheck) {
                            log("Versions reported: (current=%s, underDeployment: %s). Should I move on? %s",
                                    environmentDescription.getVersionLabel(),
                                    getVersionLabel(),
                                    String.valueOf(bHasDifferentVersion));
                            if (bHasDifferentVersion) {
                                continue;
                            }
                        }
                    }

                    // Check health of the environment
                    final boolean bHealthyP = GREEN_HEALTH.equals(environmentDescription.getHealth());
                    final boolean bReadyP = STATUS_READY.equals(environmentDescription.getStatus());

                    if (WaitFor.Health == waitFor) {
                        if (bHealthyP) {
                            log("Environment Health is 'Green'. Moving on.");

                            moveOn = true;
                        }
                    } else if (WaitFor.Status == waitFor) {
                        if (bReadyP) {
                            log("Environment Status is 'Ready'. Moving on.");

                            moveOn = true;
                        }
                    } else if (WaitFor.Both == waitFor) {
                        if (bReadyP && bHealthyP) {
                            log("Environment Status is 'Ready' and Health is 'Green'. Moving on.");

                            moveOn = true;
                        }
                    }
                }
            }


            if (moveOn) {
                return false;
            }
            log("Environment Update timed-out. Aborting.");
            return true;
        }

        protected boolean checkVersionLabel(String deployedVersionLabel) throws Exception {
            return getVersionLabel().equals(deployedVersionLabel);
        }
    }

    /**
     * Marks the deployment as successful
     */
    public static class MarkAsSuccessful extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            log("Deployment marked as 'successful'. Starting post-deployment cleanup.");

            setSuccessfulP(true);

            return false;
        }
    }

    /**
     * Abort Pending Environment Updates
     */
    public static class AbortPendingUpdates extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            final DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest().
                    withApplicationName(getApplicationName()).
                    withEnvironmentIds(Lists.<String>newArrayList(getEnvironmentId().split(","))).
                    withIncludeDeleted(false);

            final DescribeEnvironmentsResult result = getAwseb().describeEnvironments(req);

            if (result.getEnvironments().size() < 1) {
                log("Environment w/ environmentId '%s' not found. Aborting.", getEnvironmentId());

                return true;
            }

            // If there are any abortable environment updates set to true.
            boolean abort = false;

            for (Integer i = 0; i < result.getEnvironments().size(); i++) {
                String resultingStatus = result.getEnvironments().get(i).getStatus();
                boolean abortableP = result.getEnvironments().get(i).getAbortableOperationInProgress();
                String environmentId = result.getEnvironments().get(i).getEnvironmentId();

                if (!STATUS_READY.equals(resultingStatus)) {
                    if (abortableP) {
                        log("AWS Abortable Environment Update Found. Calling abort on AWSEB Service");

                        getAwseb().abortEnvironmentUpdate(new AbortEnvironmentUpdateRequest().withEnvironmentId(environmentId));

                        log("Environment Update Aborted. Proceeding.");
                        abort = true;
                    }

                } else {
                    log("No pending Environment Updates. Proceeding.");
                }
            }

            // Call wait for status if found an abortable env update.
            if (abort) {
                WaitForEnvironment waitForStatus = new WaitForEnvironment(WaitFor.Status).withoutVersionCheck();

                waitForStatus.setDeployerContext(c);

                return waitForStatus.perform();
            }


            return false;
        }
    }
}
