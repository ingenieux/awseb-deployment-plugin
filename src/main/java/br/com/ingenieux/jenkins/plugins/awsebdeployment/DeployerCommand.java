package br.com.ingenieux.jenkins.plugins.awsebdeployment;

public class DeployerCommand implements Constants {
    final DeployerContext c;

    protected DeployerCommand(DeployerContext c) {
        this.c = c;
    }

    /**
     * Returns true if we must block the flow
     *
     * @return true if we must block
     * @throws Exception
     */
    public boolean perform() throws Exception {
        return false;
    }

    public static class ValidateConfiguration extends DeployerCommand {
        public ValidateConfiguration(DeployerContext c) {
            super(c);
        }

        @Override
        public boolean perform() throws Exception {
            return false;
        }
    }
}
