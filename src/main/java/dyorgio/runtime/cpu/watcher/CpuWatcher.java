/** *****************************************************************************
 * Copyright 2018 See AUTHORS file.
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

/**
 *
 * @author dyorgio
 */
public class CpuWatcher extends Thread {

    private static final long ONE_MILLIS_IN_NANOS = 1000000l;
    private static final long ONE_SECOND_IN_NANOS = 1000l * ONE_MILLIS_IN_NANOS;

    private final long pid;
    private final int cpuCount;
    private final float maxPercentage;
    private final AbstractProcessWatcher processWatcher;

    private float cpuUsage;

    public CpuWatcher(long pid, float maxPercentage) {
        try {
            if (pid == SigarUtil.getCurrentPid()) {
                throw new RuntimeException("You cannot use your own pid(" + pid + "), deadlock will occours.");
            }
            cpuCount = SigarUtil.getCpuCount();
        } catch (RuntimeException r) {
            throw r;
        } catch (Throwable t) {
            throw new RuntimeException("Error while getting current process PID or CPU count.", t);
        }
        this.pid = pid;
        this.maxPercentage = maxPercentage;
        this.processWatcher = AbstractProcessWatcherFactory.getInstance().createWatcher(pid);
    }

    public long getPid() {
        return pid;
    }

    public float getMaxPercentage() {
        return maxPercentage;
    }

    public float getCpuUsage() {
        return cpuUsage;
    }

    public AbstractProcessWatcher getProcessWatcher() {
        return processWatcher;
    }

    @Override
    public void run() {

        CpuTimeSnapshot current;
        processWatcher.suspend();
        CpuTimeSnapshot prev = processWatcher.getCpuTimes();
        long wakeupAmount = 0;
        float error;
        try {
            while (!isInterrupted()) {
                try {
                    if (wakeupAmount > 0) {
                        processWatcher.resume();
                        sleepNanoseconds(wakeupAmount);
                        current = processWatcher.getCpuTimes();
                        processWatcher.suspend();
                        cpuUsage = current.getCpuUsage(prev) / cpuCount;
                        error = ((cpuUsage - maxPercentage) / 100f) * ONE_SECOND_IN_NANOS;
                        wakeupAmount -= (long) error;
                        if (error > 0) {
                            if (wakeupAmount > 0) {
                                wakeupAmount -= ONE_MILLIS_IN_NANOS;
                            }
                        } else {
                            if (wakeupAmount < 0) {
                                wakeupAmount += ONE_MILLIS_IN_NANOS;
                            }
                        }
                        prev = current;
                    } else {
                        wakeupAmount += ONE_MILLIS_IN_NANOS;
                        Thread.sleep(1);
                    }
                } catch (InterruptedException ex) {
                    // ignore interruptions errors
                    break;
                }
            }
        } finally {
            try {
                processWatcher.resume();
            } catch (Exception ex) {
                //ignore
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
        } catch (Throwable t) {
            throw new RuntimeException("Error while getting CPU count.", t);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        if (args == null || args.length != 2) {
            System.out.println("Usage: sudo java -jar cpu-watcher.jar PID CPU_MAX_USAGE_PERCENTAGE");
            System.exit(-1);
        }

        CpuWatcher watcher = new CpuWatcher(Integer.parseInt(args[0]), Float.valueOf(args[1]));
        watcher.start();
    }
}
