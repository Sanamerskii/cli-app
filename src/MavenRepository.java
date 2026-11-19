import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MavenRepository {
    private final String repository;
    private final Map<String, Model> cache = new LinkedHashMap<>();

    private static class Model {
        Map<String, String> properties = new LinkedHashMap<>();
        Map<String, Map<String, String>> managed = new LinkedHashMap<>();
        Map<String, Map<String, String>> dependencies = new LinkedHashMap<>();
    }

    public MavenRepository(String repository) {
        this.repository = repository.replaceAll("/+$", "");
    }

    public List<String> dependencies(String name, String version) throws Exception {
        String[] parts = name.split(":", -1);
        Model model = load(parts[0], parts[1], version, new ArrayList<>());
        List<String> result = new ArrayList<>();
        for (Map<String, String> dependency : model.dependencies.values()) {
            String group = resolve(dependency.get("groupId"), model.properties);
            String artifact = resolve(dependency.get("artifactId"), model.properties);
            String depVersion = resolve(dependency.get("version"), model.properties);
            if (depVersion.isEmpty()) throw new IllegalArgumentException("Missing version: " + group + ":" + artifact);
            result.add(group + ":" + artifact + ":" + depVersion);
        }
        return result;
    }

    private Model load(String group, String artifact, String version, List<String> active) throws Exception {
        String id = group + ":" + artifact + ":" + version;
        if (cache.containsKey(id)) return cache.get(id);
        if (active.contains(id)) throw new IllegalArgumentException("Cyclic POM inheritance: " + id);
        active.add(id);
        Element project = readPom(group, artifact, version);
        Model model = parent(project, active);
        readProperties(project, model, group, artifact, version);
        readManaged(project, model, active);
        readProjectDependencies(project, model);
        active.remove(id);
        cache.put(id, model);
        return model;
    }

    private Element readPom(String group, String artifact, String version) throws Exception {
        String path = group.replace('.', '/') + "/" + artifact + "/" + version + "/"
                + artifact + "-" + version + ".pom";
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try (var input = URI.create(repository + "/" + path).toURL().openStream()) {
            Document document = factory.newDocumentBuilder().parse(input);
            return document.getDocumentElement();
        }
    }

    private Model parent(Element project, List<String> active) throws Exception {
        Element parent = child(project, "parent");
        Model model = new Model();
        if (parent == null) return model;
        Model inherited = load(value(parent, "groupId"), value(parent, "artifactId"), value(parent, "version"), active);
        model.properties.putAll(inherited.properties);
        model.managed.putAll(inherited.managed);
        model.dependencies.putAll(inherited.dependencies);
        model.properties.put("project.parent.groupId", value(parent, "groupId"));
        model.properties.put("project.parent.artifactId", value(parent, "artifactId"));
        model.properties.put("project.parent.version", value(parent, "version"));
        return model;
    }

    private void readProperties(Element project, Model model, String group, String artifact, String version) {
        Element properties = child(project, "properties");
        if (properties != null) {
            for (Node node = properties.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node instanceof Element property) {
                    model.properties.put(property.getTagName(), property.getTextContent().trim());
                }
            }
        }
        model.properties.put("project.groupId", group);
        model.properties.put("project.artifactId", artifact);
        model.properties.put("project.version", version);
        model.properties.put("pom.groupId", group);
        model.properties.put("pom.artifactId", artifact);
        model.properties.put("pom.version", version);
    }

    private void readManaged(Element project, Model model, List<String> active) throws Exception {
        Element management = child(project, "dependencyManagement");
        if (management == null) return;
        for (Element element : elements(child(management, "dependencies"), "dependency")) {
            Map<String, String> dependency = fields(element, model.properties);
            if (dependency.get("type").equals("pom") && dependency.get("scope").equals("import")) {
                Model bom = load(dependency.get("groupId"), dependency.get("artifactId"),
                        dependency.get("version"), active);
                for (var entry : bom.managed.entrySet()) model.managed.putIfAbsent(entry.getKey(), entry.getValue());
            } else {
                model.managed.put(key(dependency), dependency);
            }
        }
    }

    private void readProjectDependencies(Element project, Model model) {
        for (Element element : elements(child(project, "dependencies"), "dependency")) {
            Map<String, String> dependency = fields(element, model.properties);
            Map<String, String> managed = model.managed.get(key(dependency));
            if (dependency.get("version").isEmpty() && managed != null) {
                dependency.put("version", managed.get("version"));
            }
            model.dependencies.put(key(dependency), dependency);
        }
    }

    private Map<String, String> fields(Element element, Map<String, String> properties) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String name : new String[] {"groupId", "artifactId", "version", "type", "scope", "classifier"}) {
            result.put(name, resolve(value(element, name), properties));
        }
        return result;
    }

    private String key(Map<String, String> dependency) {
        String type = dependency.get("type").isEmpty() ? "jar" : dependency.get("type");
        return dependency.get("groupId") + ":" + dependency.get("artifactId") + ":" + type + ":"
                + dependency.get("classifier");
    }

    private String resolve(String text, Map<String, String> properties) {
        for (int pass = 0; pass < properties.size(); pass++) {
            String previous = text;
            for (var property : properties.entrySet()) {
                text = text.replace("${" + property.getKey() + "}", property.getValue());
            }
            if (text.equals(previous)) break;
        }
        return text;
    }

    private static List<Element> elements(Element parent, String name) {
        List<Element> result = new ArrayList<>();
        if (parent == null) return result;
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && element.getTagName().equals(name)) result.add(element);
        }
        return result;
    }

    private static String value(Element parent, String name) {
        Element element = child(parent, name);
        return element == null ? "" : element.getTextContent().trim();
    }

    private static Element child(Element parent, String name) {
        List<Element> children = elements(parent, name);
        return children.isEmpty() ? null : children.get(0);
    }
}
