/** *****************************************************************************
 * Copyright 2020 See AUTHORS file.
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

import org.hyperic.sigar.SigarException;

/**
 * Thread to watch and, optionally, limit another process cpu usage.
 *
 * @author dyorgio
 */
public final class CpuWatcher extends Thread {

    private static final long ONE_MILLIS_IN_NANOS = 1000000l;
    private static final long ONE_SECOND_IN_NANOS = 1000l * ONE_MILLIS_IN_NANOS;

    private static final long WAKEUP_LIMITER_UP = 500l * ONE_MILLIS_IN_NANOS;
    private static final long WAKEUP_LIMITER_DOWN = -500l * ONE_MILLIS_IN_NANOS;

    private final long pid;
    private final int cpuCount;
    private final AbstractProcessWatcher processWatcher;

    private volatile Float usageLimit;
    private CpuTimeSnapshot prev;

    public CpuWatcher(long pid, Float usageLimit) {
        this(null, pid, usageLimit);
    }

    public CpuWatcher(ThreadGroup group, long pid, Float usageLimit) {
        super(group, null, "CpuWatcher[PID:" + pid + "]", 32l * 1024l);
        try {
            if (pid == SigarUtil.getCurrentPid()) {
                throw new RuntimeException("You cannot use your own pid(" + pid + "), deadlock will occours.");
            }
            cpuCount = SigarUtil.getCpuCount();
        } catch (RuntimeException r) {
            throw r;
        } catch (SigarException t) {
            throw new RuntimeException("Error while getting CPU count.", t);
        }
        this.pid = pid;
        setUsageLimit(usageLimit);
        this.processWatcher = AbstractProcessWatcherFactory.getInstance().createWatcher(pid);

        setDaemon(true);
    }

    public long getPid() {
        return pid;
    }

    public void setUsageLimit(Float usageLimit) {
        if (usageLimit != null && usageLimit < 0) {
            throw new RuntimeException("Invalid usage limit (" + usageLimit + "), cannot be negative.");
        }
        this.usageLimit = usageLimit;
    }

    public Float getUsageLimit() {
        return usageLimit;
    }

    public float getCpuUsage() {
        if (prev != null) {
            CpuTimeSnapshot current = processWatcher.getCpuTimes();
            try {
                return current.getCpuUsage(prev) / cpuCount;
            } finally {
                prev = current;
            }
        } else {
            prev = processWatcher.getCpuTimes();
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
        CpuTimeSnapshot prev = processWatcher.getCpuTimes();

        Thread resumeProcessHook = new Thread("Resume CpuWatcher Process") {
            @Override
            public void run() {
                processWatcher.resume();
            }
        };

        Runtime.getRuntime().addShutdownHook(resumeProcessHook);

        Float localUsageLimit;
        float cpuUsage;

        while (!isInterrupted()) {
            try {
                localUsageLimit = this.usageLimit;
                if (localUsageLimit == null) {
                    while (this.usageLimit == null) {
                        Thread.sleep(500);
                        prev = processWatcher.getCpuTimes();
                    }
                } else {
                    processWatcher.suspend();
                    long wakeupAmount = 0;
                    float error;
                    while ((localUsageLimit = this.usageLimit) != null) {
                        if (wakeupAmount > 0) {
                            processWatcher.resume();
                            sleepNanoseconds(wakeupAmount);
                            current = processWatcher.getCpuTimes();
                            processWatcher.suspend();
                            cpuUsage = current.getCpuUsage(prev) / cpuCount;
                            error = ((cpuUsage - localUsageLimit) / 100f) * ONE_SECOND_IN_NANOS;
                            wakeupAmount -= (long) error;
                            if (error > 0) {
                                if (wakeupAmount > 0) {
                                    wakeupAmount -= ONE_MILLIS_IN_NANOS;
                                }
                            } else if (wakeupAmount < 0) {
                                wakeupAmount += ONE_MILLIS_IN_NANOS;
                            }
                            if (wakeupAmount > WAKEUP_LIMITER_UP) {
                                wakeupAmount = WAKEUP_LIMITER_UP;
                            } else if (wakeupAmount < WAKEUP_LIMITER_DOWN) {
                                wakeupAmount = WAKEUP_LIMITER_DOWN;
                            }
                            prev = current;
                        } else {
                            wakeupAmount += ONE_MILLIS_IN_NANOS;
                            Thread.sleep(1);
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

    private static void sleepNanoseconds(long nanos) throws InterruptedException {
        if (nanos <= 0) {
            return;
        }
        long millisPart = 0;
        if (nanos > 999999) {
            millisPart = nanos / ONE_MILLIS_IN_NANOS;
            nanos = nanos - (millisPart * ONE_MILLIS_IN_NANOS);
        }
        Thread.sleep(millisPart, (int) nanos);
    }

    public static float getOneCoreOnePercent() {
        try {
            return 1f / SigarUtil.getCpuCount();
        } catch (SigarException t) {
            throw new RuntimeException("Error while getting CPU count.", t);
        }
    }

    @SuppressWarnings("SleepWhileInLoop")
    public static void main(String[] args) throws InterruptedException {
        if (args == null || args.length == 0) {
            System.out.println("Usage: sudo java -jar cpu-watcher.jar PID [CPU_MAX_USAGE_PERCENTAGE]");
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
