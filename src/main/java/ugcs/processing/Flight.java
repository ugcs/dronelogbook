package ugcs.processing;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.common.identity.Identity;

import java.util.Date;

/**
 * Interface for {@link Vehicle} flight
 */
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

    Identity<?> getId();
}
