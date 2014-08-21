package br.com.ingenieux.jenkins.plugins.awsebdeployment;


import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalk;
import com.amazonaws.services.s3.AmazonS3;

public enum ServiceEndpointFormatter {
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