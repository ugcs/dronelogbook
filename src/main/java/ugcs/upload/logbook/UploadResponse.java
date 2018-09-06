package ugcs.upload.logbook;

import org.json.JSONObject;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.ofNullable;

public class UploadResponse {

    private final static Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{.*}");

    private final String description;

    private final String url;

    static UploadResponse fromList(List<String> stringList) {
        final Matcher matcher = JSON_OBJECT_PATTERN.matcher(String.join(" ", stringList));
        if (matcher.find()) {
            return new UploadResponse(new JSONObject(matcher.group()));
        }
        return new UploadResponse();
    }

    private UploadResponse(JSONObject jsonObject) {
        this.description = jsonObject.has("description") ? jsonObject.getString("description") : null;
        this.url = jsonObject.has("url") ? jsonObject.getString("url") : null;
    }

    private UploadResponse() {
        this.description = null;
        this.url = null;
    }

    public Optional<String> getDescription() {
        return ofNullable(description);
    }

    public Optional<String> getUrl() {
        return ofNullable(url);
    }

    public boolean isWarning() {
        return getUrl().isPresent();
    }

}
