/** Запускает построение и визуализацию графа зависимостей. */
public class Main {
    /**
     * Читает настройки, строит граф и сохраняет его изображение.
     *
     * @param args путь к CSV-файлу настроек; должен быть передан один аргумент
     */
    public static void main(String[] args) {
        try {
            if (args.length != 1) throw new IllegalArgumentException("Usage: ./run.sh <config.csv>");
            Config config = new Config(args[0]);
            Graph graph = new Graph(config);
            Visualizer visualizer = new Visualizer(graph);
            String dot = visualizer.dot();
            System.out.print(dot);
            visualizer.saveSvg(config.values.get("output"), dot);
            if (config.values.get("ascii").equals("true")) visualizer.printTree();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }
}
