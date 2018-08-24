# ucsHub
This is a tool to import telemetry data from [UgCS](www.ugcs.com) to [DroneLogbook](www.dronelogbook.com) site.

## Building client application
* Install [Maven](https://maven.apache.org/) tool (version 3.3.9 or higher)
* Change to the directory where `pom.xml` is located and run the command:
```bash
mvn clean package
```
* after successful build you'll find `ugcsClient.jar` executable application in `target` directory


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
upload.server.password=dronelogbook_password
```

Uploading process of acquired telemetry starts if you check `Upload flight` checkbox and click on `Get telemetry` button.

Successfully uploaded file will be moved to `uploaded` directory by default. You can change target directory of uploaded files in `client.properties`:
 ```properties
 uploaded.file.folder=path_to_fodler
 ```