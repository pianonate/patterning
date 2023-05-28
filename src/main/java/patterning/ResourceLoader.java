package patterning;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class ResourceLoader {

    private ResourceLoader() {

    }

    public static String getRandomResourceAsString(String directoryName) throws IOException, URISyntaxException {
        List<String> files = getResourceFilePaths(directoryName);
        if (files.isEmpty()) {
            throw new IOException("No files found in directory: " + directoryName);
        }
        String randomFile = files.get(new Random().nextInt(files.size()));
        return getResourceAsString(randomFile);
    }


    private static List<String> getResourceFilePaths(String directoryName) throws IOException, URISyntaxException {
        URL url = getClassLoader().getResource(directoryName);
        if (url == null) {
            throw new IOException("Directory not found: " + directoryName);
        }

        return switch (url.getProtocol()) {
            case "jar" -> getResourceFilePathsFromJar(url, directoryName);
            case "file" -> getResourceFilePathsFromFileSystem(url, directoryName);
            default -> throw new IOException("Cannot list files for URL " + url);
        };
    }

    private static List<String> getResourceFilePathsFromJar(URL url, String directoryName) throws IOException {
        String dirPath = url.getPath();
        String jarPath = dirPath.substring(5, dirPath.indexOf("!")); // strip out only the JAR file
        List<String> filenames = new ArrayList<>();

        try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
            Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
            String matchPath = directoryName + "/";
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(matchPath)) { // filter according to the directory
                    String filename = name.substring(matchPath.length());
                    if (!filename.isEmpty()) { // Ignore directory
                        filenames.add(directoryName + "/" + filename);
                    }
                }
            }
        }

        return filenames;
    }

    private static List<String> getResourceFilePathsFromFileSystem(URL url, String directoryName) throws IOException, URISyntaxException {
        Path dirPath = Paths.get(url.toURI());
        List<String> filenames = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) { // Ignore subdirectories
                    filenames.add(directoryName + "/" + path.getFileName().toString());
                }
            }
        }

        return filenames;
    }

    private static String getResourceAsString(String resourceName) throws IOException {
        try (InputStream is = getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    private static ClassLoader getClassLoader() {
        return ResourceLoader.class.getClassLoader();
    }
}
