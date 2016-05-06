package br.com.ingenieux.jenkins.plugins.awsebdeployment.cmd;


import br.com.ingenieux.jenkins.plugins.awsebdeployment.AWSEBRawConfigurationOptionSetting;
import com.amazonaws.services.elasticbeanstalk.model.*;

import java.util.ArrayList;
import java.util.List;

public class CreateOrUpdateEnvironment extends DeployerCommand {

    @Override
    public boolean perform() throws Exception {
        DescribeEnvironmentsRequest deReq = new DescribeEnvironmentsRequest().
                withApplicationName(getApplicationName()).
                withEnvironmentNames(getEnvironmentName()).
                withIncludeDeleted(false);

        DescribeEnvironmentsResult deRes = getAwseb().describeEnvironments(deReq);
        final String environmentId;

        List<ConfigurationOptionSetting> list = new ArrayList<ConfigurationOptionSetting>();
        for(AWSEBRawConfigurationOptionSetting setting :getEnvironmentSettings()){
            ConfigurationOptionSetting configurationOptionSetting = new ConfigurationOptionSetting(
                    setting.getNamespace(),
                    setting.getOptionName(),
                    setting.getValue()
            );
            list.add(configurationOptionSetting);
        }
        ConfigurationOptionSetting[] configurationOptionSettings = list.toArray(new ConfigurationOptionSetting[list.size()]);

        if (1 > deRes.getEnvironments().size()) {
            CreateEnvironmentRequest ceReq = new CreateEnvironmentRequest()
                    .withApplicationName(getApplicationName())
                    .withEnvironmentName(getEnvironmentName())
                    .withCNAMEPrefix(getEnvironmentCNAMEPrefix())
                    .withTemplateName(getEnvironmentTemplateName())
                    .withVersionLabel(getVersionLabel())
                    .withOptionSettings(configurationOptionSettings);

            final CreateEnvironmentResult ceRes = getAwseb().createEnvironment(ceReq);

            environmentId = ceRes.getEnvironmentId();

            log("Created environmentId '%s' with Version Label set to '%s'", environmentId, getVersionLabel());
        } else {
            environmentId = deRes.getEnvironments().get(0).getEnvironmentId();

            UpdateEnvironmentRequest ueReq = new UpdateEnvironmentRequest()
                    .withEnvironmentId(environmentId)
                    .withVersionLabel(getVersionLabel())
                    .withOptionSettings(configurationOptionSettings);

            getAwseb().updateEnvironment(ueReq);

            log("Updated environmentId '%s' with Version Label set to '%s'", environmentId, getVersionLabel());
        }

        setEnvironmentId(environmentId);

        return false;
    }
}
