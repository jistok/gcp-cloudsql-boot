# Spring Boot Application Which Uses Google CloudSQL

The purpose of this is to illustrate a way to deploy to Cloud Foundry
a Spring Boot app which uses the GCP CloudSQL service offered by the
[GCP Service Broker](https://github.com/GoogleCloudPlatform/gcp-service-broker).

Refer to [this discussion](https://github.com/GoogleCloudPlatform/gcp-service-broker/issues/135)
for further background on why this is currently challenging.

## The following procedure assumes you are working in a Cloud Foundry installation which has a GCP Service Broker installed.

1. Create an instance of CloudSQL: `cf cs google-cloudsql-mysql small cloudsql-test`
1. This will take a bit of time.  You can monitor its progress using `cf services`; it will initially show `create in progress`, and this will change to `create succeeded` when it's ready.
1. Build the app: `bash ./mvnw clean package -DskipTests`
1. Push the app without starting it: `cf push --no-start`
1. Bind the CloudSQL instance: `cf bs gcp-cloudsql-boot cloudsql-test -c '{"role": "editor"}'`
1. Start the app: `cf start gcp-cloudsql-boot`

## Assuming all goes according to plan, you can try it:

1. Grab your app's URL from the output of that `cf push` command
1. Use Curl to get the current time from the MySQL DB (output is a list, where the first element is just "Greetings from now"):
   ```
   $ curl http://gcp-cloudsql-boot.YOUR_APPS_DOMAIN/now
   ["Greetings from now","2017-10-16 15:02:01.0"]
   ```
