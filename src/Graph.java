import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class Graph {
    public final Map<String, List<String>> edges = new LinkedHashMap<>();
    private final Map<String, List<String>> testEdges = new LinkedHashMap<>();
    private final MavenRepository repository;
    private final String filter;
    private final boolean testMode;

    public Graph(Config config) throws Exception {
        testMode = config.values.get("testMode").equals("true");
        filter = config.values.get("filter");
        repository = testMode ? null : new MavenRepository(config.values.get("repository"));
        if (testMode) readTestRepository(config.values.get("repository"));
        String root = config.values.get("package");
        if (!testMode) root += ":" + config.values.get("version");
        visit(root);
    }

    private void readTestRepository(String file) throws Exception {
        for (String line : Files.readAllLines(Path.of(file))) {
            String[] names = line.split(",", -1);
            if (names.length > 2 || !names[0].matches("[A-Z]+")
                    || (names.length == 2 && !names[1].matches("[A-Z]+"))) {
                throw new IllegalArgumentException("Invalid test repository line: " + line);
            }
            testEdges.computeIfAbsent(names[0], key -> new ArrayList<>());
            if (names.length == 2) testEdges.get(names[0]).add(names[1]);
        }
    }

    private void visit(String name) throws Exception {
        if (edges.containsKey(name)) return;
        List<String> children = new ArrayList<>();
        edges.put(name, children);
        for (String dependency : dependencies(name)) {
            String packageName = testMode ? dependency : dependency.substring(0, dependency.lastIndexOf(':'));
            if (!filter.isEmpty() && packageName.contains(filter)) continue;
            children.add(dependency);
            visit(dependency);
        }
    }

    private List<String> dependencies(String name) throws Exception {
        if (testMode) return testEdges.getOrDefault(name, List.of());
        String[] parts = name.split(":", -1);
        if (parts.length != 3 || parts[2].isEmpty() || parts[2].contains("${")) {
            throw new IllegalArgumentException("Cannot resolve Maven dependency version: " + name);
        }
        return repository.dependencies(parts[0] + ":" + parts[1], parts[2]);
    }

    public List<String> loadOrder() {
        List<String> order = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> active = new HashSet<>();
        for (String name : edges.keySet()) appendOrder(name, visited, active, order);
        return order;
    }

    private void appendOrder(String name, Set<String> visited, Set<String> active, List<String> order) {
        if (active.contains(name)) throw new IllegalArgumentException("Cyclic dependency: " + name);
        if (visited.contains(name)) return;
        active.add(name);
        for (String dependency : edges.get(name)) appendOrder(dependency, visited, active, order);
        active.remove(name);
        visited.add(name);
        order.add(name);
    }
}
