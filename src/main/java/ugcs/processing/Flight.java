package ugcs.processing;

import com.ugcs.ucs.proto.DomainProto.Vehicle;
import ugcs.common.identity.Identity;

import java.time.LocalDate;
import java.util.Date;

import static org.apache.commons.lang3.StringUtils.substringBeforeLast;
import static ugcs.time.TimeUtils.time;

/**
 * Interface for {@link Vehicle} flight
 */
public interface Flight {
    long getStartEpochMilli();

    long getEndEpochMilli();

    Vehicle getVehicle();

    Identity<?> getId();

    default Date getStartDate() {
        return new Date(getStartEpochMilli());
    }

    default Date getEndDate() {
        return new Date(getEndEpochMilli());
    }

    default LocalDate getStartLocalDate() {
        return time().toLocalDate(getStartEpochMilli());
    }

    default String getDroneSerialNumber() {
        return getVehicle().getSerialNumber();
    }

    default String getDroneName() {
        return substringBeforeLast(getVehicle().getName(), "-");
    }
}
