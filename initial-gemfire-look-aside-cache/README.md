<!-- Copyright (C) 2019-Present Pivotal Software, Inc. All rights reserved.

This program and the accompanying materials are made available under the terms of the under the Apache License, Version
2.0 (the "License”); you may not use this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License. -->

# Look-aside Cache Pattern Example

This repo contains provides an example application demonstrating the use of
Tanzu GemFire as a [look-aside cache](https://content.pivotal.io/blog/an-introduction-to-look-aside-vs-inline-caching-patterns).

The application uses [Spring Boot for Apache Geode](https://docs.spring.io/autorepo/docs/spring-boot-data-geode-build/current/reference/html5/) to cache data from the Bikewise.org public REST API. Look-aside caching is enabled with just a few annotations. When serving cached data, the application response time is dramatically improved.

# How to get the app running on local Kubernetes
When running with the beta version of Tanzu GemFire for Kubernetes there are few items you need to remember when in the envinroment.

1.  Starting with Spring Boot 2.3, you can now run a comand and your build tool (maven or gradle) will build the Docker file for you.  For this application if you run ./gradlew bootBuildImage - it create an image for you on your local docker daemon.

2.  create a deployment - kubectl create deployment [my deployment name I make up] --image=[your look-aside-cache-image-name here]. -  this should create you a deployment, replicaset, and pod using the image you build in step 1.

3.  You now need to expose that deployment so that you can access your website:  kubectl expose deployment/deployment-name --type="NodePort" --port 8080

4.  Once the deployment has been exposed you will need to find your host ip address and then add NodePort you created in the previous step.
