package ugcs.net;

import com.google.protobuf.Message;
import com.ugcs.ucs.client.Client;

import java.net.SocketAddress;

public class ClientEx extends Client {
    private static final long DEFAULT_REQUEST_TIMEOUT_MS = 120_0000L;

    private final long timeout;

    ClientEx(SocketAddress serverAddress) {
        this(serverAddress, DEFAULT_REQUEST_TIMEOUT_MS);
    }

    private ClientEx(SocketAddress serverAddress, long requestTimeout) {
        super(serverAddress);

        timeout = requestTimeout;
    }

    @Override
    public <T> T execute(Message message) throws Exception {
        return super.execute(message, timeout);
    }
}
