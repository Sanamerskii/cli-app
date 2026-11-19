import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MavenRepositoryTest {
    public static void main(String[] args) throws Exception {
        Path repo = Files.createTempDirectory("maven-test");
        try {
            writePom(repo, "parent", "<properties><lib.version>2</lib.version></properties>"
                    + "<dependencyManagement><dependencies>" + dep("lib", "${lib.version}")
                    + "</dependencies></dependencyManagement>");
            writePom(repo, "bom", "<dependencyManagement><dependencies>" + dep("other", "3")
                    + "</dependencies></dependencyManagement>");
            writePom(repo, "app", "<parent><groupId>test</groupId><artifactId>parent</artifactId>"
                    + "<version>1</version></parent><dependencyManagement><dependencies>"
                    + "<dependency><groupId>test</groupId><artifactId>bom</artifactId><version>1</version>"
                    + "<type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>"
                    + "<dependencies>" + dep("lib", "") + dep("other", "") + "</dependencies>");
            List<String> actual = new MavenRepository(repo.toUri().toString()).dependencies("test:app", "1");
            if (!actual.equals(List.of("test:lib:2", "test:other:3"))) throw new AssertionError(actual);
            System.out.println("Parent properties, managed versions and imported BOM: OK");
        } finally {
            try (var paths = Files.walk(repo)) {
                for (Path path : paths.sorted((a, b) -> b.compareTo(a)).toList()) Files.delete(path);
            }
        }
    }

    private static String dep(String name, String version) {
        String versionField = version.isEmpty() ? "" : "<version>" + version + "</version>";
        return "<dependency><groupId>test</groupId><artifactId>" + name + "</artifactId>"
                + versionField + "</dependency>";
    }

    private static void writePom(Path repo, String artifact, String content) throws Exception {
        Path directory = repo.resolve("test").resolve(artifact).resolve("1");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(artifact + "-1.pom"), "<project><groupId>test</groupId><artifactId>"
                + artifact + "</artifactId><version>1</version>" + content + "</project>");
    }
}
