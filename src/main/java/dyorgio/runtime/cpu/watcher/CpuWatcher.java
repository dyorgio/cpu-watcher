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

import java.lang.instrument.Instrumentation;
import org.hyperic.sigar.SigarException;

/**
 *
 * @author dyorgio
 */
public class CpuWatcher extends Thread {

    private static final long MAX_SLEEP_AMOUNT = 100000;
    private static final long MIN_SLEEP_AMOUNT = 20000;

    private final long pid;
    private final float maxPercentage;
    private final AbstractProcessWatcher processWatcher;

    private long sleepAmount = MIN_SLEEP_AMOUNT;
    private float cpuUsage;

    public CpuWatcher(long pid, float maxPercentage) {
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
    
    @Override
    public void run() {
        CpuTimes current;
        processWatcher.resume();
        CpuTimes prev = processWatcher.getCpuTimes();
        float error;
        while (!isInterrupted()) {
            try {
                current = processWatcher.getCpuTimes();

                cpuUsage = current.getCpuUsage(prev) / SigarUtil.getCpuCount();

                error = (cpuUsage - maxPercentage) / 100f;

                if (sleepAmount == 0.0 && error > 0.0) {
                    sleepAmount = MIN_SLEEP_AMOUNT;
                }

                sleepAmount += (int) (error * MIN_SLEEP_AMOUNT);

                if (sleepAmount < MIN_SLEEP_AMOUNT) {
                    sleepAmount = 0;
                }

                if (sleepAmount > MAX_SLEEP_AMOUNT) {
                    sleepAmount = MAX_SLEEP_AMOUNT;
                }

                if (sleepAmount >= MIN_SLEEP_AMOUNT) {
                    processWatcher.suspend();
                    sleepNanoseconds(sleepAmount);
                    processWatcher.resume();
                }
                prev = current;
                sleepNanoseconds(MAX_SLEEP_AMOUNT - sleepAmount);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        processWatcher.resume();
    }

    private void sleepNanoseconds(long nanos) throws InterruptedException {
        if (nanos == 0) {
            return;
        }
        long millisPart = 0;
        int nanosPart;
        if (nanos > 999999) {
            millisPart = nanos / 1000000;
            nanosPart = (int) (nanos - (millisPart * 1000000));
        } else {
            nanosPart = (int) nanos;
        }
        Thread.sleep(millisPart, nanosPart);
    }

    public static void main(String[] args) throws InterruptedException {
        if (args == null || args.length != 2) {
            System.out.println("Usage: sudo java -jar cpu-watcher.jar PID CPU_MAX_USAGE_PERCENTAGE");
            System.exit(-1);
        }

        CpuWatcher watcher = new CpuWatcher(Integer.parseInt(args[0]), Float.valueOf(args[1]));
        watcher.start();
    }
    
    public static void premain(String agentArgs, Instrumentation inst) {
        try {
            CpuWatcher watcher = new CpuWatcher(SigarUtil.getCurrentPid(), Float.valueOf(agentArgs));
            watcher.start();
        } catch (SigarException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
