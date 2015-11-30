package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import jenkins.security.MasterToSlaveCallable;

import java.io.Serializable;

/**
 * Created by aldrin on 30/11/15.
 */
public class SlaveDeployerCallable extends MasterToSlaveCallable<Boolean, Exception> implements Serializable {
	private static final long serialVersionUID = 1L;

    private final DeployerContext deployerContext;

    public SlaveDeployerCallable(DeployerContext deployerContext) {
        this.deployerContext = deployerContext;
    }

    @Override
    public Boolean call() throws Exception {
        DeployerChain deployerChain = new DeployerChain(deployerContext);

        boolean result = deployerChain.perform();

        return Boolean.valueOf(result);

    }
}
