/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;

public class Utils implements Constants {

    private static String VERSION = DEFAULT_VERSION;

    public static String formatPath(String mask, Object... args) {
        return strip(String.format(mask, args).replaceAll("(\\/{2,})", "/"));
    }

    private static String strip(String str) {
        return StringUtils.strip(str, "/ ");
    }

    public static String getVersion() {
        if (DEFAULT_VERSION.equals(VERSION)) {
            try (InputStream is = Utils.class.getResourceAsStream("version.properties")) {
                Properties p = new Properties();

                p.load(is);

                VERSION = p.getProperty("awseb-deployer-plugin.version");

            } catch (Exception exc) {
                throw new RuntimeException(exc);
            }
        }

        return VERSION;
    }

    public static class Replacer {

        AbstractBuild<?, ?> build;
        BuildListener listener;

        public Replacer( AbstractBuild<?, ?> build, BuildListener listener )
        {
            this.build = build;
            this.listener = listener;
        }

        public String r( String value )
            throws MacroEvaluationException, IOException, InterruptedException
        {
            return strip( TokenMacro.expandAll( build, listener, value ) );
        }
    }

}
