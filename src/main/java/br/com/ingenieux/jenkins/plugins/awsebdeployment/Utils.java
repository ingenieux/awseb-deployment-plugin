package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import hudson.EnvVars;
import hudson.Util;
import org.apache.commons.lang.StringUtils;

import java.util.Properties;

public class Utils implements Constants {
    public static String formatPath(String mask, Object... args) {
        return strip(String.format(mask, args).replaceAll("/{2,}", ""));
    }

    public static class Replacer {
        final EnvVars envVars;

        public Replacer(EnvVars envVars) {
            this.envVars = envVars;
        }

        public String r(String value) {
            return strip(Util.replaceMacro(value, envVars));
        }
    }

    private static String strip(String str) {
        return StringUtils.strip(str, "/ ");
    }

    private static String VERSION = DEFAULT_VERSION;

    public static String getVersion() {
        if (DEFAULT_VERSION.equals(VERSION)) {
            try {
                Properties p = new Properties();

                p.load(Utils.class.getResourceAsStream("version.properties"));

                VERSION = p.getProperty("awseb-deployer-plugin.version");

            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return VERSION;
    }

}
