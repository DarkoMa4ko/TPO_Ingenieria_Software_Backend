package chess.cli;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garantiza que el UML (docs/uml/*.puml) <b>no es aspiracional</b>: lo compara con las clases reales.
 *
 * <p>Si este test falla, el diagrama y el código se desalinearon y hay que actualizar uno de los dos.
 * Verifica:
 * <ul>
 *   <li>Toda clase/interfaz/enum/record del código aparece en algún diagrama, y viceversa.</li>
 *   <li>El tipo declarado (class, interface, enum, record) y el paquete son los reales.</li>
 *   <li>Cada atributo y método dibujado existe, con la misma visibilidad y {@code static}.</li>
 *   <li>Cada herencia/implementación dibujada es real, y toda herencia real está dibujada.</li>
 *   <li>Cada flecha de dependencia/asociación dibujada corresponde a una referencia real en el bytecode.</li>
 * </ul>
 */
class UmlConsistencyTest {

    private static final Pattern DECLARATION = Pattern.compile(
            "^(abstract class|class|interface|enum)\\s+([A-Za-z_][\\w.]*)(?:\\s*<<(\\w+)>>)?\\s*(\\{)?$");
    private static final Pattern PACKAGE = Pattern.compile("^package\\s+\"([^\"]+)\"\\s*\\{$");
    private static final Pattern RELATION = Pattern.compile(
            "^([A-Za-z_][\\w.]*)\\s+([.\\-*o+<|>]{2,})\\s+([A-Za-z_][\\w.]*)(?:\\s*:.*)?$");

    /** Un tipo dibujado en un diagrama. */
    private static final class Declared {
        final String name;
        final String keyword;
        final String stereotype;
        final String packageLabel;
        final List<String> members = new ArrayList<>();
        final String file;

        Declared(String name, String keyword, String stereotype, String packageLabel, String file) {
            this.name = name;
            this.keyword = keyword;
            this.stereotype = stereotype;
            this.packageLabel = packageLabel;
            this.file = file;
        }
    }

    /** Una flecha dibujada en un diagrama. */
    private static final class Relation {
        final String left;
        final String arrow;
        final String right;
        final String file;

        Relation(String left, String arrow, String right, String file) {
            this.left = left;
            this.arrow = arrow;
            this.right = right;
            this.file = file;
        }

        @Override
        public String toString() {
            return file + ": " + left + " " + arrow + " " + right;
        }
    }

    // ==================================================================================
    // Descubrimiento de archivos y clases reales
    // ==================================================================================

    private static Path projectRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve("docs").resolve("uml"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException("No se encontró la carpeta docs/uml subiendo desde "
                + Path.of("").toAbsolutePath());
    }

    /** Todas las clases del proyecto (de ambos módulos), con nombres tipo "Position.Builder". */
    private static Map<String, Class<?>> realClasses(Path root) throws Exception {
        Map<String, Class<?>> result = new LinkedHashMap<>();
        for (String module : new String[] {"chess-core", "chess-cli"}) {
            Path src = root.resolve(module).resolve("src/main/java");
            List<Path> files;
            try (Stream<Path> walk = Files.walk(src)) {
                files = walk.filter(p -> p.toString().endsWith(".java")).sorted().collect(Collectors.toList());
            }
            for (Path file : files) {
                String fileName = file.getFileName().toString();
                if (fileName.equals("package-info.java")) {
                    continue;
                }
                String packageName = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                        .map(String::trim).filter(l -> l.startsWith("package "))
                        .map(l -> l.substring("package ".length(), l.length() - 1)).findFirst().orElse("");
                String simple = fileName.substring(0, fileName.length() - ".java".length());
                Class<?> type = Class.forName(packageName + "." + simple, false, UmlConsistencyTest.class.getClassLoader());
                register(result, simple, type);
            }
        }
        return result;
    }

    private static void register(Map<String, Class<?>> into, String name, Class<?> type) {
        if (into.put(name, type) != null) {
            throw new IllegalStateException("Nombre de clase repetido en el proyecto: " + name);
        }
        for (Class<?> nested : type.getDeclaredClasses()) {
            if (!nested.isSynthetic()) {
                register(into, name + "." + nested.getSimpleName(), nested);
            }
        }
    }

    // ==================================================================================
    // Lectura de los .puml
    // ==================================================================================

    private static void parseDiagrams(Path umlDir, Map<String, List<Declared>> declared, List<Relation> relations)
            throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.list(umlDir)) {
            files = walk.filter(p -> p.getFileName().toString().matches("\\d\\d-.*\\.puml")).sorted()
                    .collect(Collectors.toList());
        }
        assertTrue(files.size() >= 3, "No se encontraron los diagramas en " + umlDir);
        for (Path file : files) {
            String fileName = file.getFileName().toString();
            Deque<String> contexts = new ArrayDeque<>(); // "package:<etiqueta>" o "class"
            Declared current = null;
            boolean inNote = false;
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("'")) {
                    continue;
                }
                if (inNote) {
                    inNote = !line.equals("end note");
                    continue;
                }
                if (line.startsWith("note ") && !line.contains(":")) {
                    inNote = true;
                    continue;
                }
                if (current != null) {
                    if (line.equals("}")) {
                        current = null;
                        contexts.pop();
                    } else {
                        current.members.add(line);
                    }
                    continue;
                }
                Matcher pkg = PACKAGE.matcher(line);
                if (pkg.matches()) {
                    contexts.push("package:" + pkg.group(1));
                    continue;
                }
                if (line.equals("}")) {
                    contexts.pop();
                    continue;
                }
                Matcher decl = DECLARATION.matcher(line);
                if (decl.matches()) {
                    String label = contexts.stream().filter(c -> c.startsWith("package:")).findFirst()
                            .map(c -> c.substring("package:".length())).orElse(null);
                    Declared d = new Declared(decl.group(2), decl.group(1), decl.group(3), label, fileName);
                    declared.computeIfAbsent(d.name, k -> new ArrayList<>()).add(d);
                    if (decl.group(4) != null) {
                        current = d;
                        contexts.push("class");
                    }
                    continue;
                }
                Matcher rel = RELATION.matcher(line);
                if (rel.matches()) {
                    relations.add(new Relation(rel.group(1), rel.group(2), rel.group(3), fileName));
                }
            }
        }
    }

    // ==================================================================================
    // Verificaciones
    // ==================================================================================

    @Test
    void umlDiagramsMatchTheRealCode() throws Exception {
        Path root = projectRoot();
        Map<String, Class<?>> real = realClasses(root);
        Map<String, List<Declared>> declared = new LinkedHashMap<>();
        List<Relation> relations = new ArrayList<>();
        parseDiagrams(root.resolve("docs/uml"), declared, relations);

        assertTrue(declared.size() > 30, "Se leyeron muy pocas clases de los diagramas: " + declared.size());
        List<String> problems = new ArrayList<>();

        checkSameSetOfTypes(real, declared, problems);
        for (List<Declared> sameType : declared.values()) {
            for (Declared d : sameType) {
                Class<?> type = real.get(d.name);
                if (type != null && !"external".equals(d.stereotype)) {
                    checkKindAndPackage(d, type, problems);
                    checkMembers(d, type, problems);
                }
            }
        }
        for (Relation r : relations) {
            checkRelation(r, real, declared, problems);
        }
        checkEveryRealInheritanceIsDrawn(real, relations, problems);

        assertEquals(List.of(), problems, "El UML y el código no coinciden:\n  " + String.join("\n  ", problems));
    }

    private static void checkSameSetOfTypes(Map<String, Class<?>> real, Map<String, List<Declared>> declared,
                                            List<String> problems) {
        for (Map.Entry<String, List<Declared>> entry : declared.entrySet()) {
            boolean external = entry.getValue().stream().anyMatch(d -> "external".equals(d.stereotype));
            if (!external && !real.containsKey(entry.getKey())) {
                problems.add("El diagrama dibuja '" + entry.getKey() + "' (" + entry.getValue().get(0).file
                        + ") pero esa clase no existe en el código");
            }
        }
        for (String name : real.keySet()) {
            if (!declared.containsKey(name)) {
                problems.add("La clase real '" + name + "' no aparece en ningún diagrama");
            }
        }
    }

    private static void checkKindAndPackage(Declared d, Class<?> type, List<String> problems) {
        String expectedKeyword = type.isInterface() ? "interface"
                : type.isEnum() ? "enum"
                : Modifier.isAbstract(type.getModifiers()) ? "abstract class" : "class";
        if (!expectedKeyword.equals(d.keyword)) {
            problems.add(d.file + ": '" + d.name + "' está dibujada como " + d.keyword + " pero es " + expectedKeyword);
        }
        boolean drawnAsRecord = "record".equals(d.stereotype);
        if (drawnAsRecord != type.isRecord()) {
            problems.add(d.file + ": '" + d.name + "' " + (type.isRecord() ? "es un record y falta <<record>>"
                    : "está marcada <<record>> pero no lo es"));
        }
        if (d.packageLabel != null && d.packageLabel.matches("[a-z][\\w.]*")
                && !d.packageLabel.equals(type.getPackageName())) {
            problems.add(d.file + ": '" + d.name + "' está en el paquete '" + d.packageLabel
                    + "' del diagrama pero su paquete real es '" + type.getPackageName() + "'");
        }
    }

    private static void checkMembers(Declared d, Class<?> type, List<String> problems) {
        for (String raw : d.members) {
            String member = raw.trim();
            final char visibility = "+-#~".indexOf(member.charAt(0)) >= 0 ? member.charAt(0) : 0;
            if (visibility != 0) {
                member = member.substring(1).trim();
            }
            boolean wantStatic = member.contains("{static}");
            member = member.replace("{static}", "").replace("{abstract}", "").trim();
            boolean isMethod = member.contains("(");
            String name = isMethod ? member.substring(0, member.indexOf('(')).trim()
                    : member.split("[:\\s]")[0].trim();

            if (visibility == 0) { // constante de enum
                boolean found = type.isEnum() && Arrays.stream(type.getEnumConstants())
                        .anyMatch(c -> ((Enum<?>) c).name().equals(name));
                if (!found) {
                    problems.add(d.file + ": '" + d.name + "' no tiene la constante '" + name + "'");
                }
                continue;
            }
            boolean found;
            if (isMethod) {
                found = Arrays.stream(type.getDeclaredMethods())
                        .filter(m -> !m.isSynthetic() && m.getName().equals(name))
                        .anyMatch(m -> matches(m.getModifiers(), visibility, wantStatic));
                if (!found && name.equals(d.name.substring(d.name.lastIndexOf('.') + 1))) { // constructor
                    found = Arrays.stream(type.getDeclaredConstructors())
                            .anyMatch(c -> matches(c.getModifiers(), visibility, false));
                }
            } else if (type.isRecord() && visibility == '+') {
                found = Arrays.stream(type.getRecordComponents()).anyMatch(c -> c.getName().equals(name))
                        || Arrays.stream(type.getDeclaredFields())
                        .anyMatch(f -> f.getName().equals(name) && matches(f.getModifiers(), visibility, wantStatic));
            } else {
                found = Arrays.stream(type.getDeclaredFields())
                        .filter(f -> !f.isSynthetic() && f.getName().equals(name))
                        .anyMatch(f -> matches(f.getModifiers(), visibility, wantStatic));
            }
            if (!found) {
                problems.add(d.file + ": '" + d.name + "' no tiene " + (isMethod ? "el método " : "el atributo ")
                        + "'" + raw.trim() + "' (con esa visibilidad / static)");
            }
        }
    }

    private static boolean matches(int modifiers, char visibility, boolean wantStatic) {
        boolean visible;
        switch (visibility) {
            case '+':
                visible = Modifier.isPublic(modifiers);
                break;
            case '-':
                visible = Modifier.isPrivate(modifiers);
                break;
            case '#':
                visible = Modifier.isProtected(modifiers);
                break;
            default:
                visible = !Modifier.isPublic(modifiers) && !Modifier.isPrivate(modifiers)
                        && !Modifier.isProtected(modifiers);
                break;
        }
        return visible && Modifier.isStatic(modifiers) == wantStatic;
    }

    private static void checkRelation(Relation r, Map<String, Class<?>> real, Map<String, List<Declared>> declared,
                                      List<String> problems) {
        boolean leftExternal = isExternal(r.left, declared);
        boolean rightExternal = isExternal(r.right, declared);
        Class<?> left = real.get(r.left);
        Class<?> right = real.get(r.right);
        if ((left == null && !leftExternal) || (right == null && !rightExternal)) {
            problems.add("Flecha con una clase inexistente -> " + r);
            return;
        }
        String arrow = r.arrow;
        if (arrow.contains("|>") || arrow.contains("<|")) {
            boolean leftIsChild = arrow.contains("|>");
            String childName = leftIsChild ? r.left : r.right;
            String parentName = leftIsChild ? r.right : r.left;
            Class<?> child = real.get(childName);
            Class<?> parent = real.get(parentName);
            if (child == null) {
                problems.add("La clase hija es externa o no existe -> " + r);
            } else if (parent == null) { // padre externo (p. ej. RuntimeException)
                if (child.getSuperclass() == null || !child.getSuperclass().getSimpleName().equals(parentName)) {
                    problems.add("'" + childName + "' no extiende '" + parentName + "' -> " + r);
                }
            } else if (!isDirectSubtype(child, parent)) {
                problems.add("'" + childName + "' no hereda/implementa directamente a '" + parentName + "' -> " + r);
            }
        } else if (arrow.contains("+")) {
            if (right == null || right.getDeclaringClass() != left) {
                problems.add("'" + r.right + "' no es una clase anidada de '" + r.left + "' -> " + r);
            }
        } else {
            boolean sourceIsLeft = arrow.endsWith(">") || !arrow.startsWith("<");
            Class<?> source = sourceIsLeft ? left : right;
            Class<?> target = sourceIsLeft ? right : left;
            if (source == null || target == null) {
                return; // relación con un tipo externo: no se verifica
            }
            if (!bytecodeMentions(source, target)) {
                problems.add("'" + source.getSimpleName() + "' no referencia a '" + target.getSimpleName()
                        + "' en su código -> " + r);
            }
        }
    }

    private static boolean isExternal(String name, Map<String, List<Declared>> declared) {
        List<Declared> list = declared.get(name);
        return list != null && list.stream().anyMatch(d -> "external".equals(d.stereotype));
    }

    private static boolean isDirectSubtype(Class<?> child, Class<?> parent) {
        return child.getSuperclass() == parent || Arrays.asList(child.getInterfaces()).contains(parent);
    }

    /** ¿El .class de {@code source} nombra a {@code target}? (campos, parámetros, retornos o cuerpo de métodos). */
    private static boolean bytecodeMentions(Class<?> source, Class<?> target) {
        String resource = "/" + source.getName().replace('.', '/') + ".class";
        try (InputStream in = source.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("No se pudo leer el bytecode de " + source.getName());
            }
            String bytes = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1);
            String internal = Pattern.quote(target.getName().replace('.', '/'));
            return Pattern.compile(internal + "(?![\\w$])").matcher(bytes).find();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void checkEveryRealInheritanceIsDrawn(Map<String, Class<?>> real, List<Relation> relations,
                                                         List<String> problems) {
        Set<String> drawn = new LinkedHashSet<>();
        for (Relation r : relations) {
            if (r.arrow.contains("|>")) {
                drawn.add(r.left + ">" + r.right);
            } else if (r.arrow.contains("<|")) {
                drawn.add(r.right + ">" + r.left);
            }
        }
        for (Map.Entry<String, Class<?>> entry : real.entrySet()) {
            Class<?> child = entry.getValue();
            List<Class<?>> parents = new ArrayList<>(Arrays.asList(child.getInterfaces()));
            if (child.getSuperclass() != null) {
                parents.add(child.getSuperclass());
            }
            for (Class<?> parent : parents) {
                String parentName = real.entrySet().stream().filter(e -> e.getValue() == parent)
                        .map(Map.Entry::getKey).findFirst().orElse(null);
                if (parentName != null && !drawn.contains(entry.getKey() + ">" + parentName)) {
                    problems.add("Falta dibujar que '" + entry.getKey() + "' hereda/implementa '" + parentName + "'");
                } else if (parentName == null && parent == RuntimeException.class
                        && !drawn.contains(entry.getKey() + ">RuntimeException")) {
                    problems.add("Falta dibujar que '" + entry.getKey() + "' extiende RuntimeException");
                }
            }
        }
    }
}
