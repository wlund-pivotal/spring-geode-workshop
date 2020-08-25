# Environment Setup for Gemfire Cluster on Kubernetes

## Create Gemfire Cluster
We are going to take a very simple approach to creating our gemfire cluster by entering the following:

```bash
$ kubectl create gemfire-cluster
```
This creates a gemfire cluster in the default namespace. There is a more complete example tha can be found with the docs at
[VMware Tanzu Gemfire](https://tgf.docs.pivotal.io/tgf/beta/create-and-delete.html).  In our simplified approach we will not use security.


## Apply the CRD for your Tanzu GemFire cluster, as in this development environment example:

```bash
$ cat << EOF | kubectl -n gemfire-cluster apply -f -
apiVersion: core.geode.apache.org/v1alpha1
kind: GeodeCluster
metadata:
  name: gemfire1
spec:
  locators:
    replicas: 2
  servers:
    replicas: 2
EOF
```

## check the creation status of the Tanzu GemFire cluster:

```bash

## Connect to the Tanzu GemFire Cluster

```bash
kubectl  exec -it gemfire-locator-0 -- gfsh
```

## Verify Gemfire is working

Since the cluster is deployed for us we need only connect. Do he following:

```bash
gfsh>connect
```


