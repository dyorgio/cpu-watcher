Cpu Watcher
===============
[![Build Status](https://travis-ci.org/dyorgio/cpu-watcher.svg?branch=master)](https://travis-ci.org/dyorgio/cpu-watcher) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.dyorgio.runtime/cpu-watcher/badge.svg?1)](https://maven-badges.herokuapp.com/maven-central/com.github.dyorgio.runtime/cpu-watcher)

A Java Library/App to monitor/limit another process CPU usage.

Why use it?
-----
* Limit any process cpu time usage in a multiplatform way.

How it works?
-----
Using a new thread to each external process that you want to monitor/limit this library watches cpu times and sends signals according with cpu specified limit and timelapse.

* SIGSTOP/SIGCONT on macos/linux
* NtSuspendProcess/NtResumeProcess on Windows.

Usage
-----
As Java library:

```java
// Create a new CpuWatcher object with target PID (own pid throws exception to prevents deadlock). 
// Target percentage is not per core, is always over the entire system load, 
// on example above we want 50% of 1 core only, and host cpu has 8 cores (4 phisical, 4 HT).
// 50%/8 = 6.25%
CpuWatcher cpuWatcher = new CpuWatcher(pid, 50f * CpuWatcher.getOneCoreOnePercent());
// start watcher thread
cpuWatcher.start();
// You can monitor current cpu usage too!
cpuWatcher.getCpuUsage();
// ... or change usage limit at runtime (null to disable limiter)
cpuWatcher.setUsageLimit(null);
// Wait for process (optional);
cpuWatcher.join();
```

As Standalone App:

```bash
java -jar cpu-watcher-{{ site.lib-version }}.jar $PID $MAX_CPU
```

Maven
-----
With common platforms support (Windows 32/64bits, Linux Desktop 32/64bits and Mac 64bits):
```xml
<dependency>
    <groupId>com.github.dyorgio.runtime</groupId>
    <artifactId>cpu-watcher</artifactId>
    <version>{{ site.lib-version }}</version>
    <!-- no classifier -->
</dependency>
```

With only one platform (win-universal, win-x64, mac-universal, mac-x64, linux-universal, linux-desktop-universal):
```xml
<dependency>
    <groupId>com.github.dyorgio.runtime</groupId>
    <artifactId>cpu-watcher</artifactId>
    <version>{{ site.lib-version }}</version>
    <classifier>${platform}</classifier>
</dependency>
```

For other platforms (aix, ppc, solaris, freebsd, etc..) download source and adjust pom.xml.
