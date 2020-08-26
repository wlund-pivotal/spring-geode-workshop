# Environment Setup for Gemfire Cluster on Kubernetes

## Create Gemfire Cluster

We are going to work in the default namespace. There is a more complete example that can be found with the docs at
[VMware Tanzu Gemfire](https://tgf.docs.pivotal.io/tgf/beta/create-and-delete.html) where the first step is
to create a gemfire-cluster namespace.  


## Apply the CRD for your Tanzu GemFire cluster, as in this development environment example:

```bash
$ cat << EOF | kubectl -n gemfire-cluster apply -f -
apiVersion: core.geode.apache.org/v1alpha1
kind: GeodeCluster
metadata:
  name: gemfire1
spec:
  exposeExternalManagement: true
    replicas: 2
  servers:
    replicas: 2
EOF
```

or you can simply create a yaml file from the contents like gemfire-cluster.yaml:

```yaml
apiVersion: core.geode.apache.org/v1alpha1
kind: GeodeCluster
metadata:
  name: gemfire1
spec:
  exposeExternalManagement: true
  locators:
    replicas: 2
  servers:
    replicas: 2
```
and create the gemfire-cluster with the following command:

```bash
kubectl apply -f gemfire-cluster.yaml
```

## Check the creation status of the Tanzu GemFire cluster:

```bash
 kubectl  get GeodeClusters
```

and you should see an output that looks similar to this:

```bash
NAME       LOCATORS   SERVERS
gemfire1   2/2        1/2
```

##  Start the Tanzu GemFire Shell (GFSH)

```bash
kubectl  exec -it gemfire-locator-0 -- gfsh
```
## Connect to the Tanzu GemFire cluster

Once GFSH is running, we need to connect to the GemFire cluster through the following command:

```bash
gfsh>connect
```

and to see the topology and configuraton of your clustter you can do the following:

```bash
gfsh>list members
```

