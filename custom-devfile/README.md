# Open Liberty Stack Customization and Application Deployment.

This sample shows how to customize the default Open Liberty Stack to meet your Java and/or Open Liberty version requriements.

## Procedure


### Build A Java-OpenLiberty Stack Custom Base Image

1. Clone the Open Liberty Stack repo.

```
git clone https://github.com/OpenLiberty/application-stack.git

cd stackimage
```

2. Build the new base image using your preferred java and Open Liberty driver versions.

- Build the image.

```
docker build  --build-arg LIBERTY_RUNTIME_VERSION=<cutom-liberty-version> --build-arg BASE_IMAGE=<openLiberty-base-image> --build-arg MAVEN_COMPILER_LEVEL=<custom-java-compiler-level> -t <custom-base-ol-stack-image-name> .

Example:
docker build --build-arg LIBERTY_RUNTIME_VERSION=20.0.0.9 --build-arg BASE_IMAGE=adoptopenjdk/openjdk8-openj9:ubi --build-arg MAVEN_COMPILER_LEVEL=1.8  -t myrepo/java-openliberty-20009-j8-ubi:0.0.1 .

```
Where:

BASE_IMAGE = The base adoptopenjdk **UBI** image to use. based in the java version you intend to use.

MAVEN_COMPILER_LEVEL = The maven compiler level that matches the java version you intend to use.

LIBERTY_RUNTIME_VERSION = The liberty runtime version you intend to use.

- Push the image to an accessible repository.

```
docker push <custom-base-ol-stack-image-name>
```

### Deploy An application

1. Clone this repository.

```
git clone https://github.com/OpenLiberty/application-stack-samples.git

cd application-stack-samples
```

2. Customize devfile.yaml.

Replace all entries labeled:

`<cutom-liberty-version>`

`<custom-java-compiler-level>`

`<custom-base-ol-stack-image-name>`

with the custom liberty version (LIBERTY_RUNTIME_VERSION), base UBI image (BASE_IMAGE), and maven compiler level (MAVEN_COMPILER_LEVEL) matching the liberty and java versions you chose when building the custom java-openliberty stack base image.

3. Customize outter-loop/Dockerfile.

- Replace `<custom-base-ol-stack-image-name>` with the java-openliberty stack custom image you created.

- Replace `<openLiberty-base-image>` with the open liberty base image that matches your OL version and/or Java version. UBI based images can be found here: https://hub.docker.com/r/openliberty/open-liberty.

- Replace `<cutom-liberty-version>` and `<custom-java-compiler-level>` entries with the same values you used when customizing devfile.yaml in step 2.


4. Login to your Kubernetes cluster.

```
oc login https://<your_cluster_hostname> -u <username> -p <password>
```

5. Deploy an application.

- Clone your application repo. As an example, we will be using a starter type jaxrs application located here: https://github.com/OpenLiberty/application-stack-intro

```
git clone https://github.com/OpenLiberty/application-stack-intro.git

cd application-stack-intro
```

- Create an assets directory and copy the application-stack-samples directory content previously cloned/updated into it. If you use a directory name other than `assets` or that directory is located somewhere else, update the `alpha.build-dockerfile` path value in the devfile.yaml file.

```
mkdir assets

cp -r <your-path>/application-stack-samples/* assets/.

```

- Create a component using the custom devfile located under assets.

```
odo create customizationSample --devfile assets/devfile.yaml
```

- Push of the application onto the cluster.

```
odo push 
```

6. Validate that the application was deployed successfully. 

```
oc get route -l app.kubernetes.io/instance=customizationsample

Sample output:

NAME                      HOST/PORT                                                     PATH   SERVICES              PORT   TERMINATION   WILDCARD
ep1-customizationsample   ep1-customizationsample-custom.apps.xxxxxxx.yy.zzz.aaa.com   /      customizationsample   9080                 None

```

Open a browser using the shown HOST/PORT. You should see a welcome page with the heading:

`Welcome to your Open Liberty Microservice built with Odo`