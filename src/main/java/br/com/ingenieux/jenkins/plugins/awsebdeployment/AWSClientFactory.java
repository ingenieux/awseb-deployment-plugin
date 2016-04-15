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

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.s3.AmazonS3;
import com.cloudbees.jenkins.plugins.awscredentials.AmazonWebServicesCredentials;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.ProxyConfiguration;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.apache.commons.lang.reflect.FieldUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.security.auth.login.CredentialNotFoundException;

import hudson.security.ACL;
import jenkins.model.Jenkins;

import static org.apache.commons.lang.StringUtils.defaultString;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class AWSClientFactory implements Constants {

  private AWSCredentialsProvider creds;

  private ClientConfiguration clientConfiguration;

  private String region;

  private AWSClientFactory(AWSCredentialsProvider creds, ClientConfiguration clientConfiguration,
                           String region) {
    this.creds = creds;
    this.clientConfiguration = clientConfiguration;
    this.region = region.toLowerCase();
  }

  public static AWSClientFactory getClientFactory(AmazonWebServicesCredentials cred,
                                                  String awsRegion) {
    AWSCredentialsProvider credentials = new DefaultAWSCredentialsProviderChain();

    if (null != cred) {
      credentials = new AWSCredentialsProviderChain(new StaticCredentialsProvider(
          new BasicAWSCredentials(cred.getCredentials().getAWSAccessKeyId(),
                                  cred.getCredentials().getAWSSecretKey())));
    }

    ClientConfiguration clientConfig = new ClientConfiguration();

    Jenkins jenkins = Jenkins.getInstance();
    if (jenkins != null && jenkins.proxy != null) {
       ProxyConfiguration proxyConfig = jenkins.proxy;
       clientConfig.setProxyHost(proxyConfig.name);
       clientConfig.setProxyPort(proxyConfig.port);
       if (proxyConfig.getUserName() != null) {
          clientConfig.setProxyUsername(proxyConfig.getUserName());
          clientConfig.setProxyPassword(proxyConfig.getPassword());
      }
    }
    clientConfig.setUserAgent("ingenieux CloudButler/" + Utils.getVersion());

    return new AWSClientFactory(credentials, clientConfig, awsRegion);
  }

  public static AWSClientFactory getClientFactory(String credentialsId, String awsRegion)
      throws CredentialNotFoundException {
    AmazonWebServicesCredentials cred = null;

    if (isNotBlank(credentialsId)) {
      cred = lookupNamedCredential(credentialsId);
    }

    return getClientFactory(cred, awsRegion);
  }

  public static AmazonWebServicesCredentials lookupNamedCredential(String credentialsId)
      throws CredentialNotFoundException {
    List<AmazonWebServicesCredentials> credentialList =
        CredentialsProvider.lookupCredentials(
            AmazonWebServicesCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
            Collections.<DomainRequirement>emptyList());

    AmazonWebServicesCredentials cred =
        CredentialsMatchers.firstOrNull(credentialList,
                                        CredentialsMatchers.allOf(
                                            CredentialsMatchers.withId(credentialsId)));

    if (cred == null) {
      throw new CredentialNotFoundException(credentialsId);
    }
    return cred;
  }

  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> serviceClazz)
      throws NoSuchMethodException, IllegalAccessException,
             InvocationTargetException, InstantiationException {

    Class<?> paramTypes[] = new Class<?>[]{AWSCredentialsProvider.class, ClientConfiguration.class};

    ClientConfiguration newClientConfiguration = new ClientConfiguration(this.clientConfiguration);

    if (AmazonS3.class.isAssignableFrom(serviceClazz)) {
      newClientConfiguration = newClientConfiguration.withSignerOverride("AWSS3V4SignerType");
    } else {
      newClientConfiguration = newClientConfiguration.withSignerOverride(null);
    }

    Object params[] = new Object[]{creds, newClientConfiguration};

    T resultObj = (T) ConstructorUtils.invokeConstructor(serviceClazz, params, paramTypes);

    if (DEFAULT_REGION.equals(defaultString(region, DEFAULT_REGION))) {
      return resultObj;
    } else {
      for (ServiceEndpointFormatter formatter : ServiceEndpointFormatter.values()) {
        if (formatter.matches(resultObj)) {
          ((AmazonWebServiceClient) resultObj).setEndpoint(getEndpointFor(formatter));
          break;
        }
      }
    }

    return resultObj;
  }

  protected String getEndpointFor(ServiceEndpointFormatter formatter) {
    return String.format(formatter.serviceMask, region);
  }

  public <T extends AmazonWebServiceClient> String getEndpointFor(T client) {
    try {
      URI endpointUri = (URI) FieldUtils.readField(client, "endpoint", true);

      return endpointUri.toASCIIString();
    } catch (Exception e) {
      return null;
    }
  }

  private enum ServiceEndpointFormatter {
    ELASTICBEANSTALK(AWSElasticBeanstalk.class, "elasticbeanstalk.%s.amazonaws.com"),
    S3(AmazonS3.class, "s3-%s.amazonaws.com");

    final Class<?> serviceClass;

    final String serviceMask;

    ServiceEndpointFormatter(Class<?> serviceClass, String serviceMask) {
      this.serviceClass = serviceClass;
      this.serviceMask = serviceMask;
    }

    public boolean matches(Object obj) {
      return serviceClass.isAssignableFrom(obj.getClass());
    }
  }
}