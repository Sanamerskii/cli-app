public class Main {
    public static void main(String[] args) {
        try {
            if (args.length != 1) throw new IllegalArgumentException("Usage: ./run.sh <config.csv>");
            Config config = new Config(args[0]);
            Graph graph = new Graph(config);
            graph.edges.forEach((name, dependencies) -> System.out.println(name + " -> " + dependencies));
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
