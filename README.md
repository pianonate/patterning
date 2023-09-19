# patterning

This project is a visualizer of (to begin with) [John Conway's Game of Life](https://en.wikipedia.org/wiki/Conway%27s_Game_of_Life), a cellular automata that he created in 1970. There's
a way better visualizer out there called golly, which goes beyond just the game of life, but this one is mine and I'm
trying to learn things by working on it. Right now, I'm learning, exploring and optimizing the hashlife algorithm, as
well as kotlin idioms.

If you want to change something, make a pull request - i'd love to see what you do to improve this.

If you want to know what I want to do next, create an issue and I'll try to respond.

once you get your environment set up and run the application, it will automatically load a pattern from resources. additionally you can press the numbers 1 - 9 on the keyboard to load other patterns

### patterns on copy.sh

You can also paste a valid RLE of a lifeform into it. Many can be found here:

https://copy.sh/life/examples/

Under the patterns column, pick one, click on the "pattern file" link, copy everything and paste it onto the canvas of this app,
and then it will be off to the races.

## try out the code in your own environment

I use homebrew - if you want to follow my path go to https://brew.sh/ and get it

### install gradle

This project is currently using 8.3

<pre><code>brew install gradle
</code></pre>

### install java - i'm using jdk20

do what you need to do to make your IDE be able to find jdk20. in intellij i opened module settings and selected Project
and in the SDK drop down I chose to add SDK and then chose 20. There are a lot of ways to get java. If you're this far
you probably already know what to do.

### this project uses kotlin

the build.gradle.kts file should pull in the latest version - at least it does for me

### install Processing for its core.jar and openGL jars

This project uses processing for animation. You can download it
here: [processing.org/download](https://processing.org/download)

### make gradle aware of the path to your core.jar

I couldn't find a repo that hosted the core.jar and i'm not knowledgeable enough or motivated enough to figure this out.
So you need to update build.gradle.kts with the path to your Processing jars and libraries.

Find where you installed processing and the core.jar that you downloaded from processing.org. I copied my Processing.app
to **/Applications** on my Mac so my core.jar is located here:

/Applications/Processing.app/Contents/Java/core.jar

You'll have to figure it out the location for your own environment and update the pathToCore val in your
build.gradle.kts - don't commit this change back as it will break me. I'll need to find a way to have gradle load that
variable from a different file but for now this is how I'm getting processing...

### update gradle properties if you're using vscode

because i'm not a gradle wonk, the hard lesson to learn when getting up and running with vscod , using its gradle
extension, was that i had to update the distributionUrl in .gradle/gradle-wrapper.properties to 8.1.1 which is what i
had installed with brew. You're probably an expert, so you know all about this sort of thing. i've switched to using
intelliJ because of it's amazing support for Kotlin - and I'm not sure if intellij just knows how to do that that sort
of thing.

### that's it for the setup

good luck - and let me know if any of this is wrong, or can be improved - I'd like it to be fully automated but that's
for another day.

### trying it out

There are some built in starting patterns - hooked up to the number keys (press 1 through 9).
