package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import hudson.Extension;
import hudson.cli.CLICommand;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.SerializationUtils;
import org.kohsuke.args4j.Argument;

import java.io.PrintWriter;

@Extension
public class ElasticBeanstalkDeployerAgent extends CLICommand {
    @Override
    public String getShortDescription() {
        return "AWS Elastic Beanstalk Deployer Agent";
    }

    @Argument
    String originalConfig;

    @Override
    protected int run() throws Exception {
        byte[] byteArr = Base64.decodeBase64(originalConfig);
        Object deserialized = SerializationUtils.deserialize(byteArr);
        DeployerContext context = (DeployerContext) deserialized;

        context.logger = new PrintWriter(stdout, true);

        DeployerChain chain = new DeployerChain(context);

        return chain.perform() ? 1 : 0;
    }
}
