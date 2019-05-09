/** *****************************************************************************
 * Copyright 2019 See AUTHORS file.
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
public class CpuTimeSnapshot {

    private final double total;
    private final double timestamp;

    public CpuTimeSnapshot(long total, long timestamp) {
        this.total = total;
        this.timestamp = timestamp;
    }

    public float getCpuUsage(CpuTimeSnapshot previous) {
        double delta = (timestamp - previous.timestamp);
        return delta == 0 ? 0 : (float) ((total - previous.total) / delta) * 100f;
    }

    @Override
    public String toString() {
        return "\ttotal:" + total + "\r\n"
                + "\ttimastamp:" + timestamp;
    }
}
