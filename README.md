[![Build Status](https://travis-ci.org/ugcs/dronelogbook.svg?branch=master)](https://travis-ci.org/ugcs/dronelogbook)

##### Latest snapshot build as [jar](https://ugcs.github.io/dronelogbook/ucsHub.jar) or [exe](https://ugcs.github.io/dronelogbook/ucsHub.exe)

# ucsHub
This is a tool to import telemetry data from [UgCS](www.ugcs.com) to [DroneLogbook](www.dronelogbook.com) site.

## Getting pre-built release distribution
1. Download the [latest](https://github.com/ugcs/dronelogbook/releases/latest) release distribution as executable jar-file (or go to the [release page](https://github.com/ugcs/dronelogbook/releases) if you want a different one).

2. Run `ucsHub-1.2.jar` (in Linux you may have to add execution permissions on jar-file).

3. All configurations are available through `client.properties` file located in `<user_home>/.dronelogbook` _application data folder_ (e. g. for Windows it is `C:\Users\<user_name>\.dronelogbook`).

> **Note**: To enable telemetry logging for emulators add the following line to the `<path to UgCS installation>\server\ucs\ucs.properties` file:
> ```properties
> ucs.telemetry.store.emulator=true
> ```

## Building application from sources
* Download sources from the repository
* Change to the directory where `pom.xml` is located and run the command (JDK version 8 or higher is required):
```bash
mvnw clean package
```
* after successful build you'll find executable `ucsHub-1.2.jar` application in `distr/multi-platform` directory
* for Windows you may use `ditr/windows-executable/ucsHub.exe`

## Connection with UgCS
Application is developed for [UgCS](www.ugcs.com) version 3.0.

Default localhost admin account is set up at `client.properties` file. Please update it if you use different account:
```properties
server.host=location_of_ugcs_host
server.port=ugcs_server_port
server.login=ugcs_login
server.password=ugcs_password
```


## Uploading telemetry data to DroneLogBook 

Firstly, you will need www.dronelogbook.com account information added to `client.properties` file:

```properties
upload.server.url=https://www.dronelogbook.com/webservices/importFlight-ugcs.php
upload.server.login=dronelogbook_login
upload.server.password=dronelogbook_password_or_md5_hash
```

Uploading process of acquired telemetry starts if you click on `Upload` button.

Acquired telemetry data will be saved as `.csv` file to `<application data folder>\telemetry` directory by default. You can change telemetry storing directory in `client.properties`:
```properties
 telemetry.file.folder=path_to_telemetry_folder
 ```

Successfully uploaded flight's telemetry will be saved to `uploaded` directory by default which is resolved relative to `application data folder` unless it is absolute. For each flight separate file is created. You can change target directory of uploaded files in `client.properties`:
 ```properties
 uploaded.file.folder=path_to_upload_folder
 ```
 
 License
 -------
 
 Licensed under the [3-Clause BSD License](./LICENSE).