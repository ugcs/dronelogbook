package ugcs.upload.logbook;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.list;
import static ugcs.upload.logbook.DroneLogbookResponse.fromList;

class DroneLogbookResponseTest {
    @Test
    void testSuccessResponse() {
        final ArrayList<String> droneLogbookSuccessResponseStub =
                list("  { " +
                        "\"status\": 0, " +
                        "\"description\": \"Test description.\", " +
                        "\"url\": \"http:\\\\\\\\host:port?some_var=some_val%12\"" +
                        "}   ");

        final DroneLogbookResponse droneLogbookResponse = fromList(droneLogbookSuccessResponseStub);

        assertThat(droneLogbookResponse.getStatus()).isEqualTo(0);
        assertThat(droneLogbookResponse.getDescription().isPresent()).isTrue();
        assertThat(droneLogbookResponse.getDescription().get()).isEqualTo("Test description.");
        assertThat(droneLogbookResponse.getUrl().isPresent()).isTrue();
        assertThat(droneLogbookResponse.getUrl().get()).isEqualTo("http:\\\\host:port?some_var=some_val%12");
        assertThat(droneLogbookResponse.isWarning()).isFalse();
        assertThat(droneLogbookResponse.isUploadSucceed()).isTrue();
        assertThat(droneLogbookResponse.isFlightDuplicated()).isFalse();
    }

    @Test
    void testFlightDuplicationResponse() {
        final ArrayList<String> droneLogbookDuplicateFlightResponse =
                list("{\"status\":0,\"description\":\"Duplicate file\"}");

        final DroneLogbookResponse droneLogbookResponse = fromList(droneLogbookDuplicateFlightResponse);

        assertThat(droneLogbookResponse.getStatus()).isEqualTo(0);
        assertThat(droneLogbookResponse.getDescription().isPresent()).isTrue();
        assertThat(droneLogbookResponse.getDescription().get()).isEqualTo("Duplicate file");
        assertThat(droneLogbookResponse.getUrl().isPresent()).isFalse();
        assertThat(droneLogbookResponse.isWarning()).isTrue();
        assertThat(droneLogbookResponse.isUploadSucceed()).isFalse();
        assertThat(droneLogbookResponse.isFlightDuplicated()).isTrue();
    }
}