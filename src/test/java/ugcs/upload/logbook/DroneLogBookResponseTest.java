package ugcs.upload.logbook;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.list;
import static ugcs.upload.logbook.DroneLogBookResponse.fromList;

class DroneLogBookResponseTest {
    @Test
    void testSuccessResponse() {
        final ArrayList<String> droneLogbookSuccessResponseStub =
                list("  { " +
                        "\"status\": 1, " +
                        "\"description\": \"Test description.\", " +
                        "\"url\": \"http:\\\\\\\\host:port?some_var=some_val%12\"" +
                        "}   ");

        final DroneLogBookResponse droneLogBookResponse = fromList(droneLogbookSuccessResponseStub);

        assertThat(droneLogBookResponse.getStatus()).isEqualTo(1);
        assertThat(droneLogBookResponse.getDescription().isPresent()).isTrue();
        assertThat(droneLogBookResponse.getDescription().get()).isEqualTo("Test description.");
        assertThat(droneLogBookResponse.getUrl().isPresent()).isTrue();
        assertThat(droneLogBookResponse.getUrl().get()).isEqualTo("http:\\\\host:port?some_var=some_val%12");
        assertThat(droneLogBookResponse.isWarning()).isFalse();
        assertThat(droneLogBookResponse.isUploadSucceed()).isTrue();
    }
}