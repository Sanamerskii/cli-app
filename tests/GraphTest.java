import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GraphTest {
    public static void main(String[] args) throws Exception {
        Path repository = Files.createTempFile("graph", ".csv");
        Path config = Files.createTempFile("config", ".csv");
        try {
            Files.writeString(repository, "A,B\nB,C\nC,A\nA,D\n");
            Files.writeString(config, "package,A\nrepository," + repository + "\ntestMode,true\nversion,1\n"
                    + "output,graph.svg\nascii,false\nfilter,D\n");
            Graph graph = new Graph(new Config(config.toString()));
            if (!graph.edges.get("A").equals(List.of("B")) || !graph.edges.get("C").equals(List.of("A"))
                    || graph.edges.containsKey("D")) throw new AssertionError(graph.edges);
            System.out.println("Recursive traversal, cycle and filter: OK");
        } finally {
            Files.deleteIfExists(config);
            Files.deleteIfExists(repository);
        }
    }
}
