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

import br.com.ingenieux.jenkins.plugins.awsebdeployment.AWSClientFactory;
import br.com.ingenieux.jenkins.plugins.awsebdeployment.Constants;
import br.com.ingenieux.jenkins.plugins.awsebdeployment.Utils;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.amazonaws.services.s3.AmazonS3Client;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.apache.commons.lang.Validate;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
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
            if (null == getLogger() && null != getLoggerOut())
                setLogger(new PrintStream(getLoggerOut().getOut(), true));

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
            Validate.notEmpty(getBucketName(), "Empty/blank bucketName parameter");
            Validate.notEmpty(getVersionLabel(), "Empty/blank versionLabel parameter");

            return false;
        }
    }

    /**
     * Represents the AWS Client Creation
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

            return false;
        }
    }

    public static class LookupEnvironmentId extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            DescribeEnvironmentsRequest req = new DescribeEnvironmentsRequest().
                    withApplicationName(getApplicationName()).
                    withEnvironmentNames(getEnvironmentName()).
                    withIncludeDeleted(false);

            DescribeEnvironmentsResult result = getAwseb().describeEnvironments(req);

            if (1 != result.getEnvironments().size()) {
                log("Unable to lookup environmentId. Skipping Update.");

                return true;
            }

            setEnvironmentId(result.getEnvironments().get(0).getEnvironmentId());

            return false;
        }
    }

    public static class UpdateApplicationVersion extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            UpdateEnvironmentRequest req = new UpdateEnvironmentRequest().
                    withEnvironmentId(getEnvironmentId()).
                    withVersionLabel(getVersionLabel());

            log("Updating environmentId '%s' with Version Label set to '%s'", getEnvironmentId(), getVersionLabel());

            getAwseb().updateEnvironment(req);

            return false;
        }
    }

    public static class ValidateEnvironmentStatus extends DeployerCommand {
        @Override
        public boolean perform() throws Exception {
            for (int nAttempt = 1; nAttempt <= DeployerContext.MAX_ATTEMPTS; nAttempt++) {
                log("Checking health of environmentId %s attempt %d/%s", getEnvironmentId(), nAttempt,
                        DeployerContext.MAX_ATTEMPTS);

                List<EnvironmentDescription> environments =
                        getAwseb().describeEnvironments(new DescribeEnvironmentsRequest()
                                .withEnvironmentIds(
                                        Collections.singletonList(getEnvironmentId())))
                                .getEnvironments();

                if (environments.size() != 1) {
                    log("Environment not found. Aborting");

                    return true;
                }

                EnvironmentDescription environmentDescription = environments.get(0);

                if (GREEN_HEALTH.equals(environmentDescription.getHealth()))
                    return false;

                Thread.sleep(TimeUnit.SECONDS.toMillis(SLEEP_TIME));
            }

            log("Environment Update timed-out. Aborting.");

            return true;
        }
    }

}
