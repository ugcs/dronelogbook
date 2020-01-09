[![Build Status](https://travis-ci.org/ugcs/dronelogbook.svg?branch=master)](https://travis-ci.org/ugcs/dronelogbook)

##### Latest snapshot build as [jar](https://ugcs.github.io/dronelogbook/telemetrySyncTool.jar), [exe](https://ugcs.github.io/dronelogbook/telemetrySyncTool.exe) or [app for mac](https://ugcs.github.io/dronelogbook/telemetrySyncTool.tar.gz) 

# Telemetry Sync Tool
This is a tool to import telemetry data from [UgCS](www.ugcs.com) to [DroneLogbook](www.dronelogbook.com) site.

## Getting pre-built release distribution
1. Download the [latest](https://github.com/ugcs/dronelogbook/releases/latest) release distribution as executable jar-file (or go to the [release page](https://github.com/ugcs/dronelogbook/releases) if you want a different one).

2. Run `telemetrySyncTool-1.3.2.jar` (in Linux you may have to add execution permissions on jar-file).

3. All configurations are available through `client.properties` file located in `<user_home>/.dronelogbook` _application data folder_ (e. g. `~/.dronelogbook` for Linux/Mac and `C:\Users\<user_name>\.dronelogbook` for Windows).

> **Note**: To enable telemetry logging for emulators add the following line to the `<path to UgCS installation>\server\ucs\ucs.properties` file:
> ```properties
> ucs.telemetry.store.emulator=true
> ```

## Building application from sources
* Download sources from the repository
* Change to the directory where `pom.xml` is located and run the command ([JDK version 8](https://jdk.java.net/8/) or higher is required):
```bash
# Linux/Mac:
./mvnw clean package
```
```cmd
# Windows:
mvnw clean package
```

>**Note**: For Maven to work you'll need to set up `JAVA_HOME` environment variable:
```bash
# Linux/Mac:
echo "export JAVA_HOME=/path/to/jdk/root/directory" >> ~/.bash_profile
```
```cmd
# Windows:
setx -m JAVA_HOME "C:\path\to\jdk\root\directory"
```
 
* after successful build you'll find executable `telemetrySyncTool-1.3.2.jar` application in `distr/multi-platform` directory
* for Windows exe-file is built in `ditr/windows-executable` directory
* for Mac the distribution can be found in `distr/mac-application` directory

## Connection with UgCS
Application is compatible with [UgCS](www.ugcs.com) version 3.0 or higher.

Default localhost admin account is set up at `client.properties`. Please update it if you use different account:
```properties
server.host=location_of_ugcs_host
server.port=ugcs_server_port
server.login=ugcs_login
server.password=ugcs_password
```


## Uploading telemetry data to DroneLogbook 

Uploading process of acquired telemetry starts if you select the drone, choose the filter in the bottom of the application, select flights and click on `Upload` button.
 
 License
 -------
 
 Licensed under the [3-Clause BSD License](./LICENSE).