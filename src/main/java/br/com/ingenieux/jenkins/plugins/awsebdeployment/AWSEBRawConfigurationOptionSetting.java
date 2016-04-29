package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import java.io.Serializable;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.stapler.DataBoundConstructor;

public class AWSEBRawConfigurationOptionSetting extends AbstractDescribableImpl<AWSEBRawConfigurationOptionSetting> implements Serializable {
    static final long serialVersionUID = 1L;

    @Getter @Setter private String namespace;

    @Getter @Setter private String optionName;

    @Getter @Setter private String value;

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
