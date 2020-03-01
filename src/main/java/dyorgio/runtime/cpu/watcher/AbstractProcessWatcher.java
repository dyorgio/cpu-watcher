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

import org.hyperic.sigar.ProcCpu;

/**
 *
 * @author dyorgio
 */
public abstract class AbstractProcessWatcher {

    protected final long pid;
    protected boolean resumed = true;

    protected AbstractProcessWatcher(final long pid) {
        this.pid = pid;
    }

    public final boolean isResumed() {
        return resumed;
    }

    public final boolean isSuspended() {
        return !resumed;
    }

    public final void suspend() {
        suspendImpl();
        resumed = false;
    }

    public final void resume() {
        resumeImpl();
        resumed = true;
    }

    public CpuTimeSnapshot getCpuTimes() {
        try {
            ProcCpu procCpu = SigarUtil.getSigar().getProcCpu(this.pid);
            return new CpuTimeSnapshot(procCpu.getTotal(), procCpu.getLastTime());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected abstract void suspendImpl();

    protected abstract void resumeImpl();
}
