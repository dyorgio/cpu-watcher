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

import dyorgio.runtime.out.process.CallableSerializable;
import dyorgio.runtime.out.process.OutProcessExecutorService;
import dyorgio.runtime.out.process.RunnableSerializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author dyorgio
 */
public class CpuWatcherTest {

    @Test
    public void testWatch() throws Throwable {
        OutProcessExecutorService sharedProcess = null;
        try {
            sharedProcess = new OutProcessExecutorService("-Xmx32m");
            Long jvmPID = sharedProcess.submit((CallableSerializable<Long>) () -> {
                try {
                    return getProcessPID();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).get();

            CpuWatcher cpuWatcher = new CpuWatcher(jvmPID, null);
            cpuWatcher.start();
            try {
                sharedProcess.submit((RunnableSerializable) () -> {
                    try {
                        long count = 0;
                        while (true) {
                            count = (long) Math.pow(count, count + 1);
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
                Thread.sleep(500);
                int count = 200;
                float[] usages = new float[count];
                float usagePercent = calculateUsagePercentage(count, usages, cpuWatcher, 95, 100);
                System.out.println("dyorgio.runtime.cpu.watcher.CpuWatcherTest.testWatch():" + usagePercent);
                Assert.assertThat("Cpu usage needs to be greater than 95%.", usagePercent, Matchers.greaterThan(95f));
            } finally {
                cpuWatcher.interrupt();
                cpuWatcher.join(3000);
                cpuWatcher.getProcessWatcher().resume();
            }
        } finally {
            if (sharedProcess != null) {
                sharedProcess.shutdown();
                sharedProcess.awaitTermination(3, TimeUnit.SECONDS);
                sharedProcess.shutdownNow();
            }
        }
    }

    @Test
    public void testLimit() throws Throwable {
        OutProcessExecutorService sharedProcess = null;
        try {
            sharedProcess = new OutProcessExecutorService("-Xmx32m");
            Long jvmPID = sharedProcess.submit((CallableSerializable<Long>) () -> {
                try {
                    return getProcessPID();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).get();

            CpuWatcher cpuWatcher = new CpuWatcher(jvmPID, 50f * CpuWatcher.getOneCoreOnePercent());
            cpuWatcher.start();
            try {
                sharedProcess.submit((RunnableSerializable) () -> {
                    try {
                        long count = 0;
                        while (true) {
                            count = (long) Math.pow(count, count + 1);
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
                Thread.sleep(500);
                int count = 200;
                float[] usages = new float[count];
                float usagePercent = calculateUsagePercentage(count, usages, cpuWatcher, 35, 55);
                System.out.println("dyorgio.runtime.cpu.watcher.CpuWatcherTest.testLimit(%):" + usagePercent);
                Assert.assertThat("Cpu usage needs to be less than 55%.", usagePercent, Matchers.lessThan(55f));
            } finally {
                cpuWatcher.interrupt();
                cpuWatcher.join(3000);
                cpuWatcher.getProcessWatcher().resume();
            }
        } finally {
            if (sharedProcess != null) {
                sharedProcess.shutdown();
                sharedProcess.awaitTermination(3, TimeUnit.SECONDS);
                sharedProcess.shutdownNow();
            }
        }
    }

    @Test
    public void testLimitAndUnlimit() throws Throwable {
        OutProcessExecutorService sharedProcess = null;
        try {
            sharedProcess = new OutProcessExecutorService("-Xmx32m");
            Long jvmPID = sharedProcess.submit((CallableSerializable<Long>) () -> {
                try {
                    return getProcessPID();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).get();

            CpuWatcher cpuWatcher = new CpuWatcher(jvmPID, 50f * CpuWatcher.getOneCoreOnePercent());
            cpuWatcher.start();
            try {
                sharedProcess.submit((RunnableSerializable) () -> {
                    try {
                        long count = 0;
                        while (true) {
                            count = (long) Math.pow(count, count + 1);
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                });
                Thread.sleep(500);
                int count = 200;
                float[] usages = new float[count];
                float usagePercent = calculateUsagePercentage(count, usages, cpuWatcher, 35, 55);
                System.out.println("dyorgio.runtime.cpu.watcher.CpuWatcherTest.testLimitAndUnlimit(1):" + usagePercent);
                Assert.assertThat("Cpu usage needs to be less than 55%.", usagePercent, Matchers.lessThan(55f));

                cpuWatcher.setUsageLimit(null);
                cpuWatcher.getProcessWatcher().resume();
                Thread.sleep(500);
                usagePercent = calculateUsagePercentage(count, usages, cpuWatcher, 95, 105);
                System.out.println("dyorgio.runtime.cpu.watcher.CpuWatcherTest.testLimitAndUnlimit(2):" + usagePercent);
                Assert.assertThat("Cpu usage needs to be greater than 95%.", usagePercent, Matchers.greaterThan(95f));

                cpuWatcher.setUsageLimit(25f * CpuWatcher.getOneCoreOnePercent());
                cpuWatcher.getProcessWatcher().resume();
                Thread.sleep(500);
                usagePercent = calculateUsagePercentage(count, usages, cpuWatcher, 15, 30);
                System.out.println("dyorgio.runtime.cpu.watcher.CpuWatcherTest.testLimitAndUnlimit(3):" + usagePercent);
                Assert.assertThat("Cpu usage needs to be less than 30%.", usagePercent, Matchers.lessThan(30f));
            } finally {
                cpuWatcher.interrupt();
                cpuWatcher.join(3000);
                cpuWatcher.getProcessWatcher().resume();
            }
        } finally {
            if (sharedProcess != null) {
                sharedProcess.shutdown();
                sharedProcess.awaitTermination(3, TimeUnit.SECONDS);
                sharedProcess.shutdownNow();
            }
        }
    }

    private static long getProcessPID() throws Exception {
        if (SigarUtil.getJavaVersion() < 9) {
            RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
            Field jvm = runtime.getClass().getDeclaredField("jvm");
            jvm.setAccessible(true);
            Object mgmtObj = jvm.get(runtime);
            Method pid_method = mgmtObj.getClass().getDeclaredMethod("getProcessId");
            pid_method.setAccessible(true);

            return ((Integer) pid_method.invoke(mgmtObj)).longValue();
        } else {
            Class processHandleClass = Class.forName("java.lang.ProcessHandle");
            Object currentProcessHandle = processHandleClass.getMethod("current").invoke(null);
            return (long) processHandleClass.getMethod("pid").invoke(currentProcessHandle);
        }
    }

    private static float calculateUsagePercentage(int count, float[] usages, CpuWatcher cpuWatcher, float expectedMinimum, float expectedMaximum) throws InterruptedException {
        float usagePercent = 0;
        for (int i = 1; i <= 10; i++) {
            if (i > 1) {
                System.out.println("dyorgio.runtime.cpu.watcher.CpuWatcherTest.calculateUsagePercentage(" + i + "): previous usage:" + usagePercent
                        + ", expectedMinimun:" + expectedMinimum + ", expectedMaximum:" + expectedMaximum);
            }
            usagePercent = calculateUsagePercentage(count, usages, cpuWatcher);
            if (usagePercent > expectedMinimum) {
                if (expectedMaximum < 0 || usagePercent < expectedMaximum) {
                    break;
                }
            }

        }
        return usagePercent;
    }

    private static float calculateUsagePercentage(int count, float[] usages, CpuWatcher cpuWatcher) throws InterruptedException {
        for (int i = 0; i < count; i++) {
            usages[i] = cpuWatcher.getCpuUsage();
            Thread.sleep(50);
        }
        return median(usages, 95) / CpuWatcher.getOneCoreOnePercent();
    }

    private static float median(float[] values, float percentile) {
        Arrays.sort(values);

        float[] valuesReduced;

        if (percentile < 100) {
            valuesReduced = new float[(int) (values.length * (percentile / 100f))];
            System.arraycopy(values, (values.length - valuesReduced.length) / 2, valuesReduced, 0, valuesReduced.length);
        } else {
            valuesReduced = values;
        }

        float total = 0;
        for (float value : valuesReduced) {
            total += value;
        }
        return total / valuesReduced.length;
    }
}
