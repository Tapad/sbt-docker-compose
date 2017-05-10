sbt-docker-compose
==================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.tapad/sbt-docker-compose/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.tapad/sbt-docker-compose)

About
-----
sbt-docker-compose is an sbt plugin that integrates the functionality of [Docker Compose](https://docs.docker.com/compose/) 
directly into the sbt build environment. This allows you to make code changes and with one sbt command start up a local 
running instance of your latest changes connected to all of its dependencies for live testing and debugging. This plugin 
is designed to be extended to allow for instances to be launched in non-local environments such as AWS and Mesos.

![Alt text](/screenshots/dockerComposeUp.png?raw=true "dockerComposeUp output")

Prerequisites
-------------
You must have [Docker](https://docs.docker.com/engine/installation/) and 
[Docker-Compose](https://docs.docker.com/compose/install/) installed.

Steps to Enable and Configure sbt-docker-compose
------------------------------------------------

1) Add the sbt-docker-compose plugin to your projects plugins.sbt file:
   ```
   addSbtPlugin("com.tapad" % "sbt-docker-compose" % "1.0.22")
   ``` 
   sbt-docker-compose is an auto-plugin which requires that sbt version 0.13.5 or higher be used.
   
2) Enable the plugin on the root project in `build.sbt`:
   ```
   enablePlugins(DockerComposePlugin)
   ``` 

3) Configure your sbt project(s) to build Docker images by setting the 'dockerImageCreationTask':
 
  - The [sbt-docker](https://github.com/marcuslonnberg/sbt-docker) plugin can be used by setting:
    ```
    dockerImageCreationTask := docker.value
    ```
    
  - The [sbt-native-packager](https://github.com/sbt/sbt-native-packager) plugin can be used by setting:
    ```
    dockerImageCreationTask := (publishLocal in Docker).value
    ```
   See the [basic-native-packager](examples/basic-native-packager) example for more details.
   
4) Define a [docker-compose.yml](https://docs.docker.com/compose/compose-file/) file which describes your component, 
its dependent images and the links between them. This path to this file can be explicitly defined or by default the
plugin will attempt to locate it in one of three places with the precedence order:
   
   - The 'resources' directory of the project as defined by the sbt 'resourceDirectory' setting.
   
   - A 'docker' directory off of the root of the project.
   
   - In the root of the project directory.

5) Optional: There are a number of optional Keys that can be set as well if you want to override the default settings:
   ```
    composeFile := // Specify the full path to the Compose File to use to create your test instance. It defaults to docker-compose.yml in your resources folder.   
    composeServiceName := // Specify the name of the service in the Docker Compose file being tested. This setting prevents the service image from being pull down from the Docker Registry. It defaults to the sbt Project name.
    composeNoBuild := // True if a Docker Compose file is to be started without building any images and only using ones that already exist in the Docker Registry. This defaults to False.
    composeRemoveContainersOnShutdown := // True if a Docker Compose should remove containers when shutting down the compose instance. This defaults to True.
    composeRemoveNetworkOnShutdown := // True if a Docker Compose should remove the network it created when shutting down the compose instance. This defaults to True.
    composeContainerStartTimeoutSeconds := // The amount of time in seconds to wait for the containers in a Docker Compose instance to start. Defaults to 500 seconds.
    composeRemoveTempFileOnShutdown := // True if a Docker Compose should remove the post Custom Tag processed Compose File on shutdown. This defaults to True.
    dockerMachineName := // If running on OSX the name of the Docker Machine Virtual machine being used. If not overridden it is set to 'default'
    dockerImageCreationTask := // The sbt task used to create a Docker image. For sbt-docker this should be set to 'docker.value' for the sbt-native-packager this should be set to '(publishLocal in Docker).value'.
    suppressColorFormatting := // True to suppress all color formatting in the output from the plugin. This defaults to the value of the 'sbt.log.noformat' property. If you are using `sbt-extras`, the use of the command line switch `-no-colors` will set this to True.
    testTagsToExecute := // Set of ScalaTest Tags to execute when dockerComposeTest is run. Separate multiple tags by a comma. It defaults to executing all tests.
    testExecutionArgs := // Additional ScalaTest Runner argument options to pass into the test runner. For example, this can be used for the generation of test reports.
    testExecutionExtraConfigTask := // An sbt task that returns a Map[String,String] of variables to pass into the ScalaTest Runner ConfigMap (in addition to standard service/port mappings).
    testDependenciesClasspath := // The path to all managed and unmanaged Test and Compile dependencies. This path needs to include the ScalaTest Jar for the tests to execute. This defaults to all managedClasspath and unmanagedClasspath in the Test and fullClasspath in the Compile Scope.
    testCasesJar := // The path to the Jar file containing the tests to execute. This defaults to the Jar file with the tests from the current sbt project.
    testCasesPackageTask := // The sbt Task to package the test cases used when running 'dockerComposeTest'. This defaults to the 'packageBin' task in the 'Test' Scope.
    variablesForSubstitution := // A Map[String,String] of variables to substitute in your docker-compose file. These are substituted substituted by the plugin and not using environment variables.
    variablesForSubstitutionTask := // An sbt task that returns a Map[String,String] of variables to substitute in your docker-compose file. These are substituted by the plugin and not using environment variables.
   ```
There are several sample projects showing how to configure sbt-docker-compose that can be found in the [**examples**](examples) folder.

To Start a Docker Compose Instance for Testing / Debugging
----------------------------------------------------------
1) To start a new instance from the project with the Plugin enabled run:
   ```
   dockerComposeUp
   ``` 
    
   To use locally built images for all services defined in the Docker Compose file instead of pulling from the Docker 
   Registry use the following command:
   ```
   dockerComposeUp skipPull
   ``` 
    
    
   By default before starting a Docker Compose instance a new Docker image will be built with your latest code changes.
   If you know you didn't make any code changes and do not want to build a new image use the 'skipBuild' argument:
   ```
   dockerComposeUp skipBuild
   ``` 
    
   You can start multiple compose instances on the same project as the plugin generates a unique name for each instance.
   
   When making frequent code changes on your local machine it is often useful to temporarily have the external ports remain the same. 
   Use the '-useStaticPorts' argument to enable this functionality:
   
   ```
    dockerComposeUp -useStaticPorts
    
    E.g. A port mapping of "0:3306" defined in the Compose file would be treated as "3306:3306" if this argument is supplied.
   ``` 
         
2) To shutdown all instances started from the current project with the Plugin enabled run:
   ```
    dockerComposeStop
   ```   
   To shutdown a specific instance regardless of the sbt project it belongs to run:
   ```  
    dockerComposeStop <unique instance id>
   ```
3) To restart a particular instance from the project with the Plugin enabled run:
   ```
    dockerComposeRestart <unique instance id>
   ```  
   You can also supply the 'skipPull', 'skipBuild' or '-useStaticPorts' argument as you would for the 'dockerComposeUp' command:
   ```   
   dockerComposeRestart <unique instance id> [skipPull or skipBuild] [-useStaticPorts]
   ```    
   If there is only one running instance from the current sbt project the Instance Id is not required:
   ```   
    dockerComposeRestart
   ```   
   If there is no running instances from the current sbt project this command will start a new instance from the project.
       
4) To display the service connection information for each running Docker Compose instance run:
   ```
    dockerComposeInstances
   ```   
Instance information is persisted in a temporary file so that it will be available between restarts of an sbt session.

5) You can use tab completion to list out all of the arguments for each command shown above.

To Execute ScalaTest Test Cases Against a Running Instance
----------------------------------------------------------
The sbt-docker-compose plugin provides the ability to run a suite of ScalaTest test cases against a Docker Compose instance.
The dynamically assigned host and port information are passed into each test case via the 
ScalaTest [ConfigMap](http://doc.scalatest.org/2.0/index.html#org.scalatest.ConfigMap).

The key into the map is the "serviceName:containerPort" (e.g. "basic:8080") that is statically defined in the Docker Compose file and it 
will return "host:hostPort" which is the Docker Compose generated and exposed endpoint that can be connected to at runtime
for testing. There is also the key "serviceName:containerId" (e.g. "basic:containerId") which maps to the docker container id.
See the [**basic-with-tests**](examples/basic-with-tests) example for more details.

By default all tests will be executed, however you can also [Tag](http://www.scalatest.org/user_guide/tagging_your_tests)
test cases and indicate to the plugin to only execute those tests:
   ``` 
    testTagsToExecute := "DockerComposeTag"
   ```
1) To start a new DockerCompose instance, run your tests and then shut it down run:
   ```
    dockerComposeTest
   ```    
2) To run your test cases against an already running instance execute:
   ```
    dockerComposeTest <instance id>
   ```    
3) To override the sbt setting 'testTagsToExecute' when starting a test pass provide a comma separated list of tags to
the "-tags" argument:
   ```
    dockerComposeTest -tags:<tag1,tag2>
   ```    
4) To attach a debugger during test case execution provide a port number to the "-debug" argument. This will suspend the
tests from running until you attach a debugger to the specified port. For example:
   ```
    dockerComposeTest <instance id> -debug:<debug port>
   ```    
**Note:** The test pass is started using the using the 'java' process that exists on your command line PATH to launch the
[ScalaTest Test Runner](http://www.scalatest.org/user_guide/using_the_runner). For this to work the classpath of your
project needs to be built with the version of scala used by the project. If this is not configured correctly you may see
an issue with the Test Runner failing to load classes.


Docker Compose File Custom Tags
-------------------------------
Custom tags are a feature of the sbt-docker-compose plugin that add the ability to pre-process the contents of the 
docker-compose.yml file and make modifications to it before using it to start your instance. There are two custom tags 
for images that this plugin supports: "\<localBuild\>" and "\<skipPull\>". Support for additional tags can be added by 
overriding the "processCustomTags" method.

1) Define "\<skipPull\>" on a set of particular images in the docker-compose file that you want to use a local copy of 
instead of pulling the latest available from the Docker Registry.
   ```
    image: yourregistry.com/service:1.0.0<skipPull>
   ```   
2) Define "\<localBuild\>" to launch the locally built version of the image instead of pulling from the public Docker 
Registry. This is how associated images from multi-project builds should be tagged. 
See the [**multi-project**](examples/multi-project) example.
   ```   
    image: service:latest<localBuild>
   ```    
Instance Connection Information
-------------------------------
After launching an instance with a dockerComposeUp command a table like the one below will be output with the set of endpoints 
that can be used to connect to the instance. The 'Host:Port' column contains the endpoints that are externally exposed.
   ```
    +---------+----------------------+-------------+--------------+----------------+--------------+---------+
    | Service | Host:Port            | Tag Version | Image Source | Container Port | Container Id | IsDebug |
    +=========+======================+=============+==============+================+==============+=========+
    | sample1 | 192.168.99.100:32973 | latest      | build        | 5005           | 0a43860a47a8 | YES     |
    | sample2 | 192.168.99.100:32974 | latest      | build        | 5005           | 54803f8a6938 | YES     |
    +---------+----------------------+-------------+--------------+----------------+--------------+---------+
   ```
The 'Image Source' column can be one of the following values:

1) **defined**: The image tag is hardcoded in the compose file. For example:
   ```
    image: service:latest
    
    image: yourregistry.com/service:1:0:0
   ```
2) **build**: The image was built when starting the topology instance or the image is tagged as "\<localBuild\>" and 
being used in a muti-project sbt compose-file. 
   ```
    image: service:latest<localBuild>
   ```    
3) **cache**: The locally cached version of the image is being used even if there may be a new version in the Docker 
Registry. This is the result of "\<skipPull\>" being defined on a particular image or being passed to dockerComposeUp.
   ```
    image: service:latest<skipPull>
   ```   
Each running instance will also output the commands that can be used to:

1) **Stop the running instance.** For example:
   ```    
    dockerComposeStop 449342
   ```
2) **Open a command shell to the container instance:**
   ```
    docker exec -it <Container Id> bash
   ```
3) **View the standard out logging from the instance:**
   ``` 
    docker-compose -p 449342 -f /tmp/compose-updated4937097142223953047.yml logs
   ```
4) **Execute test cases against the running instance:**
   ``` 
    dockerComposeTest <Instance Id>
   ```   
                                                                                                          
Debugging
---------
To debug a Docker Compose Java process edit your docker-compose.yml file to set the JAVA_TOOL_OPTIONS environment variable.
In the ports section you will also need to expose the port value defined in the "address=" parameter.
   ```
    environment:
        JAVA_TOOL_OPTIONS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
    ports:
        - "0:5005"
   ```
Once the container is started you can to attach to it remotely. The instance connection table will mark 'YES' in the 'IsDebug'
column for any exposed ports that can be attached to with the debugger. 
See the [basic](examples/basic-with-tests/docker/docker-compose.yml) example for a project configured with debugging enabled.

See the test case execution section above for information on how to attach a debugger to running test cases.

Examples
--------
In the [**examples**](examples) folder there are six different projects showing different uses for the 
sbt-docker-compose plugin.

1) [**basic-with-tests**](examples/basic-with-tests): This project outlines a very basic example of how to enable the
plugin on a simple application that will echo back "Hello, World!". The examples also shows how to create a ScalaTest 
test case that can run against the dynamically assigned endpoints. From sbt run the following to compile the code, 
build a Docker image and launch a Docker Compose instance.
   ```
    dockerComposeUp
   ```   
Run the following to execute a test case against the running instance:
   ```
    dockerComposeTest <Instance Id>
   ```
Run the following to start a new instance, run tests and shutdown the instance:
   ```
    dockerComposeTest
   ```
Note how this example project shows how the testExecutionArgs setting can be used to create an html test pass report by
by providing additional ScalaTest Runner defined [arguments](http://www.scalatest.org/user_guide/using_the_runner).
   ```
    //Specify that an html report should be created for the test pass
    testExecutionArgs := "-h target/htmldir"
   ```
2) [**basic-native-packager**](examples/basic-native-packager): This project outlines a very basic example of how to
enable the plugin on a simple application. From sbt run the following to compile the code, build a Docker image and 
launch a Docker Compose instance. In this example the sbt-native-packager is used to build the Docker image instead of 
sbt-docker.
   ```
    dockerComposeUp
   ```
3) [**no-build**](examples/no-build): This project shows how sbt-docker-compose can be used to launch instances of 
images that are already published and do not need to be built locally. This example uses the official Redis image 
from Docker Hub. Once the instance is started Redis will be available on the displayed "Host:Port". The port is
dynamically assigned so that multiple instances can be started.
   ```
    dockerComposeUp
   ```
4) [**multi-project**](examples/multi-project): This project shows how more advanced multi-project builds are supported.
From sbt you can build the Docker image and launch a running instance of a single project by executing:
   ```
    project sample1
    dockerComposeUp
   ```    
However, from the root "multi-project" you can run the following to build new Docker images for both sub projects and 
launch a running instance that consists of both images:
   ```
    project multi-project
    dockerComposeUp
   ```   
Note how the docker-compose.yml file for the root project tags each image with "\<localBuild\>". This allows dockerComposeUp 
to know that these images should not be updated from the Docker Registry.

5) [**basic-variable-substitution**](examples/basic-variable-substitution): This project demonstrates how you can re-use your 
existing docker-compose.yml with [variable substitution](https://docs.docker.com/compose/compose-file/#variable-substitution) 
using sbt-docker-compose.  Instead of passing your variables as environment variables you can define them in your build.sbt 
programmatically.

build.sbt:
   ```
    variablesForSubstitution := Map("SOURCE_PORT" -> "5555")
    
    or
    
    variablesForSubstitutionTask := { /* code */ Map("SOURCE_PORT" -> "5555") }
   ```    
docker-compose.yml:
   ```
    basic:
      image: basic:1.0.0
      environment:
        JAVA_TOOL_OPTIONS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
      ports:
        - "${SOURCE_PORT}:5005"
   ```
6) [**basic-with-tests-integration**](examples/basic-with-tests-integration): This project shows how to change the default sbt Scope of the
tests being executed from 'Test' to 'IntegrationTest' when 'dockerComposeTest' is run.

    
Currently Unsupported Docker Compose Fields
-------------------------------------------
1) "build:" - All docker compose services need to specify an "image:" field.

2) "container_name:" - To allow for multi-instance support container names need to be dynamically provided by the plugin
 instead of being explicitly defined.

3) "extends:" - All docker services must be defined in a single docker compose yml file.

Other
-----
Testing of sbt-docker-compose has been performed starting with docker-compose version: 1.5.1
