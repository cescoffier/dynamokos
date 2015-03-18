The build process is based on Maven.

## Building the sources ##

To compile, just follows the instruction:
  1. Get the sources (from SVN or from the downloads section)
  1. If needed unzip the downloaded archive
  1. Execute `mvn clean install` from the root of the downloaded sources.

That's it !

## Creating the distribution files ##

The creation of distribution is also based on Maven. From the parent pom (root), run:
```
mvn clean install assembly:assembly
```

Files will be created in the `target` folder.