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

public interface Constants {
    String DEFAULT_REGION = "us-east-1";

    int MAX_ATTEMPTS = 90;

    int SLEEP_TIME = 30;

    int MAX_ENVIRONMENT_NAME_LENGTH = 40;

    String GREEN_HEALTH = "Green";

    String STATUS_READY = "Ready";

    String DEFAULT_VERSION = "UNKNOWN";

    String WORKER_ENVIRONMENT_TYPE = "Worker";
}
