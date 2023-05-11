# GameOfLife

## Install maven 
I use homebrew so this gets you maven:

<pre><code>brew install maven
</code></pre>

## Install Gradle
This project is currently using 8.1.1

<pre><code>brew install gradle
</code></pre>

## install java - i'm using jdk20
<pre><code>brew install java
</code></pre>

## Install Processing

This project uses processing for animation.  You can download it here: [Link text](https://processing.org/download)

## Make gradle aware of a local maven repo for the processing core.jar
I couldn't find a repo that hosted the core.jar and i'm not knowledgeable enough or motivated enough to figure this out.  So a friend helped me figure out how i could install it as a local maven repo that is referenceable in my build.gradle

First, locate the core.jar that you downaloaded from processing.org so you can install into the local repo.  I copied my Processing.app to /Applications on my mac so my core.jar is located here:

/Applications/Processing.app/Contents/Java/core/library/core.jar

You'll have to figure it out the location for your own environment.

To set it up to work as a local maven repo, run this command - **be sure to replace the path to your core.jar in the -Dfile argument** 

<pre><code>mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=/Applications/Processing.app/Contents/Java/core/library/core.jar -DgroupId=com.processing -DartifactId=processing -Dversion=4.2 -Dpackaging=jar
</code></pre>

After running that command you should see "BUILD SUCCESS" and you can check that your stuff is in the right place with: 

<pre><code>ls ~/.m2/repository/com/processing/processing
</code></pre>

At which point it will be set up to work with this gradle project
