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

public class ResourceReader {
    private final ClassLoader classLoader;
    private final Random random;

    public ResourceReader() {
        this.classLoader = getClass().getClassLoader();
        this.random = new Random();
    }

    public String getRandomResourceAsString(String directoryName) throws IOException, URISyntaxException {
        List<String> files = getResourceFiles(directoryName);
        if (files.isEmpty()) {
            throw new IOException("No files found in directory: " + directoryName);
        }
        String randomFile = files.get(random.nextInt(files.size()));
        return getResourceAsString(randomFile);
    }

    /*         Enumeration<URL> resources = classLoader.getResources(directoryName);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            // Process the resource
            System.out.println("Resource: " + resource);
        }

        return filenames;*/

    private List<String> getResourceFiles(String directoryName) throws IOException, URISyntaxException {
        URL url = classLoader.getResource(directoryName);
        if (url == null) {
            throw new IOException("Directory not found: " + directoryName);
        }

        return switch (url.getProtocol()) {
            case "jar" -> getResourceFilesFromJar(url, directoryName);
            case "file" -> getResourceFilesFromFile(url, directoryName);
            default -> throw new IOException("Cannot list files for URL " + url);
        };
    }

    private List<String> getResourceFilesFromJar(URL url, String directoryName) throws IOException {
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

    private List<String> getResourceFilesFromFile(URL url, String directoryName) throws IOException, URISyntaxException {
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



    public String getResourceAsString(String resourceName) throws IOException {
        try (InputStream is = classLoader.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
