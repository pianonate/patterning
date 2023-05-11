# GameOfLife

## Install Maven 
I use homebrew so that's

<code>
brew install maven
</code>

Install Gradle - i'm using 8.1.1

brew install gradle

install java - i'm using jdk20

brew install java

Install Processing - i got it here:

https://processing.org/download

Locate the core.jar so you can install it in a local maven repo that can be referenced by the build.gradle in this project

I copied my Processing.app to Applications so my core.jar on my mac is located here:

/Applications/Processing.app/Contents/Java/core/library/core.jar

You'll have to figure it out for your own environment.

To set it up to work as a local maven repo, run this command:

mvn org.apache.maven.plugins:maven-install-plugin:2.5.2:install-file -Dfile=/Applications/Processing.app/Contents/Java/core/library/core.jar -DgroupId=com.processing -DartifactId=processing -Dversion=4.2 -Dpackaging=jar

After running that you should see "BUILD SUCCESS" and you can check that your stuff is there with: 

ls ~/.m2/repository/com/processing/processing

At which point it will be set up to work with this gradle project
