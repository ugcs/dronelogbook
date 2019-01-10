package ugcs.upload.logbook;

import lombok.ToString;
import org.json.JSONObject;
import ugcs.common.operation.Operation;
import ugcs.exceptions.logbook.LogbookAuthorizationFailed;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

/**
 * Upload {@link Operation} result data object
 *
 * @see FlightUploadPerformerFactory
 */
@ToString
public class DroneLogbookResponse {
    private final static int UNDEFINED_STATUS_VALUE = -1;
    private final static int WRONG_EMAIL_STATUS_VALUE = 1;
    private final static int WRONG_PASSWORD_STATUS_VALUE = 2;

    private final static Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{.*}");

    private final int status;

    private final String description;

    private final String url;

    static DroneLogbookResponse fromList(List<String> stringList) {
        final Matcher matcher = JSON_OBJECT_PATTERN.matcher(String.join(" ", stringList));
        if (matcher.find()) {
            return new DroneLogbookResponse(new JSONObject(matcher.group()));
        }
        return new DroneLogbookResponse();
    }

    private DroneLogbookResponse(JSONObject jsonObject) {
        this.status = jsonObject.has("status") ? jsonObject.getInt("status") : UNDEFINED_STATUS_VALUE;
        this.description = jsonObject.has("description") ? jsonObject.getString("description") : null;
        this.url = jsonObject.has("url") ? jsonObject.getString("url") : null;
    }

    private DroneLogbookResponse() {
        this.status = UNDEFINED_STATUS_VALUE;
        this.description = null;
        this.url = null;
    }

    public int getStatus() {
        return status;
    }

    public Optional<String> getDescription() {
        return ofNullable(description);
    }

    public Optional<String> getUrl() {
        return ofNullable(url);
    }

    public boolean isWarning() {
        return !getUrl().isPresent();
    }

    public boolean isUploadSucceed() {
        return getUrl().isPresent();
    }

    public boolean isFlightDuplicated() {
        return isWarning() && getDescription()
                .filter(s -> s.contains("Duplicate") && s.contains("file"))
                .isPresent();
    }

    public void assertAuthorizationSucceed() {
        if (isAuthorizationFailed()) {
            throw getDescription()
                    .map(LogbookAuthorizationFailed::new)
                    .orElseGet(LogbookAuthorizationFailed::new);
        }
    }

    private boolean isAuthorizationFailed() {
        return getStatus() == WRONG_EMAIL_STATUS_VALUE || getStatus() == WRONG_PASSWORD_STATUS_VALUE;
    }
}
