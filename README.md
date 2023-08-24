# patterning
This project is a java based visualizer of (to begin with) John Conway's, game of life that he created in 1970. There's a way better visualize out there called golly, which goes beyond just the game of life, but this one is mine and I'm trying to learn things by working on it. Right now, I'm learning, exploring and optimizing the hashlife algorithm, as well as kotlin idioms.

There are many, many enhancements to make for this to be truly interesting. And a lot of debugging to do still. There are some built in starting patterns - hooked up to the number keys (press 1 through 9). You can also paste a valid RLE of a lifeform into it.  Many can be found here:

https://copy.sh/life/examples/

Under the patterns column, pick one, drill into the screen, copy everything and paste it onto the canvas of this app, and then it will be off to the races.

If you find something that you want to change, then make a pull request - i'd love to see what you do to improve this. If you want to know what I want to do next, create an issue and I'll try to respond.

## try out the code in your own environment

I use homebrew so this gets you maven which you will need as a local repo for the processing core.jar (see below)

<pre><code>brew install maven
</code></pre>

### install gradle
This project is currently using 8.1.1

<pre><code>brew install gradle
</code></pre>

### install java for the jvm - i'm using jdk20
java 20 seems to work fine. i'm also using the kotlin k2 compiler, which is experimental but works and seems speedy. it's specified gradle.properties so you don't have to worry about it. yay!
<pre><code>brew install java
</code></pre>

### this project uses kotlin
the build.gradle.kts file should just pull in the latest version but i haven't tried a clean build in a while so you may have to mess around to get it all working right

### install Processing for its core.jar and openGL jars    

This project uses processing for animation.  You can download it here: [processing.org/download](https://processing.org/download)

### make gradle aware of a local maven repo for the processing core.jar
I couldn't find a repo that hosted the core.jar and i'm not knowledgeable enough or motivated enough to figure this out.  So a friend helped me figure out how i could install it as a local maven repo that is reference able in my build.gradle

First, locate the core.jar that you downloaded from processing.org, so you can install into the local repo.  I copied my Processing.app to **/Applications** on my Mac so my core.jar is located here:

/Applications/Processing.app/Contents/Java/core/library/core.jar

You'll have to figure it out the location for your own environment. You'll also need the gluegen-rt.jar and the jogl-all.jar from the same folder as they are packaged to work specifically with processing. i haven't figured out a way to get them from any public repo either.

To set it up to work as a local maven repo, run these commands - **be sure to replace the path to your core.jar in the -Dfile argument** 

<pre><code>mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=/Applications/Processing.app/Contents/Java/core/library/core.jar -DgroupId=com.processing -DartifactId=processing -Dversion=4.3 -Dpackaging=jar
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=/Applications/Processing.app/Contents/Java/core/library/jogl-all.jar -DgroupId=org.jogamp.jogl -DartifactId=jogl-all -Dversion=2.4.0 -Dpackaging=jar
mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=/Applications/Processing.app/Contents/Java/core/library/gluegen-rt.jar -DgroupId=org.jogamp.gluegen -DartifactId=gluegen-rt -Dversion=2.4.0 -Dpackaging=jar
</code></pre>

After running that command you should see "BUILD SUCCESS" and you can check that your stuff is in the right place with: 

<pre><code>ls ~/.m2/repository/com/processing/processing
</code></pre>


### we need to reference the openGL native libraries
THese libraries are installed with processing - i didn't go through the trouble (yet) of putting them all into the local maven- i know, inconsistent. You'll be able to see at the top of the build.gradle.kts that it references a variable that points to where they are located within your processing install.  On my machine that would be:

<pre><code>val pathToJoglLibraries = "/Applications/Processing.app/Contents/Java/core/library/"
</code></pre>

you'll need to change that to match your environment.  I'm not sure if this is the best way to do this, but it works for me.  If you have a better way, please let me know.


### update gradle properties
because i'm not a gradle wonk, the hard lesson to learn when getting up and running with vscode, using its gradle extension, was that i had to update the distributionUrl in .gradle/gradle-wrapper.properties to 8.1.1 which is what i had installed with brew. You're probably an expert, so you know all about this sort of thing.  i've switched to using intelliJ because of it's amazing support for Kotlin - and I'm not sure if intellij just knows how to do that that sort of thing.

### that's it for the setup
good luck - and let me know if any of this is wrong, or can be improved - I'd like it to be fully automated but that's for another day.