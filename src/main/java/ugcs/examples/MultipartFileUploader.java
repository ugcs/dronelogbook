package ugcs.examples;

import ugcs.upload.logbook.DroneLogbookResponse;
import ugcs.upload.logbook.MultipartUtility;

import java.io.File;

/**
 * Example for uploading data to DroneLogbook web service
 */
public class MultipartFileUploader {

    public static void main(String[] args) {
        String charset = "UTF-8";
        File uploadFile1 = new File("test.csv");
        String requestURL = "https://www.dronelogbook.com/webservices/importFlight-ugcs.php";

        MultipartUtility multipart = new MultipartUtility(requestURL, charset);

        multipart.addFormField("login", "login");
        multipart.addFormField("password", "password");

        multipart.addFilePart("data", uploadFile1);

        DroneLogbookResponse response = multipart.performRequest();

        System.out.println("SERVER REPLIED:");
        System.out.println(response);
    }
}