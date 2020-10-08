# Open Liberty Stack Customization and Application Deployment.

This sample shows how to customize the default Open Liberty Stack to meet your Java and/or Open Liberty version requriements.

## Procedure

### Build A Custom Base Image

Clone the Open Liberty Stack repo.
```
> git clone https://github.com/OpenLiberty/application-stack.git
```
2. Customize baseimage/Dockerfile.

```
> cd /application-stack/baseImage
```

Update the base OS/JDK.

Example (UBI base OS with openjdk version 8):

`FROM adoptopenjdk/openjdk8-openj9:ubi AS maven`

Update mvn references to `-Dliberty.runtime.version` to build a liberty cache of the specified version:

Example: 

`RUN mvn ... -Dliberty.runtime.version=20.0.0.10`

3. Update the maven compiler target/source in the application's  baseimage/starterapp/pom.xml:

`<maven.compiler.target>8</maven.compiler.target>`
`<maven.compiler.source>8</maven.compiler.source>`

4. Build the new image and push it to your repository.

From baseimage:

`docker build -t <myproject>/java-openliberty-j8-ubi:0.0.1 .`

`docker push <myproject>/java-openliberty-j8-ubi:0.0.1`

For this sample a base image has been built. It can be found here:

`emezari/java-openliberty-j8-ubi:0.0.1`

5. Customize assets/dev/Dockerfile. This file is used for outter loop type deployments.

Replace the base image with the image that was just created.
`FROM  <myproject>/java-openliberty-j11-ubuntu:0.0.1 as compile`

Example:
`FROM  emezari/java-openliberty-j8-ubi:0.0.1`

Replace the Liberty image used with the one that matches your OL and/or Java version.
Images can be found here:

```
UBI based images: https://hub.docker.com/r/openliberty/open-liberty
```

`FROM openliberty/open-liberty:a.b.c.d-kernel-java11-openj9-ubi`

Example:

`FROM openliberty/open-liberty:20.0.0.10-kernel-java11-openj9-ubi`


### Inner Loop Application Deployment

1. Customize assets/dev/devfile.yaml.

Update the component image created in the previous step.

```
parent:
  components:
     - name: devruntime
       container:
         image: <myproject>/java-openliberty-j11-ubuntu:0.0.1
Update references to 
```
Example:
```
parent:
  components:
     - name: devruntime
       container:
         image: emezari/java-openliberty-j8-ubi:0.0.1
Update references to 
```

Update mvn references to `-Dliberty.runtime.version` to the Open Liberty driver version you have already chosen to use in previous steps.

Example:

```
mvn ... -Dliberty.runtime.version=20.0.0.10 ...
```


2. Deploy the application.


Clone the sample app repo to this directory.

```
> git clone https://github.com/OpenLiberty/application-stack-samples.git
```

cd to the custom-jaxrs app

```
> cd application-stack-samples/custom-jaxrs
```

initialize project using odo

```
> odo create sample
```

Create a component using the custom devfile located under assets/dev/

```
> odo create customized --devfile assets/dev/devfile.yaml
```

Perform an initial odo push of the app to the cluster

```
> odo push 
```


### Outter Loop Application deployment

**WIP**

Deploy the application to the cluster.

```
> odo deploy.
```