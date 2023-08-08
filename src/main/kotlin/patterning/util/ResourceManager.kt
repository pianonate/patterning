package patterning.util

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Random
import java.util.jar.JarFile
import java.util.stream.Collectors

object ResourceManager {
    fun getRandomResourceAsString(directoryName: String): String {
        val files = getResourceFilePaths(directoryName)
        if (files.isEmpty()) {
            throw IOException("No files found in directory: $directoryName")
        }
        val randomFile = files[Random().nextInt(files.size)]
        return getResourceAsString(randomFile)
    }
    
    private fun getResourceFilePaths(directoryName: String): List<String> {
        val url = classLoader.getResource(directoryName) ?: throw IOException("Directory not found: $directoryName")
        return when (url.protocol) {
            "jar" -> getResourceFilePathsFromJar(url, directoryName)
            "file" -> getResourceFilePathsFromFileSystem(url, directoryName)
            else -> throw IOException("Cannot list files for URL $url")
        }
    }
    
    private fun getResourceFilePathsFromJar(url: URL, directoryName: String): List<String> {
        val dirPath = url.path
        val jarPath = dirPath.substring(5, dirPath.indexOf("!")) // strip out only the JAR file
        val filenames: MutableList<String> = ArrayList()
        JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8)).use { jar ->
            val entries = jar.entries() // gives ALL entries in jar
            val matchPath = "$directoryName/"
            while (entries.hasMoreElements()) {
                val name = entries.nextElement().name
                if (name.startsWith(matchPath)) { // filter according to the directory
                    val filename = name.substring(matchPath.length)
                    if (filename.isNotEmpty()) { // Ignore directory
                        filenames.add("$directoryName/$filename")
                    }
                }
            }
        }
        return filenames
    }
    
    private fun getResourceFilePathsFromFileSystem(url: URL, directoryName: String): List<String> {
        val dirPath = Paths.get(url.toURI())
        val filenames: MutableList<String> = ArrayList()
        Files.newDirectoryStream(dirPath).use { stream ->
            for (path in stream) {
                if (!Files.isDirectory(path)) { // Ignore subdirectories
                    filenames.add(directoryName + "/" + path.fileName.toString())
                }
            }
        }
        return filenames
    }
    
    private fun getResourceAsString(resourceName: String): String {
        classLoader.getResourceAsStream(resourceName).use { `is` ->
            if (`is` == null) {
                throw IOException("Resource not found: $resourceName")
            }
            BufferedReader(InputStreamReader(`is`)).use { reader ->
                return reader.lines().collect(Collectors.joining("\n"))
            }
        }
    }
    
    private val classLoader: ClassLoader
        get() = ResourceManager::class.java.classLoader
    
    fun getResourceAtFileIndexAsString(directoryName: String, fileIndex: Int): String {
        var number = fileIndex
        val files = getResourceFilePaths(directoryName)
        if (files.isEmpty()) {
            throw IOException("No files found in directory: $directoryName")
        }
        if (files.size < number) {
            number = files.size
        }
        return getResourceAsString(files[number - 1])
    }
    
    val RLE_DIRECTORY = "rle"
    
}