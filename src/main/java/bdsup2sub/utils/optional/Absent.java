/*
 * Copyright (C) 2011 The Guava Authors, Miklos Juhasz (mjuhasz)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bdsup2sub.utils.optional;

final class Absent extends Optional<Object> {
    private static final long serialVersionUID = 0;

    static final Absent INSTANCE = new Absent();

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public Object get() {
        throw new IllegalStateException("value is absent");
    }

    @Override
    public Object orNull() {
        return null;
    }

    @Override
    public boolean equals(Object object) {
        return object == this;
    }

    @Override
    public int hashCode() {
        return 0x598df91c;
    }

    @Override public String toString() {
        return "Optional.absent()";
    }

    private Object readResolve() {
        return INSTANCE;
    }
}
