package ugcs.processing;

import com.ugcs.ucs.proto.DomainProto.Vehicle;

import java.util.Date;

public interface Flight {
    long getStartEpochMilli();

    long getEndEpochMilli();

    Vehicle getVehicle();

    default Date getStartDate() {
        return new Date(getStartEpochMilli());
    }

    default Date getEndDate() {
        return new Date(getEndEpochMilli());
    }
}
