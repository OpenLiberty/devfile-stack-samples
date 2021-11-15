# Maven-image

The provided devfile allows you to start developing your maven built applications using an official maven container image.

## Prerequisites

- OpenShift Do (ODO) CLI.

- Kubernetes Cluster.


## About the Devfile

- It uses a volume mounted to /home/user/.m2 for runtime caching.

- No outer loop deployment entries.


## Inner Loop Development

1. Add the devfile to your project's root directory.

```
cd <your project root dir>
```
```
$ curl -L https://raw.githubusercontent.com/OpenLiberty/devfile-stack-samples/main/devfiles/maven-image/devfile.yaml -o devfile.yaml  
```

2. Create your application component.

```
odo create myApplication
```

3. Push the component to your cluster.

```
odo push
```