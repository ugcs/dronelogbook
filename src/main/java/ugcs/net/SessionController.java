package ugcs.net;

import com.google.protobuf.Message;
import com.ugcs.ucs.client.Client;
import com.ugcs.ucs.proto.DomainProto;
import com.ugcs.ucs.proto.DomainProto.DomainObjectWrapper;
import com.ugcs.ucs.proto.DomainProto.Vehicle;
import com.ugcs.ucs.proto.MessagesProto;
import lombok.SneakyThrows;
import ugcs.exceptions.ugcs.UgcsDisconnectedException;
import ugcs.exceptions.ugcs.UgcsFailure;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exports service gateway for interactions with {@link Client}
 */
public class SessionController implements AutoCloseable {
    private String host;
    private int port;

    private String login;
    private String password;

    private Client client;
    private ClientSessionEx session;

    private static volatile SessionController instance;

    public static SessionController sessionController() {
        if (instance == null) {
            synchronized (SessionController.class) {
                if (instance == null) {
                    instance = new SessionController();
                }
            }
        }
        return instance;
    }

    private SessionController() {
    }

    public void updateSettings(SessionSettings settings) {
        this.host = settings.getHost();
        this.port = settings.getPort();
        this.login = settings.getUcsServerLogin();
        this.password = settings.getUcsServerPassword();
    }

    public void connect() {
        final InetSocketAddress serverAddress = new InetSocketAddress(host, port);

        client = new ClientEx(serverAddress);
        refreshSession();
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

        return execute(getTelemetryRequest);
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

        return execute(getVehicleLogByTimeRangeRequest);
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

        final MessagesProto.CountTelemetryResponse countTelemetryResponse = execute(countTelemetryRequest);
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

        return execute(traceTelemetryFramesRequest);
    }

    @SneakyThrows
    public MessagesProto.GetVehicleTracksResponse getVehicleTracks(List<Vehicle> vehicles, long fromTimeEpochMilli, long toTimeEpochMilli, int limit) {
        final MessagesProto.GetVehicleTracksRequest.Builder getVehicleTracksRequestBuilder =
                MessagesProto.GetVehicleTracksRequest.newBuilder()
                        .setClientId(getClientId())
                        .setFrom(fromTimeEpochMilli)
                        .setTo(toTimeEpochMilli)
                        .setLimit(limit);
        vehicles.forEach(getVehicleTracksRequestBuilder::addVehicles);

        return execute(getVehicleTracksRequestBuilder.build());
    }

    private <T> T execute(Message message) throws Exception {
        reconnectIfConnectionLost();

        return client.execute(message);
    }

    private void reconnectIfConnectionLost() {
        if (!client.isConnected()) {
            refreshSession();
        }
    }

    private void refreshSession() {
        try {
            client.connect();
            session = new ClientSessionEx(client);
            session.authorizeHci();
            session.login(login, password);
        } catch (IOException connectException) {
            throw new UgcsDisconnectedException(connectException);
        } catch (Exception ugcsException) {
            throw new UgcsFailure(ugcsException);
        }
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
