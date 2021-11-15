# m2-repo-volume

Sample devfile for creating and mounting a persistent volume to use as the Maven repository.

## Prerequisites

- OpenShift Do (ODO) CLI.

- Kubernetes Cluster.


## About the Devfile

- It uses the parent definition that makes this devfile a child of the specified Open Liberty Stack devfile.
- A volume is created and mounted to /home/user/.m2
- Dependencies cached in stack image are copied to /home/user/.m2/repository on the first push (this adds some additional time).
- Subsequent redeploys of the pod see improved startup times since dependencies are not re-downloaded.


## Inner Loop Development

1. Add the devfile to your project's root directory.

```
cd <your project root dir>
```
```
$ curl -L https://raw.githubusercontent.com/OpenLiberty/devfile-stack-samples/main/devfiles/m2-repo-volume/devfile.yaml -o devfile.yaml  
```

2. Create your application component.

```
odo create myApplication
```

3. Push the component to your cluster.

```
odo push
```