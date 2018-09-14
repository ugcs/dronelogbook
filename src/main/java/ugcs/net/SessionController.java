package ugcs.net;

import com.ugcs.ucs.client.Client;
import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.DomainObjectWrapper;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import com.ugcs.ucs.proto.MessagesProto;
import lombok.SneakyThrows;
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

    @SneakyThrows
    public List<Vehicle> getVehicles() {
        return session.getObjectList(Vehicle.class).stream()
                .map(DomainObjectWrapper::getVehicle)
                .collect(Collectors.toList());
    }

    @SneakyThrows
    public MessagesProto.GetTelemetryResponse getTelemetry(Vehicle vehicle, long startTimeEpochMilli, long endTimeEpochMilli) {
        final MessagesProto.GetTelemetryRequest getTelemetryRequest =
                MessagesProto.GetTelemetryRequest.newBuilder()
                        .setFromTime(startTimeEpochMilli)
                        .setToTime(endTimeEpochMilli)
                        .setVehicle(vehicle)
                        .setClientId(getClientId())
                        .setLimit(0)
                        .build();

        return client.execute(getTelemetryRequest);
    }

    @SneakyThrows
    public MessagesProto.GetVehicleLogByTimeRangeResponse getVehicleLog(Vehicle vehicle, long startTimeEpochMilli, long endTimeEpochMilli) {
        final MessagesProto.GetVehicleLogByTimeRangeRequest getVehicleLogByTimeRangeRequest =
                MessagesProto.GetVehicleLogByTimeRangeRequest.newBuilder()
                        .setFromTime(startTimeEpochMilli)
                        .setToTime(endTimeEpochMilli)
                        .setClientId(getClientId())
                        .setLevel(DomainProto.SeverityLevel.SL_DEBUG)
                        .addVehicles(vehicle)
                        .build();

        return client.execute(getVehicleLogByTimeRangeRequest);
    }

    @SneakyThrows
    public long countTelemetry(Vehicle vehicle, long startTimeEpochMilli, long endTimeEpochMilli) {
        final MessagesProto.CountTelemetryRequest countTelemetryRequest =
                MessagesProto.CountTelemetryRequest.newBuilder()
                        .setClientId(getClientId())
                        .setVehicle(vehicle)
                        .setFromTime(startTimeEpochMilli)
                        .setToTime(endTimeEpochMilli)
                        .build();

        final MessagesProto.CountTelemetryResponse countTelemetryResponse = client.execute(countTelemetryRequest);
        return countTelemetryResponse.getCount();
    }

    @SneakyThrows
    public MessagesProto.TraceTelemetryFramesResponse traceTelemetryFrames(Vehicle vehicle, long originTimeEpochMilli, double intervalSec, int number) {
        final MessagesProto.TraceTelemetryFramesRequest traceTelemetryFramesRequest =
                MessagesProto.TraceTelemetryFramesRequest.newBuilder()
                        .setClientId(getClientId())
                        .setVehicle(vehicle)
                        .setInterval(intervalSec)
                        .setOriginTime(originTimeEpochMilli)
                        .setNumber(number)
                        .build();

        return client.execute(traceTelemetryFramesRequest);
    }

    @Override
    @SneakyThrows
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    private int getClientId() {
        return session.getClientId();
    }
}
