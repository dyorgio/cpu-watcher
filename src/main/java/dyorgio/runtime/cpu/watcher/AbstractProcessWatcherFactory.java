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

import dyorgio.runtime.cpu.watcher.platform.LinuxProcessWatcherFactory;
import dyorgio.runtime.cpu.watcher.platform.MacProcessWatcherFactory;
import dyorgio.runtime.cpu.watcher.platform.WinProcessWatcherFactory;
import java.util.Locale;

/**
 *
 * @author dyorgio
 */
public abstract class AbstractProcessWatcherFactory {

    private static final AbstractProcessWatcherFactory INSTANCE;

    static {
        try {
            String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
            if ((OS.contains("mac")) || (OS.contains("darwin"))) {

                INSTANCE = new MacProcessWatcherFactory();

            } else if (OS.contains("win")) {
                INSTANCE = new WinProcessWatcherFactory();
            } else if (OS.contains("nux")) {
                INSTANCE = new LinuxProcessWatcherFactory();
            } else {
                throw new RuntimeException("Unsupported OS:" + OS);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public abstract AbstractProcessWatcher createWatcher(long pid);

    public static AbstractProcessWatcherFactory getInstance() {
        return INSTANCE;
    }
}
