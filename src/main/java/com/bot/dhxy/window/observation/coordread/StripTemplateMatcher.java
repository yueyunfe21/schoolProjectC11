package com.bot.dhxy.window.observation.coordread;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Mechanical client-side port of the cloud TemplateMatcher's template-loading half
 * (dhxy-cloud-brain com.yueyunfe.dhxy.cloudbrain.TemplateMatcher), trimmed to what
 * {@link LocalCoordinateStripReader} needs: classpath/dir/jar template loading.
 */
final class StripTemplateMatcher {

    private StripTemplateMatcher() {
    }

    static List<NamedTemplate> loadTemplates(String resourceDir, Predicate<String> filter) {
        Map<String, NamedTemplate> templates = new LinkedHashMap<>();
        try {
            Enumeration<URL> urls = StripTemplateMatcher.class.getClassLoader().getResources(resourceDir);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                for (NamedTemplate template : loadTemplates(url, resourceDir, filter)) {
                    templates.putIfAbsent(template.name(), template);
                }
            }
            if (templates.isEmpty()) {
                for (NamedTemplate template : loadTemplatesFromCodeSource(resourceDir, filter)) {
                    templates.putIfAbsent(template.name(), template);
                }
            }
            return List.copyOf(templates.values());
        } catch (Exception e) {
            return List.of();
        }
    }

    private static List<NamedTemplate> loadTemplates(URL url, String resourceDir, Predicate<String> filter) {
        try {
            if ("file".equalsIgnoreCase(url.getProtocol())) {
                return loadTemplatesFromDirectory(Path.of(URI.create(url.toString())), filter);
            }
            if ("jar".equalsIgnoreCase(url.getProtocol())) {
                URLConnection connection = url.openConnection();
                connection.setUseCaches(false);
                if (connection instanceof JarURLConnection jarConnection) {
                    String entryName = jarConnection.getEntryName();
                    try (JarFile jarFile = jarConnection.getJarFile()) {
                        return loadTemplatesFromJar(jarFile, entryName == null ? resourceDir : entryName, filter);
                    }
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return List.of();
    }

    private static List<NamedTemplate> loadTemplatesFromDirectory(Path dir, Predicate<String> filter) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .filter(path -> filter == null || filter.test(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(StripTemplateMatcher::readTemplate)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        }
    }

    private static List<NamedTemplate> loadTemplatesFromCodeSource(String resourceDir, Predicate<String> filter) {
        try {
            URL location = StripTemplateMatcher.class.getProtectionDomain().getCodeSource().getLocation();
            if (location == null || !"file".equalsIgnoreCase(location.getProtocol())) {
                return List.of();
            }
            Path path = Path.of(URI.create(location.toString()));
            if (Files.isDirectory(path)) {
                return loadTemplatesFromDirectory(path.resolve(resourceDir), filter);
            }
            if (Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".jar")) {
                try (JarFile jarFile = new JarFile(path.toFile())) {
                    return loadTemplatesFromJar(jarFile, resourceDir, filter);
                }
            }
        } catch (Exception e) {
            return List.of();
        }
        return List.of();
    }

    private static List<NamedTemplate> loadTemplatesFromJar(JarFile jarFile, String resourceDir, Predicate<String> filter) {
        String prefix = resourceDir.endsWith("/") ? resourceDir : resourceDir + "/";
        return jarFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().startsWith(prefix))
                .filter(entry -> entry.getName().indexOf('/', prefix.length()) < 0)
                .filter(entry -> entry.getName().toLowerCase().endsWith(".png"))
                .sorted(Comparator.comparing(JarEntry::getName))
                .filter(entry -> {
                    String fileName = entry.getName().substring(prefix.length());
                    return filter == null || filter.test(fileName);
                })
                .map(entry -> readTemplate(jarFile, prefix, entry))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private static Optional<NamedTemplate> readTemplate(Path path) {
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                return Optional.empty();
            }
            String fileName = path.getFileName().toString();
            String name = fileName.substring(0, fileName.length() - 4);
            return Optional.of(new NamedTemplate(name, image));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Optional<NamedTemplate> readTemplate(JarFile jarFile, String prefix, JarEntry entry) {
        try {
            BufferedImage image = ImageIO.read(jarFile.getInputStream(entry));
            if (image == null) {
                return Optional.empty();
            }
            String fileName = entry.getName().substring(prefix.length());
            String name = fileName.substring(0, fileName.length() - 4);
            return Optional.of(new NamedTemplate(name, image));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    record NamedTemplate(String name, BufferedImage image) {
    }
}
