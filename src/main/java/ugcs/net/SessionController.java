package ugcs.net;

import com.ugcs.ucs.client.Client;
import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.DomainObjectWrapper;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import com.ugcs.ucs.proto.MessagesProto;
import ugcs.exceptions.ExpectedException;

import java.io.IOException;
import java.net.InetSocketAddress;
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
            client.connect();
            session = new ClientSessionEx(client);
            session.authorizeHci();
            session.login(login, new String(password));
        } catch (IOException connectException) {
            throw new ExpectedException("UgCS not available.", connectException);
        } catch (Exception ugcsException) {
            throw new ExpectedException("UgCS: " + ugcsException.getMessage(), ugcsException);
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
                            .setClientId(getClientId())
                            .setLimit(0)
                            .build();

            return client.execute(getTelemetryRequest);
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }

    public MessagesProto.GetVehicleLogByTimeRangeResponse getVehicleLog(Vehicle vehicle, long startTimeEpochMilli, long endTimeEpochMilli) {
        try {
            final MessagesProto.GetVehicleLogByTimeRangeRequest getVehicleLogByTimeRangeRequest =
                    MessagesProto.GetVehicleLogByTimeRangeRequest.newBuilder()
                            .setFromTime(startTimeEpochMilli)
                            .setToTime(endTimeEpochMilli)
                            .setClientId(getClientId())
                            .setLevel(DomainProto.SeverityLevel.SL_DEBUG)
                            .addVehicles(vehicle)
                            .build();

            return client.execute(getVehicleLogByTimeRangeRequest);
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }

    public long countTelemetry(Vehicle vehicle, long startTimeEpochMilli, long endTimeEpochMilli) {
        try {
            final MessagesProto.CountTelemetryRequest countTelemetryRequest =
                    MessagesProto.CountTelemetryRequest.newBuilder()
                            .setClientId(getClientId())
                            .setVehicle(vehicle)
                            .setFromTime(startTimeEpochMilli)
                            .setToTime(endTimeEpochMilli)
                            .build();

            final MessagesProto.CountTelemetryResponse countTelemetryResponse = client.execute(countTelemetryRequest);
            return countTelemetryResponse.getCount();
        } catch (Exception toRethrow) {
            throw new RuntimeException(toRethrow);
        }
    }

    public MessagesProto.TraceTelemetryFramesResponse traceTelemetryFrames(Vehicle vehicle, long originTimeEpochMilli, double intervalSec, int number) {
        try {
            final MessagesProto.TraceTelemetryFramesRequest traceTelemetryFramesRequest =
                    MessagesProto.TraceTelemetryFramesRequest.newBuilder()
                            .setClientId(getClientId())
                            .setVehicle(vehicle)
                            .setInterval(intervalSec)
                            .setOriginTime(originTimeEpochMilli)
                            .setNumber(number)
                            .build();

            return client.execute(traceTelemetryFramesRequest);
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

    private int getClientId() {
        return session.getClientId();
    }
}
