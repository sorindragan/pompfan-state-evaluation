# Automated Negotiation League (ANL)
Join our [Discord server](https://discord.gg/qvXK3DJTuz)!

This repository contains an example agent for the Automated Neagotiation League (ANL) 2021. A full description of the competition can be found [here](http://web.tuat.ac.jp/~katfuji/ANAC2021/genius.html).


## Agent development
There are currently three methods of developing an agent for the competition:
- Using plain Java.
    1. Make sure you have JDK 8/11 and Maven <=3.6.3 installed.
    2. Clone this repository.
    3. Run `mvn package` in the root directory.
- Two pure Docker based method that only relies on Docker. The first does not provide any Linting, Intellisence, Formatting, etc., the second does.
    - Develop your agent directly on your machine.
        1. Make sure you have [Docker installed](https://docs.docker.com/get-docker/).
        2. Clone this repository.
        3. Run "docker_compile.ps1" on Windows or "docker_compile.sh" on Linux.
    - Develop your agent inside a Docker container (**Recommended method**).
        1. Make sure you have [Docker installed](https://docs.docker.com/get-docker/).
        2. Use VSCode as your IDE ([Install if necessary](https://code.visualstudio.com)).
        3. Clone and open this repository in VSCode.
        4. Install the "Remote Development" and "Java Extension Pack" package in the marketplace of VSCode.
        5. Ctrl-Shift-P & "Remote-Containers: Reopen in Container".

The first time compiling the agent will take some time as all the dependencies need to be downloaded. The compile agent is now in the "target" directory ("\<name>-\<version>-jar-with-dependencies.jar"). **If you use this agent as a basis of your agent, make sure to change the package and class names in both the Java files and the pom.xml file. (Search for the #TODO tags in the repository)**

## Agent testing
There are also two methods of testing an agent:
- Using default [GeniusWeb](https://tracinsy.ewi.tudelft.nl/pubtrac/GeniusWeb) as described in the documentation. 
- A Docker image based on the [simplerunner](https://tracinsy.ewi.tudelft.nl/pubtrac/GeniusWeb#Stand-aloneRunning) that allows for much easier testing and analysing. **I would recommend to use this method**. 

### [Default GeniusWeb](https://tracinsy.ewi.tudelft.nl/pubtrac/GeniusWeb)
How to use default [GeniusWeb](https://tracinsy.ewi.tudelft.nl/pubtrac/GeniusWeb) is described in its documentation. It relies on Java 8, Tomcat and some manual installation work. Running negotiation sessions and tournaments is GUI based and can be a bit cumbersome if you plan to run many test setups. The results obtained through this method do not contain details on utility for the involved agents.

### [Docker GeniusWeb](https://github.com/brenting/ANL-2021-docker-runner) **Recommended**
To use this method:
1. Make sure that you have [Docker installed](https://docs.docker.com/get-docker/), this is the only requirement.
2. Clone the [ANL docker runner repo](https://github.com/brenting/ANL-2021-docker-runner).
3. Change the "settings.yaml" file to your liking (see also instructions in the repo).
4. Run "run.ps1" on Windows or "run.sh" on Linux.

This Docker image handles many operations under the hood to make this competition more easy accessible. A few key improvements:
- You can create your own tournament/testing sequence in an intuitive yaml-file.
- It adds agent utilities to the results file for better analysis.

If you have any suggestions or improvements for this Docker image, let me know on our [Discord server](https://discord.gg/qvXK3DJTuz).
