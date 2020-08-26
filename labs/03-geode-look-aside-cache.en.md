# Look-aside Cache Pattern Example

This repo contains provides an example application demonstrating the use of
Tanzu GemFire as a [look-aside cache](https://content.pivotal.io/blog/an-introduction-to-look-aside-vs-inline-caching-patterns).

The application uses [Spring Boot for Apache Geode](https://docs.spring.io/autorepo/docs/spring-boot-data-geode-build/current/reference/html5/) to cache data from the Bikewise.org public REST API. Look-aside caching is enabled with just a few annotations. When serving cached data, the application response time is dramatically improved.

# How to get the app running on local Kubernetes
When running with the beta version of Tanzu GemFire for Kubernetes there are few items you need to remember when in the envinroment.

#Section Two - deploying our application to Kubernetes
The fist thing we need to do is change our default profile from "default" to kubernetes, allowing us to retrieve our gemfire-cluster locators and servers
to connect to.

Open application.properties and uncomment the spring.profiles.active=kubernetes.  This will pick up the additional properties we need for the gemfire
cluster when we deploy our image.

##1.  Starting with Spring Boot 2.3, you can now run a comand and your build tool (maven or gradle) will build the Docker file for you.  For this application if you run ./gradlew bootBuildImage - it creates an image for you on your local docker daemon.

##2. We now need to load the image into the kind environment.

```bash
kind load docker-image docker.io/library/look-aside-cache:0.0.1-SNAPSHOT
```

##3.  create a deployment - kubectl create deployment [my deployment name I make up] --image=[your look-aside-cache-image-name here]. -  this should create you a deployment, replicaset, and pod using the image you build in step 1.

```bash
kubectl create deployment look-aside-cache --image= docker.io/library/look-aside-cache:0.0.1-SNAPSHOT
```

##4.  You now need to expose that deployment so that you can access your website:  kubectl expose deployment/deployment-name --type="NodePort" --port 8080

```bash
kubectl expose deployment look-aside-cache --type="NodePort" --port 8080

You will now need the port exposed through the new service by doing the following:

```bash
kubectl get services
```
and the output should look like the following:

```bash
NAME               TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)          AGE
kubernetes         ClusterIP   10.96.0.1        <none>        443/TCP          4d22h
look-aside-cache   NodePort    10.108.118.222   <none>        8080:*30615*/TCP   17s
```
You will need your NodePort that is highlighed in bold.

##5.  Once the deployment has been exposed you will need to find your internal ip address and then add NodePort you created in the previous step.

```bash
kubectl get nodes -o wide
```

and the output should look something similar to:

```bash
kubectl get nodes -o wide
NAME                 STATUS   ROLES    AGE     VERSION   INTERNAL-IP   EXTERNAL-IP   OS-IMAGE       KERNEL-VERSION   CONTAINER-RUNTIME
kind-control-plane   Ready    master   4d22h   v1.18.2   172.18.0.2    <none>        Ubuntu 19.10   5.4.0-1021-aws   containerd://1.3.3-14-g449e9269
```
