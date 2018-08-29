package ugcs.upload;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static javax.xml.bind.DatatypeConverter.*;

public class LogBookFileUploader {
    private static final Predicate<String> MD5_HASH_PREDICATE = Pattern.compile("^[a-fA-F0-9]{32}$").asPredicate();

    private final String serverUrl;
    private final String login;
    private final String passwordAsMd5Hash;

    public LogBookFileUploader(String serverUrl, String login, String rawPasswordOrMd5Hash) {
        this.serverUrl = serverUrl;
        this.login = login;
        this.passwordAsMd5Hash = Optional.of(rawPasswordOrMd5Hash)
                .filter(MD5_HASH_PREDICATE)
                .orElseGet(() -> calculateMd5Hash(rawPasswordOrMd5Hash.getBytes()));
    }

    public List<String> uploadFile (File fileToUpload, Charset charset) throws IOException {
        MultipartUtility multipart = new MultipartUtility(serverUrl, charset.displayName());

        multipart.addFormField("login", login);
        multipart.addFormField("password", passwordAsMd5Hash);
        multipart.addFilePart("data", fileToUpload);

        return multipart.finish();
    }

    private static String calculateMd5Hash(byte [] rawData) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(rawData);
            final byte[] digest = md.digest();
            return printHexBinary(digest).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
