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
 * This object represents a snapshot detailing the total time the CPUs have
 * spent idle, in user mode, and in kernel mode.
 *
 * @author dyorgio
 */
public class CpuTimes {

    private final long userMillis;
    private final long systemMillis;
    private final long idleMillis;

    public CpuTimes(long userMillis, long systemMillis, long idleMillis) {
        this.userMillis = userMillis;
        this.systemMillis = systemMillis;
        this.idleMillis = idleMillis;
    }

    /**
     * The total time in milliseconds that the CPUs have spent in user mode.
     *
     * @return The total time in milliseconds that the CPUs have spent in user
     * mode.
     */
    public long getUserMillis() {
        return userMillis;
    }

    /**
     * The total time in milliseconds that the CPUs have spent in kernel mode.
     *
     * @return The total time in milliseconds that the CPUs have spent in kernel
     * mode.
     */
    public long getSystemMillis() {
        return systemMillis;
    }

    /**
     * The total time in milliseconds that the CPUs have spent idle.
     *
     * @return The total time in milliseconds that the CPUs have spent idle.
     */
    public long getIdleMillis() {
        return idleMillis;
    }

    /**
     * The total time in milliseconds that the CPUs have been alive since the
     * system was last booted. Should equal the sum of the other three numbers.
     *
     * @return The total time in milliseconds that the CPUs have been alive.
     */
    public long getTotalMillis() {
        return userMillis + systemMillis + idleMillis;
    }

    /**
     * Gets the CPU usage given a previous snapshot of CPU times. The number
     * returned represents the proportion of time between the two snapshots that
     * the CPUs spent not idle.
     *
     * @param previous a CpuTimes snapshot taken previously.
     * @return the proportion of time between the previous snapshot and this
     * snapshot that the CPUs have spent working. 1 represents 100% usage, 0
     * represents 0% usage.
     */
    public float getCpuUsage(CpuTimes previous) {
        if (getIdleMillis() == previous.getIdleMillis()) {
            return 1f;
        }
        return 1 - ((float) (getIdleMillis() - previous.getIdleMillis()))
                / (float) (getTotalMillis() - previous.getTotalMillis());
    }

    @Override
    public String toString() {
        return "\tidle:" + idleMillis + "\r\n"
                + "\tsystem:" + systemMillis + "\r\n"
                + "\tuser:" + userMillis;
    }
}
