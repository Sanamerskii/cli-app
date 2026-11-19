import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigTest {
    public static void main(String[] args) throws Exception {
        Path file = Files.createTempFile("config", ".csv");
        String valid = "package,org.example:demo\nrepository,https://repo.maven.apache.org/maven2\n"
                + "testMode,false\nversion,1.0\noutput,graph.svg\nascii,false\nfilter,\n";
        try {
            Files.writeString(file, valid);
            new Config(file.toString());
            String[] invalid = {"package,", "repository,", "testMode,maybe", "version,", "output,graph.png",
                    "ascii,maybe", "filter,ignored\nunknown,value"};
            String[] original = {"package,org.example:demo", "repository,https://repo.maven.apache.org/maven2",
                    "testMode,false", "version,1.0", "output,graph.svg", "ascii,false", "filter,"};
            for (int i = 0; i < invalid.length; i++) {
                Files.writeString(file, valid.replace(original[i], invalid[i]));
                try {
                    new Config(file.toString());
                    throw new AssertionError("Accepted invalid parameter: " + original[i]);
                } catch (IllegalArgumentException expected) {
                    System.out.println("Rejected: " + original[i]);
                }
            }
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
