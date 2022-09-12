# Order Management

This is an example implementation of a microservice providing a management api for order processing.

## Build and execution

The project uses Gradle as build and execution tool. 

The service requires a running instance of MongoDB database server without authentication enabled.
It can easily be started using `docker run -p 27017:27017 --name order-mongo mongo:latest`.

Once running the service should be available on the port `8080`.


`./gradlew clean run`

## Tests

The project uses [TestContainers](https://www.testcontainers.org/) to test access to MongoDB and thus requires a docker
installation accessible be the current user.

`./gradlew clean check`

## Example requests

Example request can be found in the file `requests.http` and easily be executed using the 
[IntelliJ Rest Client](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html).