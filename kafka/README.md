# Binding a Java reactive Microservice app to an In-cluster Operator managed Kafka messaging system

## Introduction

This scenario illustrates binding an odo managed Java reactive Microservice application to a Strimzi operator that manages Kafka messaging  clusters.

## What is the Open Liberty Application Stack?
The [Open Liberty devfile stack](https://github.com/OpenLiberty/application-stack#open-liberty-application-stack) provides much of the infrastructure (Open Liberty, Maven/Gradle, Open J9, etc.) needed to start developing applications that use Maven or Gradle, and it is made available as Maven and Gradle development images. The devfiles that are provided by the stack use these images as a base to build and run your applications. 

The Open Liberty devfile stack provides two fully configured [devfiles](https://docs.devfile.io/devfile/2.1.0/user-guide/index.html): A [Maven-based devfile](https://github.com/devfile/registry/blob/main/stacks/java-openliberty/devfile.yaml) and a [Gradle-based devfile](https://github.com/devfile/registry/blob/main/stacks/java-openliberty-gradle/devfile.yaml). These devfiles define the environment and steps to build and deploy your application using the Open Liberty runtime.

## What is odo?

[Odo](https://odo.dev) is a simple CLI tool to create devfile-based components that interact directly with your Kubernetes cluster. With odo you can set up the environment, and also build, deploy, access, and debug your application. Directives to manage the environment and application are provided by a component's devfile.

## What is the Service Binding Operator?

The [Service Binding Operator](https://github.com/redhat-developer/service-binding-operator/blob/master/README.md) makes it easier for developers to bind operator managed services (i.e. databases) to applications. More specifically, the operator will collect service data, deploy resources (i.e. secrets, configmaps) containing that data, and will make the data available to the application through the ServiceBinding custom resource it provides.

## What is the Strimzi Operator?

The [Strimzi Cluster Operator](https://strimzi.io/docs/operators/latest/overview.html) simplifies the process of running [Apache Kafka](https://kafka.apache.org/documentation/#gettingStarted) in a Kubernetes cluster by facilitating the deployment and management of Apache Kafka clusters.

## Install the Operators.

### Install the Service Binding operator

Navigate to the `Operators`->`OperatorHub` in the OpenShift console and in the `Developer Tools` category select the `Service Binding Operator` operator (version 0.11.0+)

![Service Binding Operator as shown in OperatorHub](../assets/doc/images/SBO.jpg)

### Install the Strimzi operator

Navigate to the `Operators`->`OperatorHub` in the OpenShift console. Select the `Streaming & Messaging` category and search for the `Strimzi` operator provided by Strimzi.

![Strimzi Operator as shown in OperatorHub](../assets/doc/images/strimzi.png)

### Provide service resource access to the Service Binding Operator

The Service Binding Operator (version 1.0.0+) requires explicit permissions to access service resources.

Grant the Service Binding Operator's controller permission to get/list `Kafka` custom resource instances. The `Kafka` custom resource is provided by the `Strimzi` operator you installed in the previous step.

```yaml
cat <<EOF | kubectl apply -f-
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: sbo-service-view
  labels:
    servicebinding.io/controller":"true"
rules:
  - apiGroups:
      - kafka.strimzi.io
    resources:
      - kafkas
    verbs:
      - get
      - list
EOF
```

## Create a Java Open Liberty based component and the Kafka resource service


1. Create a new project and make it the default.

```
odo project create service-binding-kafka
```

2. Clone the application repository.

```shell
git clone https://github.com/OpenLiberty/application-stack-samples.git && \
cd application-stack-samples/kafka
```

3. Create a Java Open Liberty component.

For this step you can either use gradle or maven to build and deploy your application.

To use Maven:

```shell
odo create java-openliberty reactive
```

To use Gradle:

```shell
odo create java-openliberty-gradle reactive
```

4. Display the service providers and services available on the cluster.

```shell
odo catalog list services
```

Output:

```shell
NAME                                 CRDs
strimzi-cluster-operator.v0.26.0     Kafka, KafkaConnect, KafkaMirrorMaker, KafkaBridge, KafkaTopic, KafkaUser, KafkaConnector, KafkaMirrorMaker2, KafkaRebalance
...
```

5. Generate the `trimzi-cluster-operator.v0.26.0` provided `Kafka` resource/service configuration.

```shell
odo service create strimzi-cluster-operator.v0.26.0/Kafka
```

The generated Kafka resource config in `devfile.yaml` should look like this:

```yaml
- kubernetes:
    inlined: |
      apiVersion: kafka.strimzi.io/v1beta2
      kind: Kafka
      metadata:
        name: kafka
      spec:
        entityOperator:
          topicOperator: {}
          userOperator: {}
        kafka:
          config:
            inter.broker.protocol.version: "3.0"
            log.message.format.version: "3.0"
            offsets.topic.replication.factor: 3
            transaction.state.log.min.isr: 2
            transaction.state.log.replication.factor: 3
          listeners:
          - name: plain
            port: 9092
            tls: false
            type: internal
          - name: tls
            port: 9093
            tls: true
            type: internal
          replicas: 3
          storage:
            type: ephemeral
          version: 3.0.0
        zookeeper:
          replicas: 3
          storage:
            type: ephemeral
  name: kafka
```
6. Increase the application pod's pre-configured memory limit.

Open `devfile.yaml` and change the `memoryLimit` entry to `2560Mi` as shown below.

```yaml
components:
- container:
    memoryLimit: 2560Mi
```

7. Push the updates to the cluster.

```shell
odo push
```

This action deploys the application and creates a Kafka cluster instance.
Use the following command to see the status of the pods:

```shell
oc get pod -n service-binding-kafka --selector app=app
```

Once all the resources are running successfully, you should see output that is similar to this:

```shell
NAME                                     READY   STATUS    RESTARTS   AGE
kafka-entity-operator-54b84b84c8-bcrcq   3/3     Running   0          67s
kafka-kafka-0                            1/1     Running   0          93s
kafka-kafka-1                            1/1     Running   0          93s
kafka-kafka-2                            1/1     Running   0          92s
kafka-zookeeper-0                        1/1     Running   0          2m1s
kafka-zookeeper-1                        1/1     Running   0          2m1s
kafka-zookeeper-2                        1/1     Running   0          2m1s
reactive-app-b5484764f-g56gc             1/1     Running   0          2m3s
```

## Binding the database and the application

The only thing that remains is to bind the Kafka cluster instance data to the application.

1. List the available services to which the application can be bound. The Kafka service should be listed.

```shell
odo service list
```

Ouput:

```shell
NAME                                                    MANAGED BY ODO      STATE      AGE
Kafka/kafka                                             Yes (reactive)     Pushed     3m19s
...
```

2. Generate the ServiceBinding resource devfile configuration.

```shell
odo link Kafka/kafka
```

3. Update the generated ServiceBinding resource config in `devfile.yaml`.

First update the value of `detectBindingResources` to false. There is no other information from the kafka service that 
is needed.
Next, configure an id entry: `id: kafkaService` under the Kafka service definition.
Last, configure a mapping that retrieves the Kafka bootstrap server endpoints and format the output to be easily consumable by the application.

```yaml
- kubernetes
      ...
      kind: ServiceBinding
      spec:
        detectBindingResources: false
        ...
        services:
        - group: kafka.strimzi.io
          kind: Kafka
          name: kafka
          id: kafkaService
          version: v1beta2
        mappings:
        - name: mp.messaging.connector.liberty-kafka.bootstrap.servers
          value: '{{range $index, $listener := .kafkaService.status.listeners }}{{if $index}},{{end}}{{ $listener.bootstrapServers }}{{end}}'
```

After the update, the ServiceBinding resource config in `devfile.yaml` should look like this:

```yaml
- kubernetes:
    inlined: |
      apiVersion: binding.operators.coreos.com/v1alpha1
      kind: ServiceBinding
      metadata:
        creationTimestamp: null
        name: reactive-kafka-kafka
      spec:
        application:
          group: apps
          name: reactive-app
          resource: deployments
          version: v1
        bindAsFiles: false
        detectBindingResources: false
        services:
        - group: kafka.strimzi.io
          kind: Kafka
          name: kafka
          id: kafkaService
          version: v1beta2
        mappings:
        - name: mp.messaging.connector.liberty-kafka.bootstrap.servers
          value: '{{range $index, $listener := .kafkaService.status.listeners }}{{if $index}},{{end}}{{ $listener.bootstrapServers }}{{end}}'
      status:
        secret: ""
  name: reactive-kafka-kafka
```

4. Push the updates to the cluster.

```shell
odo push
```

When the updates are pushed to the cluster, a secret containing the Kafka bootstrap server information is created and the pod hosting the application is restarted. The new pod will contain the Kafka bootstrap server information, from the mentioned secret, as an environment variable.

You can inspect the mentioned secret via the Openshift console (Administrator view). Navigate to `Workloads > Secrets` and clicking on the secret named `reactive-kafka-kafka-*`.

You can also inspect the pod to see the newly set environment variables by issuing the following command:

```shell
odo exec -- bash -c 'env | grep -e bootstrap.servers'
```

Output:

```shell
mp.messaging.connector.liberty-kafka.bootstrap.servers=kafka-kafka-bootstrap.service-binding-demo.svc:9092,kafka-kafka-bootstrap.service-binding-demo.svc:9093
```

## Running the Application

1. Find the URL to access the application through a browser.

```shell
odo url list
```

Output:

```shell
Found the following URLs for component reactive
NAME     STATE      URL                                                              PORT     SECURE     KIND
ep1      Pushed     http://ep1-app-service-binding-kafka.apps.my.cluster.ibm.com     9080     false      route
```

2. Open a browser and access the `/health` and `inventory/systems` endpoints using the URL displayed by the previous command. 

http://ep1-app-service-binding-kafka.apps.my.cluster.ibm.com/health

Output:

```shell
checks	
0	
data	{}
name	"InventoryReadinessCheck"
status	"UP"
1	
data	{}
name	"SystemReadinessCheck"
status	"UP"
2	
data	{}
name	"SystemLivenessCheck"
status	"UP"
3	
data	{}
name	"InventoryLivenessCheck"
status	"UP"
status	"UP"
```

http://ep1-app-service-binding-kafka.apps.my.cluster.ibm.com/inventory/systems

Output:

```shell
0	
hostname	"reactive-app-6dc596f799-smt4q"
systemLoad	0.66
```