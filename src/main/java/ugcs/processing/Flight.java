package ugcs.processing;

import java.util.Date;

public interface Flight {
    long getStartEpochMilli();

    long getEndEpochMilli();

    default Date getStartDate() {
        return new Date(getStartEpochMilli());
    }

    default Date getEndDate() {
        return new Date(getEndEpochMilli());
    }
}
