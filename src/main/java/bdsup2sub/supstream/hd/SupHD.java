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
package bdsup2sub.supstream.hd;

import bdsup2sub.bitmap.Bitmap;
import bdsup2sub.bitmap.Palette;
import bdsup2sub.core.Configuration;
import bdsup2sub.core.Core;
import bdsup2sub.core.CoreException;
import bdsup2sub.core.Logger;
import bdsup2sub.supstream.SubPicture;
import bdsup2sub.supstream.SubtitleStream;
import bdsup2sub.tools.BitStream;
import bdsup2sub.tools.FileBuffer;
import bdsup2sub.tools.FileBufferException;
import bdsup2sub.utils.ToolBox;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static bdsup2sub.utils.TimeUtils.ptsToTimeStr;

/**
 * Reading of HD-DVD captions demuxed from EVO transport streams (HD-DVD-SUP).
 */
public class SupHD implements SubtitleStream {

    private static final Configuration configuration = Configuration.getInstance();
    private static final Logger logger = Logger.getInstance();

    /** ArrayList of captions contained in the current file  */
    private ArrayList<SubPictureHD> subPictures = new ArrayList<SubPictureHD>();
    /** color palette of the last decoded caption  */
    private Palette palette;
    /** bitmap of the last decoded caption  */
    private Bitmap bitmap;
    /** FileBuffer to read from the file  */
    private FileBuffer buffer;
    /** index of dominant color for the current caption  */
    private int primaryColorIndex;


    public SupHD(String supFile) throws CoreException {
        try {
            buffer = new FileBuffer(supFile);
        } catch (FileBufferException ex) {
            throw new CoreException(ex.getMessage());
        }
        int bufsize = (int)buffer.getSize();

        SubPictureHD pic;
        int index = 0;
        try {
            while (index < bufsize) {
                if (Core.isCanceled()) {
                    throw new CoreException("Canceled by user!");
                }
                Core.setProgress(index);

                if (buffer.getWord(index) != 0x5350) {
                    throw new CoreException("ID 'SP' missing at index " + ToolBox.toHexLeftZeroPadded(index, 8) + "\n");
                }
                int masterIndex = index + 10; //end of header
                pic = new SubPictureHD();
                // hard code size since it's not part of the format???
                pic.setWidth(1920);
                pic.setHeight(1080);
                logger.info("#" + (subPictures.size() + 1) + "\n");
                pic.setStartTime(buffer.getDWordLE(index+=2)); // read PTS
                int packetSize = buffer.getDWord(index+=10);
                // offset to command buffer
                int ofsCmd = buffer.getDWord(index+=4) + masterIndex;
                pic.setImageBufferSize(ofsCmd - (index + 4));
                index  = ofsCmd;
                int dcsq = buffer.getWord(index);
                pic.setStartTime(pic.getStartTime() + (dcsq * 1024));
                logger.info("DCSQ start    ofs: " + ToolBox.toHexLeftZeroPadded(index, 8) + "  (" + ptsToTimeStr(pic.getStartTime()) + ")\n");
                index += 2; // 2 bytes: dcsq
                int nextIndex = buffer.getDWord(index) + masterIndex; // offset to next dcsq
                index += 5;  // 4 bytes: offset, 1 byte: start
                int cmd;
                boolean stopDisplay = false;
                boolean stopCommand = false;
                int alphaSum;
                int minAlphaSum = 256 * 256; // 256 fully transparent entries
                while(!stopDisplay) {
                    cmd = buffer.getByte(index++);
                    switch (cmd) {
                        case 0x01:
                            logger.info("DCSQ start    ofs: " + ToolBox.toHexLeftZeroPadded(index, 8) + "  (" + ptsToTimeStr(pic.getStartTime() + (dcsq * 1024)) + ")\n");
                            logger.warn("DCSQ start ignored due to missing DCSQ stop\n");
                            break;
                        case 0x02:
                            stopDisplay = true;
                            pic.setEndTime(pic.getStartTime() +(dcsq*1024));
                            logger.info("DCSQ stop     ofs: " + ToolBox.toHexLeftZeroPadded(index, 8) + "  (" + ptsToTimeStr(pic.getEndTime()) + ")\n");
                            break;
                        case 0x83: // palette
                            logger.trace("Palette info  ofs: " + ToolBox.toHexLeftZeroPadded(index, 8) + "\n");
                            pic.setPaletteOffset(index);
                            index += 0x300;
                            break;
                        case 0x84: // alpha
                            logger.trace("Alpha info    ofs: " + ToolBox.toHexLeftZeroPadded(index, 8) + "\n");
                            alphaSum = 0;
                            for (int i=index; i < index+0x100; i++) {
                                alphaSum += buffer.getByte(i);
                            }
                            if (alphaSum < minAlphaSum) {
                                pic.setAlphaOffset(index);
                                minAlphaSum = alphaSum;
                            } else {
                                logger.warn("Found faded alpha buffer -> alpha buffer skipped\n");
                            }

                            index += 0x100;
                            break;
                        case 0x85: // area
                            pic.setOfsX((buffer.getByte(index)<<4) | (buffer.getByte(index+1)>>4));
                            pic.setImageWidth((((buffer.getByte(index+1)&0xf)<<8) | (buffer.getByte(index+2))) - pic.getXOffset() + 1);
                            pic.setOfsY((buffer.getByte(index+3)<<4) | (buffer.getByte(index+4)>>4));
                            pic.setImageHeight((((buffer.getByte(index+4)&0xf)<<8) | (buffer.getByte(index+5))) - pic.getYOffset() + 1);
                            logger.trace("Area info     ofs: " + ToolBox.toHexLeftZeroPadded(index, 8) + "  ("
                                    + pic.getXOffset() + ", " + pic.getYOffset() + ") - (" + (pic.getXOffset() + pic.getImageWidth()) + ", "
                                    + (pic.getYOffset() + pic.getImageHeight()) + ")\n");
                            index += 6;
                            break;
                        case 0x86: // even/odd offsets
                            pic.setImageBufferOffsetEven(buffer.getDWord(index) + masterIndex);
                            pic.setImageBufferOffsetOdd(buffer.getDWord(index+4) + masterIndex);
                            logger.trace("RLE buffers   ofs: " + ToolBox.toHexLeftZeroPadded(index, 8)
                                    + "  (even: " + ToolBox.toHexLeftZeroPadded(pic.getImageBufferOffsetEven(), 8)
                                    + ", odd: " + ToolBox.toHexLeftZeroPadded(pic.getImageBufferOffsetOdd(), 8) + "\n");
                            index += 8;
                            break;
                        case 0xff:
                            if (stopCommand) {
                                logger.warn("DCSQ stop missing.\n");
                                for (++index; index < bufsize; index++)
                                    if (buffer.getByte(index++) != 0xff) {
                                        index--;
                                        break;
                                    }
                                stopDisplay = true;
                            } else {
                                index = nextIndex;
                                // add to display time
                                int d = buffer.getWord(index);
                                dcsq = d;
                                nextIndex = buffer.getDWord(index+2) + masterIndex;
                                stopCommand = (index == nextIndex);
                                logger.trace("DCSQ          ofs: " + ToolBox.toHexLeftZeroPadded(index, 8) + "  (" + (d * 1024 / 90)
                                        + "ms),    next DCSQ at ofs: " + ToolBox.toHexLeftZeroPadded(nextIndex, 8) + "\n");
                                index += 6;
                            }
                            break;
                        default:
                            throw new CoreException("Unexpected command " + cmd + " at index " + ToolBox.toHexLeftZeroPadded(index, 8));
                    }
                }
                index = masterIndex + packetSize;
                subPictures.add(pic);
            }
        } catch (CoreException ex) {
            if (subPictures.size() == 0) {
                throw ex;
            }
            logger.error(ex.getMessage() + "\n");
            logger.trace("Probably not all caption imported due to error.\n");
        } catch (FileBufferException ex) {
            if (subPictures.size() == 0) {
                throw new CoreException (ex.getMessage());
            }
            logger.error(ex.getMessage() + "\n");
            logger.trace("Probably not all caption imported due to error.\n");
        }
    }

    public void close() {
        if (buffer != null) {
            buffer.close();
        }
    }

    /**
     * decode one line from the RLE buffer
     * @param trg target buffer for uncompressed data
     * @param trgOfs offset in target buffer
     * @param width image width of encoded caption
     * @param maxPixels maximum number of pixels in caption
     * @param src source buffer
     */
    private static void decodeLine(byte[] trg, int trgOfs, int width, int maxPixels, BitStream src) {
        int x=0;
        int pixelsLeft;
        int sumPixels = 0;
        boolean lf = false;

        while (src.bitsLeft() > 0 && sumPixels<maxPixels) {
            int rleType = src.readBits(1);
            int colorType = src.readBits(1);
            int color;
            int numPixels;

            if (colorType == 1) {
                color = src.readBits(8);
            } else {
                color = src.readBits(2); // Colors between 0 and 3 are stored in two bits
            }

            if (rleType == 1) {
                int rleSize = src.readBits(1);
                if (rleSize == 1) {
                    numPixels = src.readBits(7) + 9;
                    if (numPixels == 9) {
                        numPixels = width - x;
                    }
                } else {
                    numPixels = src.readBits(3) + 2;
                }
            } else
                numPixels = 1;

            if (x+numPixels == width) {
                src.syncToByte();
                lf = true;
            }
            sumPixels += numPixels;

            // write pixels to target
            if (x+numPixels > width) {
                pixelsLeft = x + numPixels - width;
                numPixels = width - x;
                lf = true;
            } else {
                pixelsLeft = 0;
            }

            for (int i=0; i<numPixels; i++) {
                trg[trgOfs+x+i] = (byte)color;
            }

            if (lf) {
                trgOfs += x + numPixels + width; // skip odd/even line
                x = pixelsLeft;
                lf = false;
            } else {
                x += numPixels;
            }

            // copy remaining pixels to new line
            for (int i=0; i < pixelsLeft; i++) {
                trg[trgOfs+i] = (byte)color;
            }
        }
    }

    /**
     * decode caption from the input stream
     * @param pic SubPicture object containing info about the caption
     * @param transIdx index of the transparent color
     * @return bitmap of the decoded caption
     * @throws CoreException
     */
    private Bitmap decodeImage(SubPictureHD pic, int transIdx) throws CoreException {
        int w = pic.getImageWidth();
        int h = pic.getImageHeight();
        int warnings = 0;

        if (w > pic.getWidth() || h > pic.getHeight()) {
            throw new CoreException("Subpicture too large: " + w + "x" + h + " at offset " + ToolBox.toHexLeftZeroPadded(pic.getImageBufferOffsetEven(), 8));
        }

        Bitmap bm = new Bitmap(w, h, (byte)transIdx);

        int sizeEven = pic.getImageBufferOffsetOdd() - pic.getImageBufferOffsetEven();
        int sizeOdd = pic.getImageBufferSize() + pic.getImageBufferOffsetEven() - pic.getImageBufferOffsetOdd();

        if (sizeEven <= 0 || sizeOdd <= 0) {
            throw new CoreException("Corrupt buffer offset information");
        }

        byte evenBuf[] = new byte[sizeEven];
        byte oddBuf[]  = new byte[sizeOdd];

        try {
            // copy buffers
            try {
                for (int i=0; i < evenBuf.length; i++) {
                    evenBuf[i] = (byte)buffer.getByte(pic.getImageBufferOffsetEven() + i);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                warnings++;
            }
            try {
                for (int i=0; i < oddBuf.length; i++) {
                    oddBuf[i]  = (byte)buffer.getByte(pic.getImageBufferOffsetOdd() +i);
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                warnings++;
            }
            // decode even lines
            try {
                BitStream even = new BitStream(evenBuf);
                decodeLine(bm.getInternalBuffer(), 0, w, w*(h/2+(h&1)), even);
            } catch (ArrayIndexOutOfBoundsException ex) {
                warnings++;
            }
            // decode odd lines
            try {
                BitStream odd  = new BitStream(oddBuf);
                decodeLine(bm.getInternalBuffer(), w, w, (h/2)*w, odd);
            } catch (ArrayIndexOutOfBoundsException ex) {
                warnings++;
            }

            if (warnings > 0) {
                logger.warn("problems during RLE decoding of picture at offset " + ToolBox.toHexLeftZeroPadded(pic.getImageBufferOffsetEven(), 8) + "\n");
            }

            return bm;
        } catch (FileBufferException ex) {
            throw new CoreException (ex.getMessage());
        }
    }

    /**
     * decode palette from the input stream
     * @param pic SubPicture object containing info about the caption
     * @return decoded palette
     * @throws CoreException
     */
    private Palette decodePalette(final SubPictureHD pic) throws CoreException {
        int ofs = pic.getPaletteOffset();
        int alphaOfs = pic.getAlphaOffset();

        Palette palette = new Palette(256);
        try {
            for (int i=0; i < palette.getSize(); i++) {
                // each palette entry consists of 3 bytes
                int y = buffer.getByte(ofs++);
                int cr,cb;
                if (configuration.isSwapCrCb()) {
                    cb = buffer.getByte(ofs++);
                    cr = buffer.getByte(ofs++);
                } else {
                    cr = buffer.getByte(ofs++);
                    cb = buffer.getByte(ofs++);
                }
                // each alpha entry consists of 1 byte
                int alpha = 0xff - buffer.getByte(alphaOfs++);
                if (alpha < configuration.getAlphaCrop()) { // to not mess with scaling algorithms, make transparent color black
                    palette.setRGB(i, 0, 0, 0);
                } else {
                    palette.setYCbCr(i, y, cb, cr);
                }
                palette.setAlpha(i, alpha);
            }
            return palette;
        } catch (FileBufferException ex) {
            throw new CoreException (ex.getMessage());
        }
    }

    /**
     * decode given picture
     * @param pic SubPicture object containing info about caption
     * @throws CoreException
     */
    private void decode(SubPictureHD pic) throws CoreException {
        palette = decodePalette(pic);
        bitmap  = decodeImage(pic, palette.getIndexOfMostTransparentPaletteEntry());
        primaryColorIndex = bitmap.getPrimaryColorIndex(palette.getAlpha(), configuration.getAlphaThreshold(), palette.getY());
    }

    public void decode(int index) throws CoreException {
        if (index < subPictures.size()) {
            decode(subPictures.get(index));
        } else {
            throw new CoreException("Index " + index + " out of bounds\n");
        }
    }

    public Palette getPalette() {
        return palette;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public BufferedImage getImage() {
        return bitmap.getImage(palette.getColorModel());
    }

    public BufferedImage getImage(Bitmap bm) {
        return bm.getImage(palette.getColorModel());
    }

    public int getPrimaryColorIndex() {
        return primaryColorIndex;
    }

    public SubPicture getSubPicture(int index) {
        return subPictures.get(index);
    }

    public int getFrameCount() {
        return subPictures.size();
    }

    public int getForcedFrameCount() {
        return 0;
    }

    public boolean isForced(int index) {
        return false;
    }

    public long getEndTime(int index) {
        return subPictures.get(index).getEndTime();
    }

    public long getStartTime(int index) {
        return subPictures.get(index).getStartTime();
    }

    public long getStartOffset(int index) {
        return subPictures.get(index).getImageBufferOffsetEven();
    }
}
