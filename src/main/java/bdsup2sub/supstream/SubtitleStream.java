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
package bdsup2sub.supstream;

import bdsup2sub.bitmap.Bitmap;
import bdsup2sub.bitmap.Palette;
import bdsup2sub.core.CoreException;

import java.awt.image.BufferedImage;

public interface SubtitleStream {

    /**
     * Return the Palette of the current (last decoded) frame.
     * @return Palette of the current frame
     */
    Palette getPalette();

    /**
     * Return the Bitmap of the current (last decoded) frame.
     * @return Bitmap of the current frame
     */
    Bitmap getBitmap();

    /**
     * Return current (last decoded) frame
     * @return Current (last decoded) frame
     */
    BufferedImage getImage();

    /**
     * Return given Bitmap as BufferedImage (using current Palette).
     * @param bitmap Bitmap to convert.
     * @return BufferedImage of given bitmap
     */
    BufferedImage getImage(Bitmap bitmap);

    /**
     * Get index of most dominant opaque color (for DVD subtitle export).
     * @return Index of most dominant opaque color
     */
    int getPrimaryColorIndex();

    /**
     * Decode caption.
     * @param index Index of caption
     * @throws CoreException
     */
    void decode(int index) throws CoreException;

    /**
     * Get number of frames in the currently loaded subtitle stream.
     * @return Number of frames
     */
    int getFrameCount();

    /**
     * Get number of forced frames in the currently loaded subtitle stream.
     * @return Number of forced frames
     */
    int getForcedFrameCount();

    /**
     * Get forced flag of given frame.
     * @param index Index of caption
     * @return Forced flag of given frame
     */
    boolean isForced(int index);

    /**
     * Close input stream.
     */
    void close();

    /**
     * Get end time stamp of given frame.
     * @param index Index of caption
     * @return End time stamp of given frame in 90kHz resolution
     */
    long getEndTime(int index);

    /**
     * Get start time stamp of given frame.
     * @param index Index of caption
     * @return Start time stamp of given frame in 90kHz resolution
     */
    long getStartTime(int index);

    /**
     * Get start offset (in input stream) of given frame.
     * @param index Index of caption
     * @return Start offset of given frame in input stream
     */
    long getStartOffset(int index);

    /**
     * Get SubPicture of given frame.
     * @param index Index of caption
     * @return SubPicture of caption
     */
    SubPicture getSubPicture(int index);
}
