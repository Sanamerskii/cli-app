import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class Visualizer {
    private final Graph graph;

    public Visualizer(Graph graph) {
        this.graph = graph;
    }

    public String dot() {
        StringBuilder text = new StringBuilder("digraph dependencies {\n");
        graph.edges.forEach((node, dependencies) -> {
            text.append("  \"").append(escape(node)).append("\";\n");
            for (String dependency : dependencies) {
                text.append("  \"").append(escape(node)).append("\" -> \"")
                        .append(escape(dependency)).append("\";\n");
            }
        });
        return text.append("}\n").toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public void saveSvg(String file, String dot) throws Exception {
        Process process = new ProcessBuilder("dot", "-Tsvg", "-o", file).inheritIO()
                .redirectInput(ProcessBuilder.Redirect.PIPE).start();
        try (var input = process.getOutputStream()) {
            input.write(dot.getBytes(StandardCharsets.UTF_8));
        }
        if (process.waitFor() != 0) throw new IllegalStateException("Graphviz failed to create SVG");
    }

    public void printTree() {
        String root = graph.edges.keySet().iterator().next();
        printNode(root, "", new HashSet<>());
    }

    private void printNode(String node, String indent, Set<String> active) {
        if (!active.add(node)) {
            System.out.println(indent + node + " (cycle)");
            return;
        }
        System.out.println(indent + node);
        for (String dependency : graph.edges.get(node)) printNode(dependency, indent + "  ", active);
        active.remove(node);
    }
}
