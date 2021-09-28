# Binding a Java Microservices JPA app to an In-cluster Operator Managed PostgreSQL Database on minikube

## Introduction

This scenario illustrates binding an odo managed Java MicroServices JPA application to an in-cluster Operator managed PostgreSQL Database in the minikube environment.

## What is odo?

odo is a CLI tool for creating applications on OpenShift and Kubernetes. odo allows developers to concentrate on creating applications without the need to administer a cluster itself. Creating deployment configurations, build configurations, service routes and other OpenShift or Kubernetes elements are all automated by odo.

## Installation and Configuration of the minikube environment
<b>Please Note:</b> The guide only works with minikube configured with kubernetes 1.19.x or lower. odo link cannot link services successfully in a Kubernetes 1.20.x or higher environment as of this writing.

It is recommended tothat users of this guide obtain a suitable system for running minikube with kubernetes. In practice this should be a 4 core system minimum. Before proceeding to the sample application, please follow the instructions for establishing a minikube environment:

### Install and start docker 
Please follow instructions [here](https://docs.docker.com/engine/install/) for your OS distribution.

### Install and start and configure minikube

#### Installing minikube
Follow minikube installation instructions [here](https://minikube.sigs.k8s.io/docs/start/) for your operating system target.

#### Starting minikube
If running as root, minikube will complain that docker should not be run as root as a matter of practice and will abort start up. To proceed, minikube will need to be started in a manner which will override this protection:
```shell
minikube start --force --driver=docker --kubernetes-version=v1.19.8
```

If you are a non-root user, start minikube as normal (will utilize docker by default):
```shell
minikube start --kubernetes-version=v1.19.8
```

#### Configuring minikube
ingress config:<br>
The application requires an ingress addon to allow for routes to be created easily. Configure minikube for ingress by adding ingress as a minikube addon:
```shell
minikube addons enable ingress
```

Note: It is possible that you may face the dockerhub pull rate limit if you do not have a pull secret for your personal free docker hub account in place. During ingress initialization two of the job pods used by ingress may fail to initialize due to pull rate limits. If this happens, and ingress fails to enable, you will have to add a secret for the pulls for the following service accounts:

- ingress-nginx-admission
- ingress-nginx

to add a pull secret for these service accounts: <br>
- switch to the kube-system context:
```shell
kubectl config set-context --current --namespace=kube-system
```

- create a pull secret:
```shell
kubectl create secret docker-registry regcred --docker-server=<your-registry-server> --docker-username=<your-name> --docker-password=<your-pword> --docker-email=<your-email>
```
~~~
        where:
          - <your-registry-server> is the DockerHub Registry FQDN. (https://index.docker.io/v1/)
          - <your-name> is your Docker username.
          - <your-pword> is your Docker password.
          - <your-email> is your Docker email.
~~~

- add this new cred ('regcred' in the example above) to the default service account in minikube:
```shell
kubectl patch serviceaccount ingress-nginx-admission -p '{"imagePullSecrets": [{"name": "regcred"}]}'
```

```shell
kubectl patch serviceaccount ingress-nginx -p '{"imagePullSecrets": [{"name": "regcred"}]}'
```

 Default Service Account Pull Secret patch:<br>
 Much like the ingress service accounts, the default service account will need to be patched with a pull secret configured for your personal docker account. 

 - switch to th edefault context:
 ```shell
 kubectl config set-context --current --namespace=default
```

 - create the same docker-registry secret configured for your docker , now for the default minikube context:
 ```shell
 kubectl create secret docker-registry regcred --docker-server=<your-registry-server> --docker-username=<your-name> --docker-password=<your-pword> --docker-email=<your-email>
 ```


~~~
        where:
          - <your-registry-server> is the DockerHub Registry FQDN. (https://index.docker.io/v1/)
          - <your-name> is your Docker username.
          - <your-pword> is your Docker password.
          - <your-email> is your Docker email.
~~~

- Add this new cred ('regcred' in the example above) to the default service account in minikube:
```shell
kubectl patch serviceaccount default -p '{"imagePullSecrets": [{"name": "regcred"}]}'
```
Kubernetes Dashboard graphical UI config:<br>
 It is helpful to make use of the basic kubernetes dashboard UI to interact with the various kubernetes entities in a graphical way. Please refer to the directions [here](https://minikube.sigs.k8s.io/docs/handbook/dashboard/) for enabling and starting the dashboard. Please note, this require the installation of and access to a desktop environment in order to make use of the dashboard. (GNOME + xrdb for example)

Operator Lifecycle Manager (OLM) config:
Enabling OLM on your minikube instance simplifies installation and upgrades of Operators available from [OperatorHub](https://operatorhub.io). Enable OLM with below command:
```shell
minikube addons enable olm
```

### Installing odo
Please follow the installation instructions outlined in the [odo](https://odo.dev) documentation to install the latest odo CLI (version 2.2.4+).

## Actions to Perform by Users in 2 Roles

In this example there are 2 roles:

* Cluster Admin - Installs the Operators to the cluster
* Application Developer - Imports a Java MicroServices JPA application, creates a DB instance, creates a request to bind the application and DB (to connect the DB and the application).

### Cluster Admin

The cluster admin needs to install 2 Operators into the cluster:

* Service Binding Operator (version 0.9.1+)
* A Backing Service Operator

A Backing Service Operator that is "bind-able," in other
words a Backing Service Operator that exposes binding information in secrets, config maps, status, and/or spec
attributes. The Backing Service Operator may represent a database or other services required by
applications. We'll use Dev4Devs PostgreSQL Operator found in the OperatorHub to
demonstrate a sample use case.

#### Installing the Service Binding Operator

Below `kubectl` command will make the Service Binding Operator available in all namespaces on your minikube:
```shell
kubectl create -f https://operatorhub.io/install/service-binding-operator.yaml
```

#### Installing the DB operator

Below `kubectl` command will make the PostgreSQL Operator available in `my-postgresql-operator-dev4devs-com` namespace of your minikube cluster:
```shell
kubectl create -f https://operatorhub.io/install/postgresql-operator-dev4devs-com.yaml
```
**NOTE**: This Operator will be installed in the "my-postgresql-operator-dev4devs-com" namespace and will be usable from this namespace only.

#### Providing Service Resource Access to the Service Binding Operator

Starting with v0.10.0, the Service Binding Operator, requires explicit permissions to access service resources.

Create a ClusterRole resource.

```shell
cat <<EOF | kubectl apply -f-
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: sbo-service-view
  labels:
    service.binding/controller: "true"
rules:
  - apiGroups:
      - postgresql.dev4devs.com
    resources:
      - databases
    verbs:
      - get
      - list
EOF
```

### Application Developer

#### Importing the demo Java MicroService JPA application

In this example we will use odo to manage a sample [Java MicroServices JPA application](https://github.com/OpenLiberty/application-stack-samples.git).

1. Clone the sample app repo to your system.

```shell
git clone https://github.com/OpenLiberty/application-stack-samples.git
```

2. `cd` to the sample JPA application.

```shell
cd ./application-stack-samples/jpa
```

3. Create a Java Open Liberty component.

- If you want the application to be built and deployed using Maven:

```shell
odo create java-openliberty mysboproj
```

- If you want the application to be built and deployed using Gradle:

```shell
odo create java-openliberty-gradle mysboproj
```

4. Push the application to the cluster.

```shell
odo push 
```

The application is now deployed to the cluster - you can view the status of the cluster and the application test results by streaming the openshift logs to the terminal
```shell
odo log
```

Notice the failing tests due to an UnknownDatabaseHostException:

```shell
[err] Caused by: 
[err] java.net.UnknownHostException: ${DATABASE_CLUSTERIP}
[err] 	at java.base/java.net.AbstractPlainSocketImpl.connect(AbstractPlainSocketImpl.java:220)
[err] 	at java.base/java.net.SocksSocketImpl.connect(SocksSocketImpl.java:392)
[err] 	at java.base/java.net.Socket.connect(Socket.java:609)
[err] 	at org.postgresql.core.PGStream.createSocket(PGStream.java:231)
[err] 	at org.postgresql.core.PGStream.<init>(PGStream.java:95)
[err] 	at org.postgresql.core.v3.ConnectionFactoryImpl.tryConnect(ConnectionFactoryImpl.java:98)
[err] 	at org.postgresql.core.v3.ConnectionFactoryImpl.openConnectionImpl(ConnectionFactoryImpl.java:213)
[err] 	... 86 more
[ERROR] Tests run: 2, Failures: 1, Errors: 1, Skipped: 0, Time elapsed: 0.706 s <<< FAILURE! - in org.example.app.it.DatabaseIT
[ERROR] testGetAllPeople  Time elapsed: 0.33 s  <<< FAILURE!
org.opentest4j.AssertionFailedError: Expected at least 2 people to be registered, but there were only: [] ==> expected: <true> but was: <false>
        at org.example.app.it.DatabaseIT.testGetAllPeople(DatabaseIT.java:67)

[ERROR] testGetPerson  Time elapsed: 0.047 s  <<< ERROR!
java.lang.NullPointerException
        at org.example.app.it.DatabaseIT.testGetPerson(DatabaseIT.java:55)

[INFO]
[INFO] Results:
[INFO]
[ERROR] Failures:
[ERROR]   DatabaseIT.testGetAllPeople:57 Expected at least 2 people to be registered, but there were only: [] ==> expected: <true> but was: <false>
[ERROR] Errors:
[ERROR]   DatabaseIT.testGetPerson:41 NullPointer
[INFO]
[ERROR] Tests run: 2, Failures: 1, Errors: 1, Skipped: 0
[INFO]
[ERROR] Integration tests failed: There are test failures.
```

This issue occurs because the application, currently, does not have access to data needed to access the database. Note that the database instance has not been created yet. This will be resolved by the next sequence of steps.

#### Creating a database to be used by the sample application

Since the PostgreSQL Operator we installed in above step is available only in `my-postgresql-operator-dev4devs-com` namespace, let's first make sure that odo uses this namespace to perform any tasks:

```shell
odo project set my-postgresql-operator-dev4devs-com
```

We can use the PostgreSQL Operator's default configuration to start a Postgres database, but since our application requires specific information about the database, lets make sure that information is properly populated in the database service we start.

1. Display the service providers and services available.

```shell
odo catalog list services

Services available through Operators
NAME                                CRDs
postgresql-operator.v0.1.1          Backup, Database
service-binding-operator.v0.9.1     ServiceBinding, ServiceBinding
```

2. Generate the yaml config of the Database service provided by the postgresql-operator.v0.1.1 operator and store it in a file.

```shell
odo service create postgresql-operator.v0.1.1/Database --dry-run > db.yaml
```

3. Open db.yaml and do the following:

Customize the database name, user, and password values under the `spec:` section as shown:

```yaml
spec:
  databaseName: "sampledb"
  databasePassword: "samplepwd"
  databaseUser: "sampleuser"
```

Customize the resource instance name and add the needed annotations under the `metadata` section as shown:

```yaml
metadata:
  name: sampledatabase
  annotations:
    service.binding/db_name: 'path={.spec.databaseName}'
    service.binding/db_password: 'path={.spec.databasePassword}'
    service.binding/db_user: 'path={.spec.databaseUser}'
```

Adding the annotations ensures that the Service Binding Operator will inject the `databaseName`, `databasePassword` and `databaseUser` spec values into the application. Note that the instance name you configure will be used as part of the name of various artifacts and resource references. Be sure to change it.

4. Generate the Database service devfile configuration.

```shell
odo service create --from-file db.yaml
```

5. Push the updates to the cluster.

```shell
odo push
```

This action creates a Dev4Ddevs Database resource instance, which in turn triggers the creation of a PostgreSQL database instance in the `my-postgresql-operator-dev4devs-com` namespace.

#### Binding the database and the application

The only thing that remains is to bind the PostgreSQL database data to the application.

1. List the available services to which the application can be bound. The PostgreSQL database service should be listed.

```shell
odo service list
```

Output:

```shell
NAME                        MANAGED BY ODO      STATE      AGE
Database/sampledatabase     Yes (mysboproj)     Pushed     50s
```

2. Generate the service binding devfile configuration.

```shell
odo link Database/sampledatabase
```

3. push the updates to the cluster.

```shell
odo push
```

When the updates are pushed to the cluster, a secret containing the database connection information is created and the pod hosting the application is restarted. The new pod now contains the database connection information, from the mentioned secret, as environment variables.

- Inspecting the secret.

 You can do this via the dashboard console. Navigate to `Secrets` and clicking on the secret named `mysboproj-database-sampledatabase`. Notice that it contains 4 pieces of data all related to the connection information for your PostgreSQL database instance.

- Inspecting the pod.

To see the newly set environment variables containing database connection information, issue the following command:

```shell
odo exec -- bash -c 'export | grep DATABASE'
```

Output:

```shell
declare -x DATABASE_CLUSTERIP="172.30.36.67"
declare -x DATABASE_DB_NAME="sampledb"
declare -x DATABASE_DB_PASSWORD="samplepwd"
declare -x DATABASE_DB_USER="sampleuser"
...
```

#### Running the Application

1. Create the URL to access the application through a browser.

- Create the URL.

```shell
odo url create --host $(minikube ip).nip.io
```

- Push the data to the cluster to activate it.

```shell
odo push
```

- To see the URL that was created, list the URL's associated to the application component.

```shell
odo url list
```

Output:

```shell
Found the following URLs for component mysboproj
NAME               STATE      URL                                           PORT     SECURE     KIND
mysboproj-9080     Pushed     http://mysboproj-9080.192.168.49.2.nip.io     9080     false      ingress
```

2. Open a browser and go to the URL shown by the previous step.

- Click `Create New Person` button. 

![main page](./assets/ol-stack-jpa-app-db-bind-browser-main.png)

- Enter a user's name and age via the form shown on the page, and click the `Save`.

![Create Person xhtml page](./assets/ol-stack-jpa-app-db-bind-browser-data-entry.png)

After you save the data to the postgreSQL database, notice that you are re-directed to the PersonList.xhtml page. The data being displayed is retrieved from the database.

![Person List xhtml page](./assets/ol-stack-jpa-app-db-bind-browser-show-data.png)

You may inspect the database instance itself and query the table to see the data in place by using the **psql** command line tool. For that, navigate to the pod hosting the database instance from the OpenShift Console, click on the terminal tab, and issue the following commands:

- To access the sampledb database.
```shell
psql sampledb
```

Sample output:

```shell

sh-4.2$ psql sampledb
psql (9.6.10)
Type "help" for help.

sampledb=# 
```

- To query the database.

```shell
sampledb=# SELECT * FROM person;
```
Sample output:

```shell
 personid | age |  name   
----------+-----+---------
        7 |  52 | Person1
(1 row)

sampledb=# 
```