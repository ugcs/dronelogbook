package ugcs.ucsHub;

import com.ugcs.ucs.client.Client;
import com.ugcs.ucs.proto.DomainProto.DomainObjectWrapper;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import com.ugcs.ucs.proto.MessagesProto;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SessionController implements AutoCloseable {
    private final String host;
    private final int port;


    private final String login;
    private final char[] password;

    private Client client;
    private ClientSessionEx session;

    public SessionController(String host, int port, String login, char[] password) {
        this.host = host;
        this.port = port;
        this.login = login;
        this.password = password;
    }

    public void connect() {
        final InetSocketAddress serverAddress = new InetSocketAddress(host, port);

        try {
            client = new Client(serverAddress);

            // Register a server telemetry event listener.
            // This method just adds listeners to the client listeners
            // collection and can be invoked any time during the client
            // life-cycle (even after re-connections).
            // This listener will stay quiet until event subscription will
            // be registered on the-server side.
            //client.addNotificationListener(new TelemetryListener());
            client.connect();

            session = new ClientSessionEx(client);

            // To create a new client session application should send
            // an AuthorizeHciRequest message and set the clientId field
            // value to -1. In this case server will create a new session
            // object and return its identity in the clientId field.
            session.authorizeHci();

            // Any session should be associated with an authenticated user.
            // To authenticate user provide its credentials via LoginRequest
            // message.
            session.login(login, new String(password));
            Arrays.fill(password, 'x');
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }

    public List<Vehicle> getVehicles() {
        try {
            return session.getObjectList(Vehicle.class).stream()
                    .map(DomainObjectWrapper::getVehicle)
                    .collect(Collectors.toList());
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }

    public MessagesProto.GetTelemetryResponse getTelemetry(Vehicle vehicle, long startTimeEpochMilli, long endTimeEpochMilli) {
        try {
            final MessagesProto.GetTelemetryRequest getTelemetryRequest =
                    MessagesProto.GetTelemetryRequest.newBuilder()
                            .setFromTime(startTimeEpochMilli)
                            .setToTime(endTimeEpochMilli)
                            .setVehicle(vehicle)
                            .setClientId(session.getClientId())
                            .setLimit(0)
                            .build();

            return client.execute(getTelemetryRequest);
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }

    @Override
    public void close() {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }
}
