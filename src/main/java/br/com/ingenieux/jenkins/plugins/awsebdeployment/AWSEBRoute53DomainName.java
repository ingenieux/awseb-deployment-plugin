package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

public class AWSEBRoute53DomainName extends AbstractDescribableImpl<AWSEBRoute53DomainName> implements Serializable {
    static final long serialVersionUID = 1L;

    @Getter
    @Setter
    private String name;


    @DataBoundConstructor
    public AWSEBRoute53DomainName(String name) {
        this.name = name.trim();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<AWSEBRoute53DomainName> {
        @Override
        public String getDisplayName() {
            return "Domain Name";
        }
    }
}
