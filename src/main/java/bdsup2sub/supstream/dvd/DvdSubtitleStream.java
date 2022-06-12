/*
 * Copyright 2013 Volker Oth (0xdeadbeef) / Miklos Juhasz (mjuhasz)
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
package bdsup2sub.supstream.dvd;

import bdsup2sub.bitmap.Palette;
import bdsup2sub.supstream.SubtitleStream;

/**
 * Interface fot SUB/IDX and SUP/IFO handling.
 */
public interface DvdSubtitleStream extends SubtitleStream {

    /**
     * Get frame alpha values.
     * @param index Index of caption
     * @return Integer array with 4 entries representing the frame alpha values
     */
    int[] getFrameAlpha(int index);

    /**
     * Get frame Palette.
     * @param index Index of caption
     * @return Integer array with 4 entries representing the frame palette
     */
    int[] getFramePalette(int index);

    /**
     * Get original frame alpha values (as they were before editing).
     * @param index Index of caption
     * @return Integer array with 4 entries representing the original frame alpha values.
     */
    int[] getOriginalFrameAlpha(int index);

    /**
     * Get original frame Palette (as they were before editing).
     * @param index index of caption
     * @return Integer array with 4 entries representing the original frame palette.
     */
    int[] getOriginalFramePalette(int index);

    /**
     * Get imported 16 color DVD Palette.
     * @return Imported 16 color DVD Palette.
     */
    Palette getSrcPalette();

    /**
     * Replace imported 16 color DVD Palette with a new Palette.
     * @param pal New Palette
     */
    void setSrcPalette(Palette pal);

    /**
     * Get language index read from Idx.
     * @return language index.
     */
    int getLanguageIndex();
}
