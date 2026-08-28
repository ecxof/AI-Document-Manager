package com.example.aidocumentmanager;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the dependency direction documented in docs/architecture.md.
 *
 * <p>
 * These rules held only by convention until now, and the config package had in
 * fact broken the second one for as long as it existed: it reached up into
 * DialogService to report a failed config read. A rule nothing checks is a rule
 * that drifts, so it is checked here.
 */
class PackageLayeringTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java", "com", "example", "aidocumentmanager");

    /** MainApp is the JavaFX Application itself, so it is UI by definition. */
    private static final String ENTRY_POINT = "MainApp.java";

    private record SourceFile(Path path, String contents) {

        boolean isUi() {
            return path.toString().replace('\\', '/').contains("/ui/")
                    || path.getFileName().toString().equals(ENTRY_POINT);
        }

        boolean imports(String prefix) {
            return contents.lines().anyMatch(line -> line.startsWith("import " + prefix));
        }
    }

    private static List<SourceFile> mainSources() throws IOException {
        List<SourceFile> sources = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                sources.add(new SourceFile(path, Files.readString(path)));
            }
        }
        return sources;
    }

    @Test
    void nothingBelowTheUiImportsJavaFx() throws IOException {
        List<Path> offenders = mainSources().stream()
                .filter(source -> !source.isUi())
                .filter(source -> source.imports("javafx."))
                .map(SourceFile::path)
                .toList();

        assertTrue(offenders.isEmpty(),
                "these are meant to be testable without a scene graph, but they import JavaFX: " + offenders);
    }

    @Test
    void nothingBelowTheUiDependsOnTheUi() throws IOException {
        List<Path> offenders = mainSources().stream()
                .filter(source -> !source.isUi())
                .filter(source -> source.imports("com.example.aidocumentmanager.ui."))
                .map(SourceFile::path)
                .toList();

        assertTrue(offenders.isEmpty(),
                "the dependency arrow points from ui downwards, never back up: " + offenders);
    }

    @Test
    void theLeafPackagesDependOnNothingOfOurs() throws IOException {
        List<Path> offenders = mainSources().stream()
                .filter(source -> {
                    String path = source.path().toString().replace('\\', '/');
                    return path.contains("/domain/") || path.contains("/common/");
                })
                .filter(source -> source.imports("com.example.aidocumentmanager.")
                        && !onlyImportsItsOwnPackage(source))
                .map(SourceFile::path)
                .toList();

        assertTrue(offenders.isEmpty(),
                "domain and common are the leaves and must not depend on other packages of ours: " + offenders);
    }

    private static boolean onlyImportsItsOwnPackage(SourceFile source) {
        String ownPackage = source.contents().lines()
                .filter(line -> line.startsWith("package "))
                .findFirst()
                .orElseThrow()
                .replace("package ", "")
                .replace(";", "");

        return source.contents().lines()
                .filter(line -> line.startsWith("import com.example.aidocumentmanager."))
                .allMatch(line -> line.startsWith("import " + ownPackage + "."));
    }
}
