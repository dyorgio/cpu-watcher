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
package dyorgio.runtime.cpu.watcher.platform;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.NtDll;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.W32APIOptions;
import dyorgio.runtime.cpu.watcher.AbstractProcessWatcher;
import dyorgio.runtime.cpu.watcher.AbstractProcessWatcherFactory;

/**
 *
 * @author dyorgio
 */
public class WinProcessWatcherFactory extends AbstractProcessWatcherFactory {

    @Override
    public AbstractProcessWatcher createWatcher(int pid) {
        return new WinProcessWatcher(pid);
    }

    @Override
    public int getCurrentPid() {
        return Kernel32.INSTANCE.GetCurrentProcessId();
    }

    private static final class WinProcessWatcher extends AbstractProcessWatcher {

        private volatile WinNT.HANDLE process = null;

        WinProcessWatcher(final int pid) {
            super(pid);
        }

        @Override
        protected void suspendImpl() {
            WinNT.HANDLE processLocal = this.process;
            if (processLocal == null) {
                synchronized (this) {
                    processLocal = this.process;
                    if (processLocal == null) {
                        this.process = processLocal = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_SUSPEND_RESUME, false, (int) pid);
                    }
                }
            }
            if (processLocal != null) {
                NtDllExt.INSTANCE.NtSuspendProcess(processLocal);
            }
        }

        @Override
        protected void resumeImpl() {
            WinNT.HANDLE processLocal = this.process;
            if (processLocal == null) {
                synchronized (this) {
                    processLocal = this.process;
                    if (processLocal == null) {
                        this.process = processLocal = Kernel32.INSTANCE.OpenProcess(WinNT.PROCESS_SUSPEND_RESUME, false, (int) pid);
                    }
                }
            }
            if (processLocal != null) {
                NtDllExt.INSTANCE.NtResumeProcess(process);
            }
        }

        @Override
        @SuppressWarnings("FinalizeDeclaration")
        protected void finalize() throws Throwable {
            if (process != null) {
                Kernel32.INSTANCE.CloseHandle(process);
            }
            super.finalize();
        }

        @Override
        public void freeResources() {
            WinNT.HANDLE processLocal = this.process;
            if (processLocal != null) {
                synchronized (this) {
                    processLocal = this.process;
                    if (processLocal != null) {
                        Kernel32.INSTANCE.CloseHandle(process);
                        this.process = null;
                    }
                }
            }
        }
    }

    private static interface NtDllExt extends NtDll {

        @SuppressWarnings("FieldNameHidesFieldInSuperclass")
        NtDllExt INSTANCE = Native.load("NtDll", NtDllExt.class, W32APIOptions.DEFAULT_OPTIONS);

        public int NtResumeProcess(HANDLE handle);

        public int NtSuspendProcess(HANDLE handle);
    }
}
