Cpu Watcher
===============
[![Build Status](https://travis-ci.org/dyorgio/cpu-watcher.svg?branch=master)](https://travis-ci.org/dyorgio/cpu-watcher) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.dyorgio.runtime/cpu-watcher/badge.svg?1)](https://maven-badges.herokuapp.com/maven-central/com.github.dyorgio.runtime/cpu-watcher)

[GitHub Pages Site](https://dyorgio.github.io/cpu-watcher/)

A Java Library/App to limit another process CPU usage.

Why use it?
-----
* Limit any process cpu time usage in a multiplatform way.

Usage
-----
As Java library:

```java
// get target pid (on example we are using our own pid)
long pid = SigarUtil.getCurrentPid();
// Create a new CpuWatcher object. 
// Target percentage is not per core, is always over the entire system load, 
// on example above we want 50% of 1 core only, and host cpu has 8 cores (4 phisical, 4 HT).
// 50%/8 = 6.25%
CpuWatcher cpuWatcher = new CpuWatcher(pid, 6.25f);
// start watcher thread
cpuWatcher.start();
// You can monitor current cpu usage too!
cpuWatcher.getCpuUsage();
// Wait for process (optional);
cpuWatcher.join();
```

As Standalone App:

```bash
java -jar cpu-watcher-{{ site.lib-version }}.jar $PID $MAX_CPU
```

Maven
-----
For common platforms: Windows 32/64bits, Linux Desktop 32/64bits and Mac 64bits support included.
```xml
<dependency>
    <groupId>com.github.dyorgio.runtime</groupId>
    <artifactId>cpu-watcher</artifactId>
    <version>{{ site.lib-version }}</version>
</dependency>
```

For only selected platform: win-universal, win-x64, mac-universal, mac-x64, linux-universal, linux-desktop-universal
```xml
<dependency>
    <groupId>com.github.dyorgio.runtime</groupId>
    <artifactId>cpu-watcher</artifactId>
    <version>{{ site.lib-version }}</version>
    <classifier>${platform}</classifier>
</dependency>
```

For other platforms (aix, ppc, solaris, freebsd, etc..) download source and adjust pom.xml.