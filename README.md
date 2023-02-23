# jfrtofp-server
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/me.bechberger/jfrtofp-server?server=https%3A%2F%2Fs01.oss.sonatype.org)
[![Build](https://github.com/parttimenerd/jfrtofp-server/actions/workflows/push.yml/badge.svg)](https://github.com/parttimenerd/jfrtofp-server/actions/workflows/push.yml)

Bundle of the [JFR to FirefoxProfiler converter](https://github.com/parttimenerd/jfrtofp-server) 
with a custom [Firefox Profiler](https://profiler.firefox.com) for JDK 11+.

The project is the base for my [Java Profiler Plugin](https://github.com/parttimenerd/intellij-profiler-plugin)
for IntelliJ IDEA.

*This is in alpha state, it does not work with really large JFR files and might still have bugs.*

It actually uses the custom [Firefox Profiler fork](https://github.com/parttimenerd/firefox-profiler/tree/merged)
which includes many of our own PRs which are not yet upstream (and might be less stable).

## Basic Usage
Download the latest `jfrtofp-server-all.jar` release and simply pass the JFR file as its first argument:

```sh
  java -jar jfrtofp-server-all.jar samples/small_profile.jfr
```

This will start a server at `localhost:4243`, `localhost:4243/` redirects to the Firefox Profiler instance.

## Run from Source
Requires NodeJS (see Firefox Profiler build instructions) to be installed.

```sh
  git clone --recursive https://github.com/parttimenerd/jfrtofp-server.git
  cd jfrtofp-server
  ./build.sh
  ./gradlew run --args="samples/small_profile.jfr"
```

## Usage as a Library
```groovy
dependencies {
    implementation 'com.github.parttimenerd:jfrtofp-server:main-SNAPSHOT'
}

repositories {
    maven {
        url = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
    }
}
```

## License
MIT, Copyright 2023 SAP SE or an SAP affiliate company, Johannes Bechberger
and jfrtofp-server contributors


*This project is a prototype of the [SapMachine](https://sapmachine.io) team
at [SAP SE](https://sap.com)*
