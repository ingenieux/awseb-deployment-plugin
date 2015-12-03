/*
 * Copyright 2011 ingenieux Labs
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import org.apache.commons.lang.StringUtils;

import java.util.Properties;

import hudson.EnvVars;
import hudson.Util;

public class Utils implements Constants {

    private static String VERSION = DEFAULT_VERSION;

    public static String formatPath(String mask, Object... args) {
        return strip(String.format(mask, args).replaceAll("/{2,}", ""));
    }

    private static String strip(String str) {
        return StringUtils.strip(str, "/ ");
    }

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

    public static class Replacer {

        final EnvVars envVars;

        public Replacer(EnvVars envVars) {
            this.envVars = envVars;
        }

        public String r(String value) {
            return strip(Util.replaceMacro(value, envVars));
        }
    }

}
