sbt-docker-compose
==================

About
-----
sbt-docker-compose is an sbt plugin that integrates the functionality of [Docker Compose] (https://docs.docker.com/compose/) 
directly into the sbt build environment. This allows you to make code changes and with one sbt command start up a local 
running instance of your latest changes connected to all of its dependencies for live testing and debugging. This plugin 
is designed to be extended to allow for instances to be launched in non-local environments such as AWS and Mesos.

Steps to Enable and Configure sbt-docker-compose
------------------------------------------------

1) Configure your sbt project(s) to build Docker images:
 
  - The [sbt-docker] (https://github.com/marcuslonnberg/sbt-docker) plugin is configured by default.
 
  - The [sbt-native-packager] (https://github.com/sbt/sbt-native-packager) plugin can be used instead by setting:
  
  
    dockerImageCreationPlugin := NativePackager
      
   See the [basic-native-packager] (examples/basic-native-packager) example for more details.

2) Add the sbt-docker-compose plugin to your projects plugins.sbt file:

    addSbtPlugin("com.tapad" % "sbt-docker-compose" % "0.0.1-SNAPSHOT")
    
   sbt-docker-compose is an auto-plugin which requires that sbt version 0.13.5 or higher be used.
   
3) Enable the plugin on the sbt projects you would like to test:

    enablePlugins(DockerComposePlugin)

4) Define a [docker-compose.yml] (https://docs.docker.com/compose/compose-file/) file which describes your component, 
its dependent images and the links between them. This file can be located in one of three places with the precedence order:
   
   - The 'resources' directory of the project as defined by the sbt 'resourceDirectory' setting.
   
   - A 'docker' directory off of the root of the project.
   
   - In the root of the project directory.

5) Optional: There are a number of optional Keys that can be set as well if you want to override the default settings:
   
    composeFile := // Specify the full path to the Compose File to use to create test environment. It defaults to docker-compose.yml in your resources folder.   
    composeServiceName := // Specify the name of the service in the Docker Compose file being tested. This setting prevents the service image from being pull down from the Docker Registry. It defaults to the sbt Project name.
    composeNoBuild := // True if a Docker Compose file is to be started without building any images and only using ones that already exist in the Docker Registry. This defaults to False.
    composeRemoveContainersOnShutdown := // True if a Docker Compose should remove containers when shutting down the compose instance. This defaults to True.
    composeContainerStartTimeoutSeconds := // The amount of time in seconds to wait for the containers in a Docker Compose instance to start. Defaults to 500 seconds.
    composeRemoveTempFileOnShutdown := // True if a Docker Compose should remove the post Custom Tag processed Compose File on shutdown. This defaults to True.
    dockerMachineName =: // If running on OSX the name of the Docker Machine Virtual machine being used. If not overridden it is set to 'default'
    dockerImageCreationPlugin =: // Specifies the sbt plugin being used Docker image creation. This defaults to SbtDocker for the 'sbt-docker' plugin but can also be set to NativePackager for the 'sbt-native-packager' plugin.

There are several sample projects showing how to configure sbt-docker-compose that can be found in the [**examples**] (examples) folder.

To Start a Docker Compose Instance for Testing / Debugging
----------------------------------------------------------
1) To start a new instance from the project with the Plugin enabled run:

    dockerComposeUp
   
   To use locally built images for all services defined in the Docker Compose file instead of pulling from the Docker 
   Registry use the following command:
   
    dockerComposeUp skipPull
    
   By default before starting a Docker Compose instance a new Docker image will be built with your latest code changes.
   If you know you didn't make any code changes and do not want to build a new image use the 'skipBuild' argument:
   
    dockerComposeUp skipBuild
    
   You can start multiple compose instances on the same project as the plugin generates a unique name for each instance.
   
2) To shutdown all instances started from the current project with the Plugin enabled run:

    dockerComposeStop
    
   To shutdown a specific instance regardless of the sbt project it belongs to run:
   
    dockerComposeStop <unique instance id>

3) To display the service connection information for each running Docker Compose instance run:

    dockerComposeInstances
    
Instance information is persisted in a temporary file so that it will be available between restarts of an sbt session.

Docker Compose File Custom Tags
-------------------------------
Custom tags add the ability to pre-process the contents of the docker-compose.yml file and make modifications to it 
before using it to start your instance. There are two custom tags for images that this plugin supports: "\<localBuild\>"
and "\<skipPull\>". Support for additional tags can be added by overriding the "processCustomTags" method.

1) Define "\<skipPull\>" on a set of particular images in the docker-compose file that you want to use a local copy of 
instead of pulling the latest available from the Docker Registry.

    image: yourregistry.com/service:1.0.0<skipPull>
   
2) Define "\<localBuild\>" to launch the locally built version of the image instead of pulling from the public Docker 
Registry. This is how associated images from multi-project builds should be tagged. 
See the [**multi-project**] (examples/multi-project) example.
   
    image: service:latest<localBuild>
    
Instance Connection Information
-------------------------------
After launching an instance with a dockerComposeUp command a table like the one below will be output with the set of endpoints 
that can be used to connect to the instance. The 'Host:Port' column contains the endpoints that are externally exposed.

    +---------+----------------------+-------------+--------------+----------------+--------------+---------+
    | Service | Host:Port            | Tag Version | Image Source | Container Port | Container Id | IsDebug |
    +=========+======================+=============+==============+================+==============+=========+
    | sample1 | 192.168.99.100:32973 | latest      | build        | 5005           | 0a43860a47a8 | YES     |
    | sample2 | 192.168.99.100:32974 | latest      | build        | 5005           | 54803f8a6938 | YES     |
    +---------+----------------------+-------------+--------------+----------------+--------------+---------+

The 'Image Source' column can be one of the following values:

1) **defined**: The image tag is hardcoded in the compose file. For example:

    image: service:latest
    
    image: yourregistry.com/service:1:0:0

2) **build**: The image was built when starting the topology instance or the image is tagged as "\<localBuild\>" and 
being used in a muti-project sbt compose-file. 

    image: service:latest<localBuild>
    
3) **cache**: The locally cached version of the image is being used even if there may be a new version in the Docker 
Registry. This is the result of "\<skipPull\>" being defined on a particular image or being passed to dockerComposeUp.

    image: service:latest<skipPull>
    
Each running instance will also output the commands that can be used to:

1) **Stop the running instance.** For example:
    
    dockerComposeStop 449342

2) **Open a command shell to the container instance:**

    docker exec -it <Container Id> bash

3) **View the standard out logging from the instance:**
 
    docker-compose -p 449342 -f /tmp/compose-updated4937097142223953047.yml logs
    
                                                                                                          
Debugging
---------
To debug a Docker Compose Java process edit your docker-compose.yml file to set the JAVA_TOOL_OPTIONS environment variable.
In the ports section you will also need to expose the port value defined in the "address=" parameter.

    environment:
        JAVA_TOOL_OPTIONS: -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
    ports:
        - "0:5005"

Once the container is started you can to attach to it remotely. The instance connection table will mark 'YES' in the 'IsDebug'
column for any exposed ports that can be attached to with the debugger. 
See the [basic] (examples/basic/docker/docker-compose.yml) example for a project configured with debugging enabled.

Examples
--------
In the [**examples**] (examples) folder there are three different projects showing different uses for the 
sbt-docker-compose plugin.

1) [**basic**] (examples/basic): This project outlines a very basic example of how to enable the plugin on a simple 
application. From sbt run the following to compile the code, build a Docker image, and launch a Docker Compose instance.

    dockerComposeUp
    
2) [**basic-native-packager**] (examples/basic-native-packager): The same as the 'basic' example except that the 
sbt-native-packager is used to build the Docker image instead of sbt-docker.

    dockerComposeUp

3) [**no-build**] (examples/no-build): This project shows how sbt-docker-compose can be used to launch instances of 
images that are already published and do not need to be built locally.
This example uses the official Redis image from Docker Hub. Once the instance is started Redis will be available on the 
displayed "Host:Port". The port is dynamically assigned so that multiple instances can be started.

    dockerComposeUp

4) [**multi-project**] (examples/multi-project): This project shows how more advanced multi-project builds are supported.
From sbt you can build the Docker image and launch a running instance of a single project by executing:

    project sample1
    dockerComposeUp
    
However, from the root "multi-project" you can run the following to build new Docker images for both sub projects and 
launch a running instance that consists of both images:

    project multi-project
    dockerComposeUp
    
Note how the docker-compose.yml file for the root project tags each image with "\<localBuild\>". This allows dockerComposeUp 
to know that these images should not be updated from the Docker Registry.

Other
-----
Testing of sbt-docker-compose has been performed starting with docker-compose version: 1.5.1