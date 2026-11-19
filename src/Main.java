public class Main {
    public static void main(String[] args) {
        try {
            if (args.length != 1) throw new IllegalArgumentException("Usage: ./run.sh <config.csv>");
            Config config = new Config(args[0]);
            MavenRepository repository = new MavenRepository(config.values.get("repository"));
            for (String dependency : repository.dependencies(config.values.get("package"), config.values.get("version"))) {
                System.out.println(dependency);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
