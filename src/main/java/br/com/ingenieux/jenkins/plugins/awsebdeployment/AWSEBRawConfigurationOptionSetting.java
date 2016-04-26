package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

public class AWSEBRawConfigurationOptionSetting extends AbstractDescribableImpl<AWSEBRawConfigurationOptionSetting> {
    private String namespace;

    private String optionName;

    private String value;

    public String getNamespace() {
        return namespace;
    }

    public String getOptionName() {
        return optionName;
    }

    public String getValue() {
        return value;
    }

    @DataBoundConstructor
    public AWSEBRawConfigurationOptionSetting(String namespace, String optionName, String value) {
        this.namespace = namespace.trim();
        this.optionName = optionName.trim();
        this.value = value.trim();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AWSEBRawConfigurationOptionSetting> {
        @Override
        public String getDisplayName() {
            return "Configuration Option Setting";
        }
    }
}
