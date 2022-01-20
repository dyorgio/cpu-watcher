/** *****************************************************************************
 * Copyright 2022 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***************************************************************************** */
package dyorgio.runtime.cpu.watcher;

import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;
import oshi.util.GlobalConfig;

/**
 * Thread to watch and, optionally, limit another process cpu usage.
 *
 * @author dyorgio
 */
public final class CpuWatcher extends Thread {

    static {
        GlobalConfig.set("oshi.util.memoizer.expiration", 0);
    }
    static final SystemInfo SYSTEM_INFO = new SystemInfo();
    static final OperatingSystem OPERATING_SYSTEM = SYSTEM_INFO.getOperatingSystem();
    private static int CPU_COUNT = -1;

    private final long pid;
    private final int cpuCount;
    private final AbstractProcessWatcher processWatcher;

    private volatile Float usageLimit;
    private final ThreadLocal<CpuTimeSnapshot> previousCpuTime = new ThreadLocal();

    private CpuTimeSnapshot previousCpuTimeLocal = null;

    public CpuWatcher(int pid, Float usageLimit) {
        this(null, pid, usageLimit);
    }

    public CpuWatcher(ThreadGroup group, int pid, Float usageLimit) {
        super(group, null, "CpuWatcher[PID:" + pid + "]");
        AbstractProcessWatcherFactory factory = AbstractProcessWatcherFactory.getInstance();

        try {
            if (pid == factory.getCurrentPid()) {
                throw new RuntimeException("You cannot use your own pid(" + pid + "), deadlock will occours.");
            }
            // cache for PERF
            cpuCount = getCpuCount();
        } catch (RuntimeException r) {
            throw r;
        } catch (Exception t) {
            throw new RuntimeException("Error while getting CPU count.", t);
        }
        this.pid = pid;
        setUsageLimit(usageLimit);
        this.processWatcher = factory.createWatcher(pid);

        setDaemon(true);
        setPriority(MAX_PRIORITY);
    }

    public long getPid() {
        return pid;
    }

    public void setUsageLimit(Float usageLimit) {
        if (usageLimit != null && usageLimit < 0) {
            throw new RuntimeException("Invalid usage limit (" + usageLimit + "), cannot be negative.");
        }
        this.usageLimit = usageLimit;
        previousCpuTimeLocal = null;
    }

    public Float getUsageLimit() {
        return usageLimit;
    }

    public float getCpuUsage() {
        if (previousCpuTime.get() != null) {
            CpuTimeSnapshot current = processWatcher.getCpuTimes();
            try {
                return current.getCpuUsage(previousCpuTime.get()) / cpuCount;
            } finally {
                previousCpuTime.set(current);
            }
        } else {
            previousCpuTime.set(processWatcher.getCpuTimes());
            return 0;
        }
    }

    public AbstractProcessWatcher getProcessWatcher() {
        return processWatcher;
    }

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {

        CpuTimeSnapshot current;
        CpuTimeSnapshot prev;

        Thread resumeProcessHook = new Thread("Resume CpuWatcher Process") {
            @Override
            public void run() {
                processWatcher.resume();
            }
        };

        Runtime.getRuntime().addShutdownHook(resumeProcessHook);

        Float localUsageLimit;

        while (!isInterrupted()) {
            try {
                localUsageLimit = this.usageLimit;
                if (localUsageLimit == null) {
                    while (this.usageLimit == null) {
                        Thread.sleep(25);
                    }
                } else {
                    processWatcher.resume();
                    float usageDiff;
                    float currUsage;
                    while ((localUsageLimit = this.usageLimit) != null) {

                        if (processWatcher.isSuspended()) {
                            processWatcher.resume();
                        }

                        current = processWatcher.getCpuTimes();
                        
                        prev = previousCpuTimeLocal;
                        if (prev == null) {
                            prev = previousCpuTimeLocal = current;
                        }
                        usageDiff = (currUsage = (current.getCpuUsage(prev) / cpuCount)) - localUsageLimit;

                        if (usageDiff > 0) {
//                            System.out.println("DIFF: " + usageDiff //
//                                    + ", USAGE: " + currUsage //
//                                    + ", LIMIT: " + localUsageLimit //
//                                    + ", SLEEP:" + Math.min((long) (Math.pow((currUsage / localUsageLimit), 2) * 100f), 500)//
//                            );
                            if (!processWatcher.isSuspended()) {
                                processWatcher.suspend();
                            }
                            Thread.sleep(Math.min((long) (Math.pow((currUsage / localUsageLimit), 2) * 100f), 500));
                        } else {
                            Thread.sleep(10);
                        }
                    }

                }
            } catch (InterruptedException ex) {
                // ignore interruptions errors
                interrupt();
                break;
            } finally {
                try {
                    processWatcher.resume();
                    Runtime.getRuntime().removeShutdownHook(resumeProcessHook);
                } catch (Exception ex) {
                    //ignore
                }
            }
        }
    }

    public static float getOneCoreOnePercent() {
        try {
            return 1f / getCpuCount();
        } catch (Exception t) {
            throw new RuntimeException("Error while getting CPU count.", t);
        }
    }

    public static int getCpuCount() {
        if (CPU_COUNT == -1) {
            CPU_COUNT = SYSTEM_INFO.getHardware().getProcessor().getLogicalProcessorCount();
        }
        return CPU_COUNT;
    }

    @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) throws InterruptedException {
        if (args == null || args.length == 0) {
            System.out.println("Usage: [sudo] java -jar cpu-watcher.jar PID [CPU_MAX_USAGE_PERCENTAGE]");
            System.exit(-1);
        }

        Float limit = args.length == 2 ? Float.valueOf(args[1]) : null;
        final CpuWatcher watcher = new CpuWatcher(Integer.parseInt(args[0]), limit);
        watcher.start();
        while (!Thread.currentThread().isInterrupted()) {
            System.out.println(watcher.getCpuUsage());
            Thread.sleep(1000);
        }
    }
}
