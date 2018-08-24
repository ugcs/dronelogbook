import ugcs.upload.MultipartUtility;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This program demonstrates a usage of the {@link MultipartUtility} class.
 * @author www.codejava.net
 *
 */
public class MultipartFileUploader {

	public static void main(String[] args) {
		String charset = "UTF-8";
		File uploadFile1 = new File("test.csv");
		//File uploadFile2 = new File("PIC2.JPG");
		String requestURL = "https://www.dronelogbook.com/webservices/importFlight-ugcs.php";

		try {
			MultipartUtility multipart = new MultipartUtility(requestURL, charset);
			
			// multipart.addHeaderField("login", "login");
			// multipart.addHeaderField("password", "password");
			
			multipart.addFormField("login", "login");
			multipart.addFormField("password", "password");
			
			multipart.addFilePart("data", uploadFile1);
			//multipart.addFilePart("fileUpload", uploadFile2);

			List<String> response = multipart.finish();
			
			System.out.println("SERVER REPLIED:");
			
			for (String line : response) {
				System.out.println(line);
			}
		} catch (IOException ex) {
			System.err.println(ex);
		}
	}
}