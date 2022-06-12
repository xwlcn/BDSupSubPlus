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

public enum StreamID {
    /** Blu-Ray SUP stream */
    BDSUP,
    /** HD-DVD SUP or DVD SUP stream (same ID) */
    SUP,
    /** DVD VobSub SUB stream*/
    DVDSUB,
    /** Sony BDN XML */
    XML,
    /** DVD IFO */
    IFO,
    /** DVD VobSub IDX */
    IDX,
    /** UNKNOWN */
    UNKNOWN
}
