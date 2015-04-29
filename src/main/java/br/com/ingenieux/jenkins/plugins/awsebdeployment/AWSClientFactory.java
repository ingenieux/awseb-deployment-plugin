package br.com.ingenieux.jenkins.plugins.awsebdeployment;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;

import org.apache.commons.lang.reflect.ConstructorUtils;

import java.lang.reflect.InvocationTargetException;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class AWSClientFactory {

  private AWSCredentialsProvider creds;

  private ClientConfiguration clientConfiguration;

  private String region;

  public AWSClientFactory(AWSCredentialsProvider creds, ClientConfiguration clientConfiguration,
                          String region) {
    this.creds = creds;
    this.clientConfiguration = clientConfiguration;
    this.region = region;
  }

  @SuppressWarnings("unchecked")
  public <T> T getService(Class<T> serviceClazz)
      throws NoSuchMethodException, IllegalAccessException,
             InvocationTargetException, InstantiationException {
    
    Class<?> paramTypes[];
    Object params[];

    if (isNotBlank(creds.getCredentials().getAWSAccessKeyId()) && isNotBlank(creds.getCredentials().getAWSSecretKey())) {
        params = new Object[]{creds, clientConfiguration};
        paramTypes = new Class<?>[]{AWSCredentialsProvider.class, ClientConfiguration.class};
    } else {
        //let the AWS SDK pick credentials from environment or IAM instance role
        params = new Object[]{clientConfiguration};
        paramTypes = new Class<?>[]{ClientConfiguration.class};
    }
    
    T resultObj = (T) ConstructorUtils.invokeConstructor(serviceClazz, params, paramTypes);

    if (isNotBlank(region)) {
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
}