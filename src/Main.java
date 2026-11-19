public class Main {
    public static void main(String[] args) {
        try {
            if (args.length != 1) throw new IllegalArgumentException("Usage: ./run.sh <config.csv>");
            Config config = new Config(args[0]);
            config.values.forEach((key, value) -> System.out.println(key + "=" + value));
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
