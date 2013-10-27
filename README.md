jenkins-awseb-plugin
====================

Jenkins Plugin for AWS Elastic Beanstalk

TL;DR
=====

Add a Post Build Step. Here's an example:

![http://content.screencast.com/users/aldrinleal/folders/Snagit/media/f2042a57-892f-4cd0-9400-c6eca2674eba/10.27.2013-16.58.png](Example Config)

Results into this on your logs:

  Copying contents of C:\projetos\sources\awseb-deployment\work\jobs\docs.ingenieux.com.br\workspace\target\ingenieux-docs.war into temp file C:\Users\Aldrin\AppData\Local\Temp\awseb-896909810375432889.zip
  Uploading file C:\Users\Aldrin\AppData\Local\Temp\awseb-896909810375432889.zip as s3://ingenieux-beanstalk-apps/ingenieux-docs/ingenieux-docs-app-2013-10-27_17-03-37.zip
  Creating application version 2013-10-27_17-03-37 for application ingenieux-docs-app for path s3://ingenieux-beanstalk-apps/ingenieux-docs/ingenieux-docs-app-2013-10-27_17-03-37.zip
  Environment not found. Continuing
  Finished: SUCCESS
  Finished: SUCCESS

Enjoy

Gotchas
=======

  * No validation is performed
  * Its not working correctly when the root object is a Directory (it adds the prepending dir name. No good)
  * Doesn't do include/excludes
  * Environment Update hasn't been very tested
