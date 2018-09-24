package ugcs.exceptions.ugcs;

import ugcs.exceptions.ExpectedException;

public class UgcsDisconnectedException extends ExpectedException {

    public UgcsDisconnectedException(Throwable cause) {
        super("UgCS not available. Check if server is running.", cause);
    }
}
