ImageSleuthClient2
==================

Using the ISClient requires an ImageSleuth account. You may obtain an account at ImageSleuth.com. We offer free trials to allow you to assess the quality of our service.

Once you set up your user account at ImageSleuth.com, login and access your user dashboard where you will find the information needed to run the client under the "download" tab.

Once you have download the executable, navigate to the download directory and type:

java –jar ImageSleuthClient2-1.0-SNAPSHOT.jar

You may run the client in another directory, but you will either have to put the jar file in your classpath or specify the path to the jar file in the command.

You will be able to get the base-url, secret-key and secret-token from the download tab on your user account.

The ImageSleuth API v1 consists of two non-blocking calls:

base-url/api/v1/job, which is a post command that sends the image file to the service via the [file] parameter.
    The post command returns a unique job id [id] upon successful upload of the image file.
base-url/api/v1/results, which is a get command that sends the job id in parameter [id] and retrieves either:
    a 200 code and a results string which represents the JSON results object or
    a code of 202 means that the result is not yet available, so please poll again later.
Each call requires basic authentication, where the secret-key, secret-token represent the user/password respectively.

This version of the client processes POST and GET commands concurrently, so it is suited for processing large repositories of images.
