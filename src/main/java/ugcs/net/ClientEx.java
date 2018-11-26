package ugcs.net;

import com.google.protobuf.Message;
import com.ugcs.ucs.client.Client;
import ugcs.exceptions.ugcs.UgcsFailure;

import java.net.SocketAddress;

/**
 * {@link Client} with given timeout for message interaction
 */
public class ClientEx extends Client {
    private static final long DEFAULT_REQUEST_TIMEOUT_MS = 120_000L;

    private final long timeout;

    ClientEx(SocketAddress serverAddress) {
        this(serverAddress, DEFAULT_REQUEST_TIMEOUT_MS);
    }

    private ClientEx(SocketAddress serverAddress, long requestTimeout) {
        super(serverAddress);

        timeout = requestTimeout;
    }

    @Override
    public <T> T execute(Message message) {
        try {
            return super.execute(message, timeout);
        } catch (Exception e) {
            throw new UgcsFailure(e);
        }
    }
}
