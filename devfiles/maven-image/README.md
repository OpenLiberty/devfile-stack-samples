# Maven-image

The provided devfile allows you to start developing your maven built applications using an official maven container image.

## Prerequisites

- OpenShift Do (ODO) CLI.

- Kubernetes Cluster.


## About The Devfile

- It uses a Volume mounted to /home/user/.m2 for runtime caching.

- No outer loop deployment entries.


## Inner loop development

1. Add the devfile to your project's root directory.

```
cd <your project root dir>
```
```
$ curl -L https://raw.githubusercontent.com/OpenLiberty/application-stack-samples/master/devfiles/maven-image/devfile.yaml -o devfile.yaml  
```

2. Create your application component.

```
odo create myApplication
```

3. Push the component to your cluster.

```
odo push
```