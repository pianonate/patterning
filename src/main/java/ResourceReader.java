import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    public String getRandomResourceAsString(String directoryName) throws IOException {
        List<String> files = getResourceFiles(directoryName);
        if (files.isEmpty()) {
            throw new IOException("No files found in directory: " + directoryName);
        }
        String randomFile = files.get(random.nextInt(files.size()));
        return getResourceAsString(randomFile);
    }

    private List<String> getResourceFiles(String directoryName) throws IOException {
        URL url = classLoader.getResource(directoryName);
        if (url == null) {
            throw new IOException("Directory not found: " + directoryName);
        }
        if (!url.getProtocol().equals("jar")) {
            throw new IOException("Directory listing not supported for protocol: " + url.getProtocol());
        }
        String dirPath = url.getPath();
        String jarPath = dirPath.substring(5, dirPath.indexOf("!")); // strip out only the JAR file
        try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8))) {
            Enumeration<JarEntry> entries = jar.entries(); // gives ALL entries in jar
            String matchPath = directoryName + "/";
            List<String> filenames = new ArrayList<>();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(matchPath)) { // filter according to the directory
                    String filename = name.substring(matchPath.length());
                    if (!filename.isEmpty()) { // Ignore directory
                        filenames.add(directoryName + "/" + filename);
                    }
                }
            }
            return filenames;
        }
    }

    // todo: this  will read through all resources in a jar
    //       so you need to set up a build configuration to create the jar (gradle / jar task)
    //       then a build configuration that is of type Jar Application and before running excute the jar task
    //       and in the Jar Applicatio you need to point to the jar in build/libraries
    //       that's all so that you can get at the resources that are populated in the jar
    //       the getResourceRandom relies (currently) on there being a folder named rle in the resources folder
    //       so there's more work to be done to get this to all be more organized to be more Patterning app specific
    //       and provide the flexibility to load files up into the default set that you're working with
    //

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
