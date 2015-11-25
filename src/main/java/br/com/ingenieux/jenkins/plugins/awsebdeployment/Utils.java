package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import hudson.EnvVars;
import hudson.Util;
import org.apache.commons.lang.StringUtils;

public class Utils {
    public static String formatPath(String mask, Object... args) {
        return strip(String.format(mask, args).replaceAll("/{2,}", ""));
    }

    public static String getValue(String value, EnvVars env) {
        return strip(Util.replaceMacro(value, env));
    }

    private static String strip(String str) {
        return StringUtils.strip(str, "/ ");
    }
}
