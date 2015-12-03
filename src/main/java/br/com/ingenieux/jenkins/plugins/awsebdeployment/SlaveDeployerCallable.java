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

import java.io.Serializable;

import jenkins.security.MasterToSlaveCallable;

public class SlaveDeployerCallable extends MasterToSlaveCallable<Boolean, Exception>
    implements Serializable {

  private static final long serialVersionUID = 1L;

  private final DeployerContext deployerContext;

  public SlaveDeployerCallable(DeployerContext deployerContext) {
    this.deployerContext = deployerContext;
  }

  @Override
  public Boolean call() throws Exception {
    DeployerChain deployerChain = new DeployerChain(deployerContext);

    return deployerChain.perform();
  }
}
