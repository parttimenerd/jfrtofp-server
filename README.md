# jfrtofp
[![Maven Package](https://jitpack.io/v/parttimenerd/jfrtofp-server.svg)](https://jitpack.io/#parttimenerd/jfrtofp-server)
[![Build](https://github.com/parttimenerd/jfrtofp-server/actions/workflows/push.yml/badge.svg)](https://github.com/parttimenerd/jfrtofp-server/actions/workflows/push.yml)

Bundle of the [JFR to FirefoxProfiler converter](https://github.com/parttimenerd/jfrtofp-server) 
with a custom [Firefox Profiler](https://profiler.firefox.com) for JDK 11+.

## Basic Usage
Download the latest `jfrtofp-server-all.jar` release and simply pass the JFR file as its first argument:

```sh
  java -jar jfrtofp-server-all.jar samples/small_profile.jfr
```

This will start a server at `localhost:4243`, `localhost:4243/` redirects to the Firefox Profiler instance.

## Run from Source
Requires NodeJS (see Firefox Profiler build instructions) to be installed.

```sh
  git clone https://github.com/parttimenerd/jfrtofp-server.git
  cd jfrtofp-server
  ./build.sh
  ./gradlew run --args="samples/small_profile.jfr"
```

## Usage as a Library
```xml
<dependency>
    <groupId>com.github.parttimenerd</groupId>
    <artifactId>jfrtofp-server</artifactId>
    <version>main-SNAPSHOT</version>
</dependency>
```
or
```groovy
implementation 'com.github.parttimenerd:jfrtofp-server:main-SNAPSHOT'
```
from [JitPack](https://jitpack.io/#parttimenerd/jfrtofp-server).

## License
MIT