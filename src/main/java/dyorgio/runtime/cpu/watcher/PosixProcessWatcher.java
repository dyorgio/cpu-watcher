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

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 *
 * @author dyorgio
 */
public class PosixProcessWatcher extends AbstractProcessWatcher {

    private static final int SIGSTOP = 19;
    private static final int SIGCONT = 18;
    
    private static final int SIGSTOP_MAC = 17;
    private static final int SIGCONT_MAC = 19;
    
    private final int sigstop;
    private final int sigcont;

    public PosixProcessWatcher(int pid, boolean mac) {
        super(pid);
        if (mac) {
            sigstop = SIGSTOP_MAC;
            sigcont = SIGCONT_MAC;
        } else {
            sigstop = SIGSTOP;
            sigcont = SIGCONT;
        }
    }

    @Override
    protected void suspendImpl() {
        CLibrary.INSTANCE.kill((int) pid, sigstop);
    }

    @Override
    protected void resumeImpl() {
        CLibrary.INSTANCE.kill((int) pid, sigcont);
    }

    interface CLibrary extends Library {

        CLibrary INSTANCE = (CLibrary) Native.load("c", CLibrary.class);

        int getpid();

        void kill(int pid, int signal);
    }
}
