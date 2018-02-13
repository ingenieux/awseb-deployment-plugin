# jenkins-awseb-plugin

[![Join the chat at https://gitter.im/ingenieux/awseb-deployment-plugin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/ingenieux/awseb-deployment-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Jenkins Plugin for AWS Elastic Beanstalk Deployments

## TODO LIST

  * Update Docs
  * Unit Tests (I know, I know)
  * Implement a better deployment pipeline using a Chain of Responsibility
  * Support Job DSL and Workflow Plugins
  * Ensure parameters are properly dealt
    * See https://github.com/jenkinsci/credentials-plugin/issues/35

## Using AWSEB in the pipeline

With credentials saved using Jenkins Credentials plugin, you can use the following snippet to use the code:

```groovy
script {
  def files = findFiles(glob: "build/libs/*.war")
  def date = new Date()
  def version = env.BRANCH_NAME + "-" + sha() + "-" + date.format('yyyyMMdd.HHmm')

  echo """WAR File -> ${files[0].name} ${files[0].path} ${files[0].directory} ${files[0].length} ${files[0].lastModified}"""
  awsEbDeployment zeroDowntime: false,
          awsRegion: "(REGION)",
          credentialId: "(YOUR SAVED CREDS)",
          applicationName: "(APP_NAME)",
          environmentName: "(ENV_NAME)",
          bucketName: "(S3_BUCKET_NAME)",
          keyPrefix: "(S3_BUCKET_PREFIX)",
          rootObject: files[0].path,
          versionLabelFormat: version
}
```
