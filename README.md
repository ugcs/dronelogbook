# ucsHub
This is a tool to import telemetry data from [UgCS](www.ugcs.com) to [DroneLogbook](www.dronelogbook.com) site.

## Getting pre-built release distribution
1. Download the [latest](https://github.com/ugcs/dronelogbook/releases/download/v1.0/ucsHub-1.0.zip) release distribution as zip-archive (or go to the [release page](https://github.com/ugcs/dronelogbook/releases) if you want a different one).

2. Unpack downloaded archive to any place in your local filesystem.

3. Run `ucsHub-1.0.jar` in the result folder (in Linux you may have to add execution permissions on jar-file).

4. All configurations are available through `client.properties` file located in the distribution folder.

> **Note**: To enable telemetry logging for emulators add the following line to the `<path to UgCS installation>\server\ucs\ucs.properties` file:
> ```properties
> ucs.telemetry.store.emulator=true
> ```

## Building application from sources
* Install [Maven](https://maven.apache.org/) tool (version 3.3.9 or higher)
* Change to the directory where `pom.xml` is located and run the command:
```bash
mvn clean package
```
* after successful build you'll find executable `ucsHub-1.0.jar` application and `client.properties` configuration file in `distr` directory


## Connection with UGCS
ucsHub is developed for [UgCS](www.ugcs.com) version 3.0.

Default localhost ugcs admin account is set up at `client.properties` file. Please update it if you use different account:
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

Uploading process of acquired telemetry starts if you check `Upload flights` checkbox and click on `Get telemetry` button.

Successfully uploaded file will be moved to `uploaded` directory by default. You can change target directory of uploaded files in `client.properties`:
 ```properties
 uploaded.file.folder=path_to_fodler
 ```
 
 License
 -------
 
 Licensed under the [3-Clause BSD License](./LICENSE).