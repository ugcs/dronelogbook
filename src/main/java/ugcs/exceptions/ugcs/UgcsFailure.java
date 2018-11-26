package ugcs.exceptions.ugcs;

import org.apache.commons.lang3.StringUtils;
import ugcs.exceptions.ExpectedException;

import static java.lang.String.format;

public class UgcsFailure extends ExpectedException {
    static String INCORRECT_CREDENTIALS_MESSAGE = "Incorrect UgCS login credentials. Please try again.";

    public UgcsFailure(Throwable cause) {
        super(getMessageFromCause(cause), cause);
    }

    private static String getMessageFromCause(Throwable cause) {
        final String causeMessageInLowerCase = cause.getMessage().toLowerCase();

        if (causeMessageInLowerCase.contains("authentication")) {
            return INCORRECT_CREDENTIALS_MESSAGE;
        }

        if (cause instanceof UgcsFailure) {
            return cause.getMessage();
        }

        if (StringUtils.isEmpty(cause.getMessage())) {
            return format("UgCS failure - \"%s\"", cause.getClass().getName());
        }

        return format("UgCS failure - \"%s\"", cause.getMessage());
    }
}