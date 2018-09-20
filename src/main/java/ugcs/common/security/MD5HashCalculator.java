package ugcs.common.security;

import ugcs.common.LazyFieldEvaluator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static javax.xml.bind.DatatypeConverter.printHexBinary;

public class MD5HashCalculator extends LazyFieldEvaluator {
    private static final Predicate<String> MD5_HASH_PREDICATE = Pattern.compile("^[a-fA-F0-9]{32}$").asPredicate();

    private final String rawPasswordOrMd5Hash;

    public static MD5HashCalculator of(String rawPasswordOrMd5Hash) {
        return new MD5HashCalculator(rawPasswordOrMd5Hash);
    }

    public String hash() {
        return evaluateField("md5Hash",
                () -> Optional.of(rawPasswordOrMd5Hash)
                        .filter(MD5_HASH_PREDICATE)
                        .orElseGet(() -> calculateMd5Hash(rawPasswordOrMd5Hash.getBytes()))
        );
    }

    private MD5HashCalculator(String rawPasswordOrMd5Hash) {
        this.rawPasswordOrMd5Hash = rawPasswordOrMd5Hash;
    }

    private static String calculateMd5Hash(byte[] rawData) {
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
