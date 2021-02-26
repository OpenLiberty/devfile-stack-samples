# no-custom-image

The provided devfile allows you to start developing your maven built applications without the need of a custom image.

## Prerequisites

- OpenShift Do (ODO) CLI.

- Kubernetes Cluster.


## About The Devfile

- It uses a Volume mounted to /home/user/.m2 for runtime caching.

- No outer loop deployment entries.


## Inner loop development

1. Copy the devfile to your application directory.

```
$ ls /myproject/myApplication
README.md   devfile.yaml    src     pom.xml     
```

2. Create your application component.

```
odo create myApplication
```

3. Push the component to your cluster.

```
odo push
```