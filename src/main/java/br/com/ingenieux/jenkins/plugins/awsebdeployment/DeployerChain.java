package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import br.com.ingenieux.jenkins.plugins.awsebdeployment.exception.InvalidEnvironmentsSizeException;
import br.com.ingenieux.jenkins.plugins.awsebdeployment.exception.InvalidParametersException;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient;
import com.amazonaws.services.elasticbeanstalk.model.*;
import com.amazonaws.services.s3.AmazonS3Client;
import hudson.FilePath;
import hudson.util.DirScanner;
import org.apache.commons.lang.ArrayUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang.StringUtils.isBlank;

public class DeployerChain {
    final DeployerContext c;

    public DeployerChain(DeployerContext deployerContext) {
        this.c = deployerContext;
    }

    public boolean perform() throws Exception {
        // TODO: Cleanup and Move into a real Chain of Responsibility Pattern
        try {
            validateParameters();
            initAWS();
            String[] environmentNames = generateEnvironmentNames();

            log("Running Version %s", Utils.getVersion());

            uploadArchive();
            ApplicationVersionDescription applicationVersion = createApplicationVersion();
            String environmentId = getEnvironmentId(environmentNames);

            if (c.deployerConfig.isZeroDowntime()) {
                String templateName = createConfigurationTemplate(environmentId);

                String clonedEnvironmentId = createEnvironment(applicationVersion.getVersionLabel(), templateName, environmentNames);

                validateEnvironmentStatus(clonedEnvironmentId);

                swapEnvironmentCnames(environmentId, clonedEnvironmentId);

                terminateEnvironment(environmentId);
            } else {
                updateEnvironment(environmentId);

                validateEnvironmentStatus(environmentId);
            }

            log("q'Apla!");

            return true;
        } catch (InvalidParametersException e) {
            log("Skipping update: %s", e.getMessage());

            return false;
        } catch (InvalidEnvironmentsSizeException e) {
            log("Environment %s/%s not found. Continuing", e.getApplicationName(), e.getEnvironmentName());

            return false;
        }

    }

    private void terminateEnvironment(String environmentId) {
        log("Terminating environment %s", environmentId);

        TerminateEnvironmentRequest request = new TerminateEnvironmentRequest().withEnvironmentId(environmentId);

        c.awseb.terminateEnvironment(request);
    }

    private void swapEnvironmentCnames(String environmentId, String clonedEnvironmentId) throws InterruptedException {
        log("Swapping CNAMEs from environment %s to %s", environmentId, clonedEnvironmentId);

        SwapEnvironmentCNAMEsRequest request = new SwapEnvironmentCNAMEsRequest()
                .withSourceEnvironmentId(environmentId).withDestinationEnvironmentId(clonedEnvironmentId);

        c.awseb.swapEnvironmentCNAMEs(request);

        Thread.sleep(TimeUnit.SECONDS.toMillis(DeployerContext.SLEEP_TIME)); //So the CNAMEs will swap
    }

    private String createEnvironment(String versionLabel, String templateName, String[] environmentNames) {
        log("Creating environment based on application %s/%s from version %s and configuration template %s",
                c.applicationName, c.environmentName, versionLabel, templateName);

        String newEnvironmentName = environmentNames[0];
        for (String environmentName : environmentNames) {
            try {
                getEnvironmentId(environmentName);
            } catch (InvalidEnvironmentsSizeException e) {
                newEnvironmentName = environmentName;

                break;
            }
        }

        CreateEnvironmentRequest request = new CreateEnvironmentRequest()
                .withEnvironmentName(newEnvironmentName).withVersionLabel(versionLabel)
                .withApplicationName(c.applicationName).withTemplateName(templateName);

        return c.awseb.createEnvironment(request).getEnvironmentId();
    }

    private UpdateEnvironmentResult updateEnvironment(String environmentId) throws Exception {
        for (int nAttempt = 1; nAttempt <= DeployerContext.MAX_ATTEMPTS; nAttempt++) {
            log("Update attempt %d/%s", nAttempt, DeployerContext.MAX_ATTEMPTS);

            log("Environment found (environment id=%s). Attempting to update environment to version label %s",
                    environmentId, c.versionLabel);

            UpdateEnvironmentRequest uavReq = new UpdateEnvironmentRequest()
                    .withEnvironmentName(c.environmentName).withVersionLabel(
                            c.versionLabel);

            try {
                UpdateEnvironmentResult result = c.awseb.updateEnvironment(uavReq);
                log("Environment updated (environment id=%s). Attempting to validate environment status.", environmentId);

                return result;
            } catch (Exception exc) {
                log("Problem: " + exc.getMessage());

                if (nAttempt == DeployerContext.MAX_ATTEMPTS) {
                    log("Giving it up");

                    throw exc;
                }

                log("Reattempting in %ds, up to %d", DeployerContext.SLEEP_TIME, DeployerContext.MAX_ATTEMPTS);

                Thread.sleep(TimeUnit.SECONDS.toMillis(DeployerContext.SLEEP_TIME));
            }
        }

        throw new Exception();
    }

    private void validateEnvironmentStatus(String environmentId) throws Exception {
        for (int nAttempt = 1; nAttempt <= DeployerContext.MAX_ATTEMPTS; nAttempt++) {
            log("Checking health of environment %s attempt %d/%s", environmentId, nAttempt, DeployerContext.MAX_ATTEMPTS);

            List<EnvironmentDescription> environments = c.awseb.describeEnvironments(new DescribeEnvironmentsRequest()
                    .withEnvironmentIds(Collections.singletonList(environmentId))).getEnvironments();

            if (environments.size() != 1) {
                throw new InvalidEnvironmentsSizeException(c.applicationName, c.environmentName);
            }

            EnvironmentDescription environmentDescription = environments.get(0);
            if (DeployerContext.GREEN_HEALTH.equals(environmentDescription.getHealth())) {
                return;
            }

            Thread.sleep(TimeUnit.SECONDS.toMillis(DeployerContext.SLEEP_TIME));
        }
    }

    private String getEnvironmentId(String... environmentNames) throws InvalidEnvironmentsSizeException {
        DescribeEnvironmentsResult environments = c.awseb
                .describeEnvironments(new DescribeEnvironmentsRequest()
                        .withApplicationName(c.applicationName)
                        .withIncludeDeleted(false));

        for (EnvironmentDescription description : environments.getEnvironments()) {
            if (ArrayUtils.contains(environmentNames, description.getEnvironmentName())) {
                return description.getEnvironmentId();
            }
        }

        throw new InvalidEnvironmentsSizeException(c.applicationName, environmentNames[0]);
    }

    private String createConfigurationTemplate(String environmentId) {
        log("Creating configuration template from application %s with label %s", c.applicationName, c.versionLabel);

        CreateConfigurationTemplateRequest request = new CreateConfigurationTemplateRequest()
                .withEnvironmentId(environmentId).withApplicationName(c.applicationName)
                .withTemplateName(c.versionLabel);

        return c.awseb.createConfigurationTemplate(request).getTemplateName();
    }

    private ApplicationVersionDescription createApplicationVersion() {
        log("Creating application version %s for application %s for path %s",
                c.versionLabel, c.applicationName, c.s3ObjectPath);

        CreateApplicationVersionRequest cavRequest = new CreateApplicationVersionRequest()
                .withApplicationName(c.applicationName)
                .withAutoCreateApplication(true)
                .withSourceBundle(new S3Location(c.bucketName, c.objectKey))
                .withVersionLabel(c.versionLabel);

        return c.awseb.createApplicationVersion(cavRequest).getApplicationVersion();
    }

    private void uploadArchive() throws Exception {
        File localArchive = getLocalFileObject(c.rootFileObject);
        c.objectKey = Utils.formatPath("%s/%s-%s.zip", c.keyPrefix, c.applicationName,
                c.versionLabel);

        c.s3ObjectPath = "s3://" + Utils.formatPath("%s/%s", c.bucketName, c.objectKey);

        log("Uploading file %s as %s", localArchive.getName(), c.s3ObjectPath);

        c.s3.putObject(c.bucketName, c.objectKey, localArchive);
    }

    private void validateParameters() throws InvalidParametersException {
        c.keyPrefix = c.deployerConfig.getKeyPrefix();
        c.bucketName = c.deployerConfig.getBucketName();
        c.applicationName = c.deployerConfig.getApplicationName();
        c.versionLabel = c.deployerConfig.getVersionLabelFormat();
        c.environmentName = c.deployerConfig.getEnvironmentName();

        if (isBlank(c.environmentName)) {
            throw new InvalidParametersException("Empty/blank environmentName parameter");
        }

        if (isBlank(c.applicationName)) {
            throw new InvalidParametersException("Empty/blank applicationName parameter");
        }

        if (isBlank(c.bucketName)) {
            throw new InvalidParametersException("Empty/blank bucketName parameter");
        }

        if (isBlank(c.versionLabel)) {
            throw new InvalidParametersException("Empty/blank versionLabel parameter");
        }
    }

    private String[] generateEnvironmentNames() {
        List<String> environmentNames = new ArrayList<String>() {{
            add(c.environmentName);
        }};

        if (c.deployerConfig.isZeroDowntime()) {
            String newEnvironmentName = c.environmentName.length() <= DeployerContext.MAX_ENVIRONMENT_NAME_LENGTH - 2 ?
                    c.environmentName : c.environmentName.substring(0, c.environmentName.length() - 2);

            environmentNames.add(newEnvironmentName + "-2");
        }

        return environmentNames.toArray(new String[environmentNames.size()]);
    }

    private void initAWS()
            throws Exception {
        AWSClientFactory factory = AWSClientFactory.getClientFactory(c.deployerConfig.getCredentialId(), c.deployerConfig.getAwsRegion());

        c.s3 = factory.getService(AmazonS3Client.class);
        c.awseb = factory.getService(AWSElasticBeanstalkClient.class);
    }

    void log(String mask, Object... args) {
        c.logger.println(String.format(mask, args));
    }

    private File getLocalFileObject(FilePath rootFileObject) throws Exception {
        File resultFile = File.createTempFile("awseb-", ".zip");

        if (!rootFileObject.isDirectory()) {
            log("Root File Object is a file. We assume its a zip file, which is okay.");

            rootFileObject.copyTo(new FileOutputStream(resultFile));
        } else {
            log("Zipping contents of Root File Object (%s) into tmp file %s (includes=%s, excludes=%s)",
                    rootFileObject.getName(), resultFile.getName(),
                    c.deployerConfig.getIncludes(), c.deployerConfig.getExcludes());

            rootFileObject.zip(new FileOutputStream(resultFile),
                    new DirScanner.Glob(c.deployerConfig.getIncludes(),
                            c.deployerConfig.getExcludes()));
        }

        return resultFile;
    }
}
