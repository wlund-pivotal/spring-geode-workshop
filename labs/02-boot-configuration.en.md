# Spring Boot Auto-configuration for Tanzu GemFire

This lab is a modified version of the guide provided by John Blum at [apache-geode-docs](https://docs.spring.io/spring-boot-data-geode-build/current/reference/html5/guides/boot-configuration.html)

This lab walks you through building a simple Customer Service, Spring Boot application using Apache Geode
to manage Customer interactions. You should already be familiar with Spring Boot and Apache Geode.

By the end of this lesson, you should have a better understanding of what Spring Boot for Apache Geode’s (SBDG)
*auto-configuration* support actually does.

## Application Domain Classes

We will build the Spring Boot, Customer Service application from the ground up.

### `Customer` class

We begin by modeling the data our application needs to manage, namely a `Customer`. 
For this example, the `Customer` class is implemented as follows:

**Customer class.**

    @Region("Customers")
    @EqualsAndHashCode
    @ToString(of = "name")
    @RequiredArgsConstructor(staticName = "newCustomer")
    public class Customer {

        @Id @NonNull @Getter
        private Long id;

        @NonNull @Getter
        private String name;

    }

The `Customer` class uses [Project Lombok](https://projectlombok.org/) to simplify the implementation so we can focus on
the details we care about. Lombok is useful for testing or prototyping purposes. However, using Project Lombok is
optional and in most production applications, and not recommended it.

The `Customer` class is annotated with Spring Data Geode’s (SDG) `@Region` annotation. `@Region`
is a mapping annotation declaring the Apache Geode cache `Region` in which `Customer` data will be persisted.  A `Region` is a distributed version of `java.util.Map`.

Tthe `@Id` annotation is used to designate the `Customer.id` field as
the identifier for `Customer` objects. The identifier is the Key used in the Entry stored in the "Customers"\`Region\`.

### `CustomerRepository` interface

Next, we create a *Data Access Object* (DAO) to persist `Customers` to Apache Geode. We create the DAO
using Spring Data’s *Repository* abstraction:

**CustomerRepository interface.**

    public interface CustomerRepository extends CrudRepository<Customer, Long> {

        Customer findByNameLike(String name);

    }

`CustomerRepository` is a Spring Data `CrudRepository`. `CrudRepository` provides basic CRUD (CREATE, READ, UPDATE,
and DELETE) data access operations along with the ability to define simple queries on `Customers`.

Spring Data Geode will create a proxy implementation for your application-specific *Repository* interfaces, implementing
any query methods you may have explicitly defined on the interface in addition to the data access operations provided in
the `CrudRepository` interface extension.

In addition to the base `CrudRepository` operations, `CustomerRepository` has additionally defined a
`findByNameLike(:String):Customer` query method. 

### `CustomerServiceApplication` (Spring Boot main class)

Now that we have created the basic domain classes of our Customer Service application, we need a main application class
to drive the interactions with Customers:

**CustomerServiceApplication class.**

    @SpringBootApplication
    @EnableClusterAware
    @EnableEntityDefinedRegions(basePackageClasses = Customer.class)
    public class CustomerServiceApplication {

        public static void main(String[] args) {

            new SpringApplicationBuilder(CustomerServiceApplication.class)
                .web(WebApplicationType.NONE)
                .build()
                .run(args);
        }

        @Bean
        ApplicationRunner runner(CustomerRepository customerRepository) {

            return args -> {

                assertThat(customerRepository.count()).isEqualTo(0);

                Customer jonDoe = Customer.newCustomer(1L, "Jon Doe");

                System.err.printf("Saving Customer [%s]%n", jonDoe);

                jonDoe = customerRepository.save(jonDoe);

                assertThat(jonDoe).isNotNull();
                assertThat(jonDoe.getId()).isEqualTo(1L);
                assertThat(jonDoe.getName()).isEqualTo("Jon Doe");
                assertThat(customerRepository.count()).isEqualTo(1);

                System.err.println("Querying for Customer [SELECT * FROM /Customers WHERE name LIKE '%Doe']");

                Customer queriedJonDoe = customerRepository.findByNameLike("%Doe");

                assertThat(queriedJonDoe).isEqualTo(jonDoe);

                System.err.printf("Customer was [%s]%n", queriedJonDoe);
            };
        }
    }

The `CustomerServiceApplication` class is annotated with `@SpringBootApplication`. Therefore, the main class is
a proper Spring Boot application equipped with all the features of Spring Boot (e.g. *auto-configuration*).

We use Spring Boot’s `SpringApplicationBuilder` in the `main` method to configure and bootstrap
the Customer Service application.

Then, we declare a Spring Boot `ApplicationRunner` bean, which is invoked by Spring Boot after the Spring container
(i.e. `ApplicationContext`) has been properly initialized and started. Our `ApplicationRunner` defines the Customer
interactions performed by our Customer Service application.

Specifically, the runner creates a new `Customer` object ("Jon Doe"), saves him to the "Customers" Region, and then
queries for "Jon Doe" using an OQL query with the predicate: `name LIKE '%Doe'`.

`%` is the wildcard for OQL text searches.

## Running the Example

You can build the `CustomerServiceApplication` class from your IDE (e.g. IntelliJ IDEA, Spring Tool Suite, VScode ) or from the command-line with
the `mvn clean package`.

Then run the `CustomerServiceApplication` class from the command-line using the command 

`$ mvn spring-boot:run`

## Auto-configuration for Apache Geode, Take One

While it is not apparent, there is a lot of hidden, intrinsic power provided by Spring Boot Data Geode (SBDG)
in this example.

### Cache instance

First, in order to put anything into Apache Geode you need a cache instance. A cache instance is also required to
create `Regions` which ultimately store the application’s data (state). Again, a `Region` is a Key/Value data
structure, similar to a `java.util.Map`, mapping a Key to a Value or an Object.

SBDG is opinionated and assumes most Apache Geode applications will be client applications in Apache Geode’s
[*Topologies and Communication*](https://geode.apache.org/docs/guide/14/topologies_and_comm/book_intro.html)
Therefore, SBDG auto-configures a `ClientCache` instance by default.

The intrinsic `ClientCache` *auto-configuration* provided by SBDG can be made apparent by disabling it:

**Disabling ClientCache Auto-configuration.**

    @SpringBootApplication(exclude = ClientCacheAutoConfiguration.class)
    @EnableEntityDefinedRegions(basePackageClasses = Customer.class, clientRegionShortcut = ClientRegionShortcut.LOCAL)
    public class CustomerServiceApplication {
      // ...
    }

Note the `exclude` on the `ClientCacheAutoConfiguration.class`.

With the correct log level set, you will see an error message similar to:

**Error resulting from no ClientCache instance.**

2020-08-24 17:26:20,964 ERROR agnostics.LoggingFailureAnalysisReporter:  40 - 

***************************
APPLICATION FAILED TO START
***************************

Description:

Parameter 0 of method runner in com.vmware.spring.geode.workshop.crm.CustomerServiceApplication required a bean of type 'com.vmware.spring.geode.workshop.crm.repo.CustomerRepository' that could not be found.


Action:

Consider defining a bean of type 'com.vmware.spring.geode.workshop.crm.repo.CustomerRepository' in your configuration.


Essentially, the `CustomerRepository` could not be injected into our `CustomerServiceApplication` class,
`ApplicationRunner` bean method because the `CustomerRepository`, which depends on the "Customers" Region,
could not be created. The `CustomerRepository` could not be created because the "Customers" Region
could not be created. The "Customers" Region could not be created because there was no cache instance available
(e.g. `ClientCache`) to create `Regions`, resulting in a trickling effect.

We are going to take advantage of an annotation that allows our developer environment to determine what environment
we are running in and create a local (near) cache or a client server connection through auto configuration via the
following:

**Equivalent ClientCache configuration.**

    @SpringBootApplication
    @EnableClusterAware
    @EnableEntityDefinedRegions(basePackageClasses = Customer.class)
    public class CustomerServiceApplication {
      // ...
    }

Otherwise you would need to explicitly declare the `@ClientCacheApplication` annotation if you were not using SBDG.

### Repository instance

We are also using the Spring Data (Geode) *Repository* infrastructure in the Customer Service application. 

If we disable the Spring Data *Repository* *auto-configuration*:

**Disabling Spring Data Repositories Auto-configuration.**

    @SpringBootApplication(exclude = RepositoriesAutoConfiguration.class)
    @EnableEntityDefinedRegions(basePackageClasses = Customer.class, clientRegionShortcut = ClientRegionShortcut.LOCAL)
    public class CustomerServiceApplication {
      // ...
    }

The application would throw a similar error on startup:

2020-08-24 17:38:49,107  INFO xt.annotation.ConfigurationClassEnhancer: 323 - @Bean method PdxConfiguration.pdxDiskStoreAwareBeanFactoryPostProcessor is non-static and returns an object assignable to Spring's BeanFactoryPostProcessor interface. This will result in a failure to process annotations such as @Autowired, @Resource and @PostConstruct within the method's declaring @Configuration class. Add the 'static' modifier to this method to avoid these container lifecycle issues; see @Bean javadoc for complete details.
2020-08-24 17:38:49,112  INFO xt.annotation.ConfigurationClassEnhancer: 323 - @Bean method RegionTemplateAutoConfiguration.regionTemplateBeanFactoryPostProcessor is non-static and returns an object assignable to Spring's BeanFactoryPostProcessor interface. This will result in a failure to process annotations such as @Autowired, @Resource and @PostConstruct within the method's declaring @Configuration class. Add the 'static' modifier to this method to avoid these container lifecycle issues; see @Bean javadoc for complete details.
2020-08-24 17:38:49,184  INFO trationDelegate$BeanPostProcessorChecker: 335 - Bean 'org.springframework.geode.boot.autoconfigure.DataImportExportAutoConfiguration' of type [org.springframework.geode.boot.autoconfigure.DataImportExportAutoConfiguration$$EnhancerBySpringCGLIB$$cb0836d1] is not eligible for getting processed by all BeanPostProcessors (for example: not eligible for auto-proxying)
2020-08-24 17:38:49,229  WARN ation.AnnotationConfigApplicationContext: 559 - Exception encountered during context initialization - cancelling refresh attempt: org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'runner' defined in com.vmware.spring.geode.workshop.crm.CustomerServiceApplication: Unsatisfied dependency expressed through method 'runner' parameter 0; nested exception is org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'com.vmware.spring.geode.workshop.crm.repo.CustomerRepository' available: expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: {}
2020-08-24 17:38:49,236  INFO ConditionEvaluationReportLoggingListener: 136 - 

Error starting ApplicationContext. To display the conditions report re-run your application with 'debug' enabled.
2020-08-24 17:38:49,307 ERROR agnostics.LoggingFailureAnalysisReporter:  40 - 

***************************
APPLICATION FAILED TO START
***************************

Description:

Parameter 0 of method runner in com.vmware.spring.geode.workshop.crm.CustomerServiceApplication required a bean of type 'com.vmware.spring.geode.workshop.crm.repo.CustomerRepository' that could not be found.


Action:

Consider defining a bean of type 'com.vmware.spring.geode.workshop.crm.repo.CustomerRepository' in your configuration.

In this case, there was simply no proxy implementation for the `CustomerRepository` interface provided by the framework
since the *auto-configuration* was disabled. The `ClientCache` and "Customers" `Region` do exist in this case, though.

The Spring Data *Repository auto-configuration* even takes care of locating our application *Repository* interface
definitions for us.

Without *auto-configuration*, you would need to explicitly:

**Equivalent Spring Data Repositories configuration.**

    @SpringBootApplication(exclude = RepositoriesAutoConfiguration.class)
    @EnableEntityDefinedRegions(basePackageClasses = Customer.class, clientRegionShortcut = ClientRegionShortcut.LOCAL)
    @EnableGemfireRepositories(basePackageClasses = CustomerRepository.class)
    public class CustomerServiceApplication {
      // ...
    }

That is, you would need to explicitly declare the `@EnableGemfireRepositories` annotation and set the `basePackages`
attribute, or the equivalent, type-safe `basePackageClasses` attribute, to the package containing your application
*Repository* interfaces, if you were not using SBDG.

### Entity-defined Regions

So far, the only explicit declaration of configuration in our Customer Service application is
the `@EnableEntityDefinedRegions` annotation.

Using SDG’s `@EnableEntityDefinedRegions` annotation is very convenient and can scan for the Regions
(whether client or server (peer) Regions) required by your application based the entity classes themselves
(e.g. `Customer`). In addition, we are going to take advantage of a very useful annotation for allowing SBDG to
automatically detect our topolocy (e.g. local/near cache or client server):

**Annotation-based config for the "Customers" Region.**

    @SpringBootApplication
    @EnableClusterAware
    @EnableEntityDefinedRegions(basePackageClasses = Customer.class)
    class CustomerServiceApplication { }

The `basePackageClasses` attribute is an alternative to `basePackages`, and a type-safe way to target the packages
(and subpackages) containing the entity classes that your application will persist to Apache Geode. You only need to
choose one class from each top-level package for where you want the scan to begin. Spring Data Geode uses this class
to determine the package to begin the scan. 'basePackageClasses\` accepts an array of `Class` types so you can specify
multiple independent top-level packages. The annotation also includes the ability to filter types.

However, the `@EnableEntityDefinedRegions` annotation only works when the entity class (e.g. `Customer`) is explicitly
annotated with the `@Region` annotation (e.g. `@Region("Customers")`), otherwise it ignores the class. You will find
this annotation on Customer in the model package. 

**Enable Cluster Aware**
Initially we want to get up and running as quickly as possible.  By not starting a locator/region and using @EnableClusterAware we allow SBDG to detect that we are using a client `LOCAL` Region and we
are not required to start a cluster of Apache Geode servers for the client to be able to store data. This allows rapid local development
for developers. 

While client `LOCAL` Regions can be useful for some purposes (e.g. local processing, querying and aggregating of data),
it is more common for a client to persist data in a cluster of servers, and for that data to be shared by multiple
clients (instances) in the application architecture, especially as the application is scaled out to handle demand.

## Switching to Client/Server

We continue with our example by switching from a local context to a client/server topology.

If you are rapidly prototyping and developing your application and simply want to lift off the ground quickly, then it
is useful to start locally and gradually migrate towards a client/server architecture.

To switch to client/server, all you need to do is startup a locator(s) and server(s) and SBDG will automatically
detect your topology. 

**Client/Server Topology Region Configuration.**

    @SpringBootApplication
    @EnableClusterAware
    @EnableEntityDefinedRegions(basePackageClasses = Customer.class)

    class CustomerServiceApplication { }


The @EnableClusterAware auto configuration determines the topology by determining whether there is a cluster of servers to
communicate with and to store/access data from. 

There are several ways in which to start a cluster. For this example, we are going to use the tools provided with Apache Geode, or VMWare Tanzu  GemFire, i.e. *Gfsh* (GemFire/Geode Shell). For this workshop, we have installed GFSH for you.  

To start the command shell locally, open a command/terminal prompt and type
$ gfsh

**Running Gfsh.**


        _________________________     __
       / _____/ ______/ ______/ /____/ /
      / /  __/ /___  /_____  / _____  /
     / /__/ / ____/  _____/ / /    / /
    /______/_/      /______/_/    /_/    1.12.1

    Monitor and Manage Apache Geode
    gfsh>

You are set to go.

The lab environment running on kubernetes was discussed in the lab setup.  Two locators and 2 servers have been started.

Execute the *Gfsh* shell script using:

With our gemfire-cluster create with an two Apache Geode Locators and (Cache) Servers running we can verify by listing
and describing the members:

**List and Describe Members.**

    gfsh>list members
       Name    | Id
    ---------- | ---------------------------------------------------
    LocatorOne | 10.99.199.24(LocatorOne:68425:locator)<ec><v0>:1024
    ServerOne  | 10.99.199.24(ServerOne:68434)<v1>:1025


    gfsh>describe member --name=ServerOne
    Name        : ServerOne
    Id          : 10.99.199.24(ServerOne:68434)<v1>:1025
    Host        : 10.99.199.24
    Regions     :
    PID         : 68434
    Groups      :
    Used Heap   : 27M
    Max Heap    : 3641M
    Working Dir : /Users/jblum/pivdev/lab/ServerOne
    Log file    : /Users/jblum/pivdev/lab/ServerOne/ServerOne.log
    Locators    : 10.99.199.24[10334]

    Cache Server Information
    Server Bind              : null
    Server Port              : 40404
    Running                  : true
    Client Connections       : 0

What happens if we try to run the application now?

**RegionNotFoundException.**

    17:42:16.873 [main] ERROR o.s.b.SpringApplication - Application run failed
    java.lang.IllegalStateException: Failed to execute ApplicationRunner
        ...
        at example.app.crm.CustomerServiceApplication.main(CustomerServiceApplication.java:51) [classes/:?]
    Caused by: org.springframework.dao.DataAccessResourceFailureException: remote server on 10.99.199.24(SpringBasedCacheClientApplication:68473:loner):51142:f9f4573d:SpringBasedCacheClientApplication: While performing a remote query; nested exception is org.apache.geode.cache.client.ServerOperationException: remote server on 10.99.199.24(SpringBasedCacheClientApplication:68473:loner):51142:f9f4573d:SpringBasedCacheClientApplication: While performing a remote query
        at org.springframework.data.gemfire.GemfireCacheUtils.convertGemfireAccessException(GemfireCacheUtils.java:230) ~[spring-data-geode-2.0.14.RELEASE.jar:2.0.14.RELEASE]
        at org.springframework.data.gemfire.GemfireAccessor.convertGemFireAccessException(GemfireAccessor.java:91) ~[spring-data-geode-2.0.14.RELEASE.jar:2.0.14.RELEASE]
        at org.springframework.data.gemfire.GemfireTemplate.find(GemfireTemplate.java:329) ~[spring-data-geode-2.0.14.RELEASE.jar:2.0.14.RELEASE]
        at org.springframework.data.gemfire.repository.support.SimpleGemfireRepository.count(SimpleGemfireRepository.java:129) ~[spring-data-geode-2.0.14.RELEASE.jar:2.0.14.RELEASE]
        ...
        at example.app.crm.CustomerServiceApplication.lambda$runner$0(CustomerServiceApplication.java:59) ~[classes/:?]
        at org.springframework.boot.SpringApplication.callRunner(SpringApplication.java:783) ~[spring-boot-2.0.9.RELEASE.jar:2.0.9.RELEASE]
        ... 3 more
    Caused by: org.apache.geode.cache.client.ServerOperationException: remote server on 10.99.199.24(SpringBasedCacheClientApplication:68473:loner):51142:f9f4573d:SpringBasedCacheClientApplication: While performing a remote query
        at org.apache.geode.cache.client.internal.AbstractOp.processChunkedResponse(AbstractOp.java:352) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.QueryOp$QueryOpImpl.processResponse(QueryOp.java:170) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.AbstractOp.processResponse(AbstractOp.java:230) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.AbstractOp.attempt(AbstractOp.java:394) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.AbstractOp.attemptReadResponse(AbstractOp.java:203) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.ConnectionImpl.execute(ConnectionImpl.java:275) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.pooling.PooledConnection.execute(PooledConnection.java:332) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.OpExecutorImpl.executeWithPossibleReAuthentication(OpExecutorImpl.java:900) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.OpExecutorImpl.execute(OpExecutorImpl.java:158) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.OpExecutorImpl.execute(OpExecutorImpl.java:115) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.PoolImpl.execute(PoolImpl.java:763) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.QueryOp.execute(QueryOp.java:58) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.client.internal.ServerProxy.query(ServerProxy.java:70) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.query.internal.DefaultQuery.executeOnServer(DefaultQuery.java:456) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.query.internal.DefaultQuery.execute(DefaultQuery.java:338) ~[geode-core-1.2.1.jar:?]
        at org.springframework.data.gemfire.GemfireTemplate.find(GemfireTemplate.java:311) ~[spring-data-geode-2.0.14.RELEASE.jar:2.0.14.RELEASE]
        at org.springframework.data.gemfire.repository.support.SimpleGemfireRepository.count(SimpleGemfireRepository.java:129) ~[spring-data-geode-2.0.14.RELEASE.jar:2.0.14.RELEASE]
        ...
        at example.app.crm.CustomerServiceApplication.lambda$runner$0(CustomerServiceApplication.java:59) ~[classes/:?]
        at org.springframework.boot.SpringApplication.callRunner(SpringApplication.java:783) ~[spring-boot-2.0.9.RELEASE.jar:2.0.9.RELEASE]
        ... 3 more
    Caused by: org.apache.geode.cache.query.RegionNotFoundException: Region not found:  /Customers
        at org.apache.geode.cache.query.internal.DefaultQuery.checkQueryOnPR(DefaultQuery.java:599) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.query.internal.DefaultQuery.execute(DefaultQuery.java:348) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.cache.query.internal.DefaultQuery.execute(DefaultQuery.java:319) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.internal.cache.tier.sockets.BaseCommandQuery.processQueryUsingParams(BaseCommandQuery.java:121) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.internal.cache.tier.sockets.BaseCommandQuery.processQuery(BaseCommandQuery.java:65) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.internal.cache.tier.sockets.command.Query.cmdExecute(Query.java:91) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.internal.cache.tier.sockets.BaseCommand.execute(BaseCommand.java:165) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.internal.cache.tier.sockets.ServerConnection.doNormalMsg(ServerConnection.java:791) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.internal.cache.tier.sockets.ServerConnection.doOneMessage(ServerConnection.java:922) ~[geode-core-1.2.1.jar:?]
        at org.apache.geode.internal.cache.tier.sockets.ServerConnection.run(ServerConnection.java:1180) ~[geode-core-1.2.1.jar:?]
        ...

The application fails to run because we (deliberately) did not create a corresponding, server-side, "Customers" Region.
In order for a client to send data via a client `PROXY` Region (a Region with no local state) to a server in a cluster,
at least one server in the cluster must have a matching Region by name (i.e. "Customers").

Indeed, there are no Regions in the cluster:

**List Regions.**

    gfsh>list regions
    No Regions Found

Of course, you could create the matching server-side, "Customers" Region using *Gfsh*:

    gfsh>create region --name=Customers --type=PARTITION

But, what if you have hundreds of application domain objects each requiring a Region for persistence? It is not an
unusual or unreasonable requirement in any practical enterprise scale application.

While it is not a "convention" in Spring Boot for Apache Geode (SBDG), Spring Data for Apache Geode (SDG) comes to
our rescue. We simply only need to enable cluster configuration from the client:

**Enable Cluster Configuration.**

    @SpringBootApplication
    @EnableClusterAware
    @EnableEntityDefinedRegions(basePackageClasses = Customer.class)    @SpringBootApplication
    public class CustomerServiceApplication {
      // ...
    }


SDG is careful not to stomp on existing Regions since those Regions may have data already. Declaring the
@EnableClusterConfiguration` annotation is a useful development-time feature, but it is recommended that you
explicitly define and declare your Regions in production environments, either using *Gfsh* or Spring confg.

It is now possible to replace the SDG `@EnableClusterConfiguration` annotation with SBDG’s `@EnableClusterAware`
annotation as you see above, which has the same effect of pushing configuration metadata from the
client to the server (or cluster). 

SBDG’s `@EnableClusterAware` annotation makes it unnecessary to
explicitly have to configure parameters like `clientRegionShortcut` on the SDG `@EnableEntityDefinedRegions`
annotation (or similar annotation, e.g. SDG’s `@EnableCachingDefinedRegions`). Finally, because the SBDG's`
@EnableClusterAware` annotation is meta-annotated with SDG’s `@EnableClusterConfiguration annotation` is automatically
configured with the `useHttp` attribute to `true`.

Now, we can run our application again, and this time, it works!

**Client/Server Run Successful.**

      .   ____          _            __ _ _
     /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
    ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
     \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
      '  |____| .__|_| |_|_| |_\__, | / / / /
     =========|_|==============|___/=/_/_/_/
     :: Spring Boot ::        (v2.0.9.RELEASE)

    Saving Customer [Customer(name=Jon Doe)]
    Querying for Customer [SELECT * FROM /Customers WHERE name LIKE '%Doe']
    Customer was [Customer(name=Jon Doe)]

    Process finished with exit code 0

In the cluster (server-side), we will also see that the "Customers" Region was created successfully:

**List & Describe Regions.**

    gfsh>list regions
    List of regions
    ---------------
    Customers


    gfsh>describe region --name=/Customers
    ..........................................................
    Name            : Customers
    Data Policy     : partition
    Hosting Members : ServerOne

    Non-Default Attributes Shared By Hosting Members

     Type  |    Name     | Value
    ------ | ----------- | ---------
    Region | size        | 1
           | data-policy | PARTITION

We see that the "Customers" Region has a size of 1, containing "Jon Doe".

We can verify this by querying the "Customers" Region:

**Query for all Customers.**

    gfsh>query --query="SELECT customer.name FROM /Customers customer"
    Result : true
    Limit  : 100
    Rows   : 1

    Result
    -------
    Jon Doe

That was easy!

## Auto-configuration for Apache Geode, Take Two

What may not be apparent in this example up to this point is how the data got from the client to the server. Certainly,
our client did send `Jon Doe` to the server, but our `Customer` class is not `java.io.Serializable`. So, how was an
instance of `Customer` streamed and sent from the client to the server then (it is using a Socket)?

Any object sent over a network, between two Java processes, or streamed to/from disk, must be serializable.

As further evidence, we can adjust our query slightly:

**Invalid Query.**

    gfsh>query --query="SELECT * FROM /Customers"
    Message : Could not create an instance of a class example.app.crm.model.Customer
    Result  : false

If you tried to perform a `get`, you would hit a similar error:

**Region.get(key).**

    gfsh>get --region=/Customers --key=1 --key-class=java.lang.Long
    Message : Could not create an instance of a class example.app.crm.model.Customer
    Result  : false

So, how was the data sent then? How were we able to access the data stored in the server(s) on the cluster with the
OQL query `SELECT customer.name FROM /Customers customer` as seen above?

Apache Geode and Tanzu GemFire provide 2 proprietary serialization formats in addition to *Java Serialization*:
{apache-geode-docs}/developing/data\_serialization/gemfire\_data\_serialization.html\[Data Serialization\]
and {apache-geode-docs}/developing/data\_serialization/gemfire\_pdx\_serialization.html\[PDX\], or *Portable Data Exchange*.

While *Data Serialization* is more efficient, PDX is more flexible (i.e. "portable"). PDX enables data to be queried
in serialized form and is the format used to support both Java and Native Clients (C++, C\#) simultaneously. Therefore,
PDX is auto-configured in Spring Boot Data Geode (SBDG) by default.

This is convenient since you may not want to implement `java.io.Serializable` for all your application domain model
types that you store in Apache Geode. In other cases, you may not even have control over the types referred to by your
application domain model types to make them `Serializable`, such as when using a 3rd party library.

So, SBDG auto-configures PDX and uses Spring Data Geode’s `MappingPdxSerializer` as the `PdxSerializer` to de/serialize
all application domain model types.

If we were to disable PDX *auto-configuration*, you would see the effects of trying to serialize a non-serializable type,
`Customer`.  This would be apparent when you tried to query /Customers. We will leave this as an exercise for the student. 

If you were not using SBDG, then you would need to enable PDX serialization explicitly.

The Spring Boot Data Geode/Gemfire PDX *auto-configuration* provided by SBDG is equivalent to:

**Equivalent PDX Configuration with Spring Data Gemfire.**

    @SpringBootApplication
    @ClientCacheApplication
    @EnableEntityDefinedRegions(basePackageClasses = Customer.class)
    @EnableClusterConfiguration(useHttp = true)
    @EnablePdx
    public class CustomerServiceApplication {
      // ...
    }

In addition to the `@ClientCacheApplication` annotation, you would need to annotate the `CustomerServiceApplication`
class with SDG’s `@EnablePdx` annotation, which is responsible for configuring PDX serialization and registering
SDG’s `MappingPdxSerializer`.

[*Sample Applications*](https://docs.spring.io/spring-gemfire/docs/current/reference/html/#samples).

