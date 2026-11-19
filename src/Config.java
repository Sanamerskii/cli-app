import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class Config {
    private static final int FIELD_COUNT = 2;
    public final Map<String, String> values = new LinkedHashMap<>();

    public Config(String file) throws IOException {
        for (String line : Files.readAllLines(Path.of(file))) {
            String[] fields = line.split(",", -1);
            if (fields.length != FIELD_COUNT || values.putIfAbsent(fields[0].trim(), fields[1].trim()) != null) {
                throw new IllegalArgumentException("Invalid CSV line: " + line);
            }
        }
        validate();
    }

    private void validate() {
        String[] keys = {"package", "repository", "testMode", "version", "output", "ascii", "filter"};
        if (values.size() != keys.length) throw new IllegalArgumentException("Expected seven configuration keys");
        for (String key : keys) {
            if (!values.containsKey(key)) throw new IllegalArgumentException("Missing parameter: " + key);
        }
        requireValue("package");
        requireValue("repository");
        requireValue("version");
        requireValue("output");
        checkBoolean("testMode");
        checkBoolean("ascii");
        if (values.get("testMode").equals("true")) {
            validateTestRepository();
        } else {
            validateMavenRepository();
        }
        if (!values.get("output").endsWith(".svg")) throw new IllegalArgumentException("Output must end in .svg");
    }

    private void validateTestRepository() {
            if (!values.get("package").matches("[A-Z]+")) throw new IllegalArgumentException("Invalid test package");
            if (!Files.isRegularFile(Path.of(values.get("repository")))) {
                throw new IllegalArgumentException("Test repository file not found");
            }
    }

    private void validateMavenRepository() {
        if (!values.get("package").matches("[A-Za-z0-9_.-]+:[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Package must be groupId:artifactId");
        }
        URI uri = URI.create(values.get("repository"));
        if (uri.getHost() == null || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IllegalArgumentException("Repository must be an HTTP(S) URL");
        }
    }

    private void requireValue(String key) {
        if (values.get(key).isEmpty()) throw new IllegalArgumentException("Empty parameter: " + key);
    }

    private void checkBoolean(String key) {
        String value = values.get(key);
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalArgumentException("Parameter must be true or false: " + key);
        }
    }
}
