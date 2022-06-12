/*
 * Copyright 2013 Miklos Juhasz (mjuhasz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bdsup2sub.core;

public enum PaletteMode {
    KEEP_EXISTING {
        @Override
        public String toString() {
            return "keep existing";
        }
    },
    CREATE_NEW {
        @Override
        public String toString() {
            return "create new";
        }
    },
    CREATE_DITHERED {
        @Override
        public String toString() {
            return "dithered";
        }
    },
}
