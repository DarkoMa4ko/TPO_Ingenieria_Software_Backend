package chess.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regla de arquitectura: el núcleo solo puede depender de la biblioteca estándar ({@code java.*})
 * y de sí mismo. Nada de frameworks, interfaz gráfica, E/S ni adaptadores. Si alguien agrega un
 * import prohibido, este test falla.
 */
class CoreIndependenceTest {

    private static Path sourceRoot() {
        for (String candidate : new String[] {"src/main/java", "chess-core/src/main/java"}) {
            Path path = Path.of(candidate);
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        throw new IllegalStateException("No se encontró src/main/java (ejecutar los tests desde el módulo chess-core)");
    }

    @Test
    void coreOnlyImportsTheJavaStandardLibraryAndItself() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceRoot())) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                for (String line : Files.readAllLines(file)) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("import ")) {
                        String imported = trimmed.substring("import ".length()).replace("static ", "");
                        boolean allowed = imported.startsWith("java.") || imported.startsWith("chess.core.");
                        boolean forbidden = imported.startsWith("java.awt") || imported.startsWith("javax.")
                                || imported.startsWith("java.sql") || imported.startsWith("java.net")
                                || imported.startsWith("java.io.Console");
                        if (!allowed || forbidden) {
                            violations.add(file.getFileName() + " -> " + imported);
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), violations, "El núcleo importa dependencias externas");
    }

    @Test
    void coreDoesNotPrintOrReadFromTheConsole() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(sourceRoot())) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).collect(Collectors.toList())) {
                String content = Files.readString(file);
                if (content.contains("System.out") || content.contains("System.err") || content.contains("System.in")) {
                    violations.add(file.getFileName().toString());
                }
            }
        }
        assertTrue(violations.isEmpty(), "El núcleo no debe hacer E/S: " + violations);
    }
}
