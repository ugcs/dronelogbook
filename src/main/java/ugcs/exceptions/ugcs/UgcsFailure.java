package ugcs.exceptions.ugcs;

import ugcs.exceptions.ExpectedException;

import static java.lang.String.format;

public class UgcsFailure extends ExpectedException {

    public UgcsFailure(Throwable cause) {
        super(format("UgCS failure - \"%s\"", cause.getMessage()), cause);
    }
}