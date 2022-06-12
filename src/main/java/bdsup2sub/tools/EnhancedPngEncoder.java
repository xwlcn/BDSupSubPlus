/*
 * Copyright 2013 J. David Eisenberg / 0xdeadbeef / Miklos Juhasz (mjuhasz)
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
package bdsup2sub.tools;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * @author J. David Eisenberg - modified by 0xdeadbeef (added tRNS tag for transparency in palette mode)
 * @version 1.5, 19 Oct 2003 (modified by 0xdeadbeef in 2009 and mjuhasz in 2012)
 *
 * CHANGES:<br>
 * --------<br>
 * 19-Sep-2003 : Fix for platforms using EBCDIC (contributed by Paulo Soares);<br>
 * 19-Oct-2003 : Change private fields to protected fields so that<br>
 *               PngEncoderB can inherit them (JDE)<br>
 *               Fixed bug with calculation of nRows<br>
 *               Added modifications for unsigned short images<br>
 *                   (contributed by Christian at xpogen.com) <br>
 * 10-Nov-2012 : Removed unused constants and useless method override. (mjuhasz)
 */
public class EnhancedPngEncoder extends PngEncoder {

    /** PLTE tag. */
    private static final byte PLTE[] = { 80, 76, 84, 69 };

    /** tRNS tag. */
    private static final byte TRNS[] = {0x74, 0x52, 0x4e, 0x53 };

    private BufferedImage image;
    private WritableRaster wRaster;
    private int tType;

    public EnhancedPngEncoder() {
        this(null, false, FILTER_NONE, 0);
    }

    /**
     * Class constructor specifying BufferedImage to encode, with no alpha channel encoding.
     *
     * @param image A Java BufferedImage object
     */
    public EnhancedPngEncoder(BufferedImage image) {
        this(image, false, FILTER_NONE, 0);
    }

    /**
     * Class constructor specifying BufferedImage to encode, and whether to encode alpha.
     *
     * @param image A Java BufferedImage object
     * @param encodeAlpha Encode the alpha channel? false=no; true=yes
     */
    public EnhancedPngEncoder(BufferedImage image, boolean encodeAlpha) {
        this(image, encodeAlpha, FILTER_NONE, 0);
    }

    /**
     * Class constructor specifying BufferedImage to encode, whether to encode alpha, and filter to use.
     *
     * @param image A Java BufferedImage object
     * @param encodeAlpha Encode the alpha channel? false=no; true=yes
     * @param whichFilter 0=none, 1=sub, 2=up
     */
    public EnhancedPngEncoder(BufferedImage image, boolean encodeAlpha, int whichFilter) {
        this(image, encodeAlpha, whichFilter, 0);
    }

    /**
     * Class constructor specifying BufferedImage source to encode, whether to encode alpha,
     * filter to use, and compression level.
     *
     * @param image A Java BufferedImage object
     * @param encodeAlpha Encode the alpha channel? false=no; true=yes
     * @param whichFilter 0=none, 1=sub, 2=up
     * @param compLevel 0..9
     */
    private EnhancedPngEncoder(BufferedImage image, boolean encodeAlpha, int whichFilter, int compLevel) {
        this.image = image;
        this.encodeAlpha = encodeAlpha;
        setFilter(whichFilter);
        if (compLevel >=0 && compLevel <= 9) {
            this.compressionLevel = compLevel;
        }
    }

    /**
     * Set the BufferedImage to be encoded.
     * @param image A Java BufferedImage object
     */
    public void setImage(BufferedImage image) {
        this.image = image;
        pngBytes = null;
    }

    /**
     * Creates an array of bytes that is the PNG equivalent of the current image.
     *
     * @return an array of bytes, or null if there was a problem
     */
    @Override
    public byte[] pngEncode() {
        byte[] pngIdBytes = { -119, 80, 78, 71, 13, 10, 26, 10 };

        if (image == null) {
            System.err.println("pngEncode: image is null; returning null");
            return null;
        }
        width = image.getWidth(null);
        height = image.getHeight(null);

        if (!establishStorageInfo()) {
            System.err.println("pngEncode: cannot establish storage info");
            return null;
        }

        /* start with an array that is big enough to hold all the pixels
         * (plus filter bytes), and an extra 200 bytes for header info
         */
        pngBytes = new byte[((width+1) * height * 3) + 200];

        /* keep track of largest byte written to the array */
        maxPos = 0;

        bytePos = writeBytes(pngIdBytes, 0);
        // hdrPos = bytePos;
        writeHeader();
        // dataPos = bytePos;
        if (writeImageData()) {
            writeEnd();
            pngBytes = resizeByteArray(pngBytes, maxPos);
        } else {
            System.err.println("pngEncode: writeImageData failed => null");
            pngBytes = null;
        }
        return pngBytes;
    }

    /**
     * Get and set variables that determine how picture is stored.
     *
     * Retrieves the writable raster of the buffered image,
     * as well its transfer type.
     *
     * Sets number of output bytes per pixel, and, if only
     * eight-bit bytes, turns off alpha encoding.
     * @return true if 1-byte or 4-byte data, false otherwise
     */
    boolean establishStorageInfo() {
        int dataBytes;

        wRaster = image.getRaster();
        dataBytes = wRaster.getNumDataElements();
        tType = wRaster.getTransferType();

        if (((tType == DataBuffer.TYPE_BYTE) && (dataBytes == 4)) ||
            ((tType == DataBuffer.TYPE_INT) && (dataBytes == 1)) ||
            // on Win 2k/ME, tType == 1, dataBytes == 1
            ((tType == DataBuffer.TYPE_USHORT) && (dataBytes == 1))) {
            bytesPerPixel = (encodeAlpha) ? 4 : 3;
        } else if ((tType == DataBuffer.TYPE_BYTE) && (dataBytes == 1)) {
            bytesPerPixel = 1;
            encodeAlpha = false;    // one-byte samples
        } else {
            System.err.println("PNG encoder cannot establish storage info:");
            System.err.println("  TransferType == " + tType);
            System.err.println("  NumDataElements == " + dataBytes);
            return false;
        }
        return true;
    }

    /**
     * Write a PNG "IHDR" chunk into the pngBytes array.
     */
    @Override
    protected void writeHeader() {
        int startPos;

        startPos = bytePos = writeInt4(13, bytePos);
        bytePos = writeBytes(IHDR, bytePos);
        width = image.getWidth(null);
        height = image.getHeight(null);
        bytePos = writeInt4(width, bytePos);
        bytePos = writeInt4(height, bytePos);
        bytePos = writeByte(8, bytePos); // bit depth
        if (bytesPerPixel != 1) {
            bytePos = writeByte((encodeAlpha) ? 6 : 2, bytePos); // direct model
        } else {
            bytePos = writeByte(3, bytePos); // indexed
        }
        bytePos = writeByte(0, bytePos); // compression method
        bytePos = writeByte(0, bytePos); // filter method
        bytePos = writeByte(0, bytePos); // no interlace
        crc.reset();
        crc.update(pngBytes, startPos, bytePos-startPos);
        crcValue = crc.getValue();
        bytePos = writeInt4((int) crcValue, bytePos);
    }

    void writePalette(IndexColorModel icm) {
        byte[] redPal = new byte[256];
        byte[] greenPal = new byte[256];
        byte[] bluePal = new byte[256];
        byte[] allPal = new byte[768];
        int i;

        icm.getReds(redPal);
        icm.getGreens(greenPal);
        icm.getBlues(bluePal);
        for (i=0; i<256; i++) {
            allPal[i*3  ] = redPal[i];
            allPal[i*3+1] = greenPal[i];
            allPal[i*3+2] = bluePal[i];
        }
        bytePos = writeInt4(768, bytePos);
        bytePos = writeBytes(PLTE, bytePos);
        crc.reset();
        crc.update(PLTE);
        bytePos = writeBytes(allPal, bytePos);
        crc.update(allPal);
        crcValue = crc.getValue();
        bytePos = writeInt4((int) crcValue, bytePos);
    }

    void writeAlpha(IndexColorModel icm) {
        byte[] alpha = new byte[256];
        icm.getAlphas(alpha);
        bytePos = writeInt4(256, bytePos);
        bytePos = writeBytes(TRNS, bytePos);
        crc.reset();
        crc.update(TRNS);
        bytePos = writeBytes(alpha, bytePos);
        crc.update(alpha);
        crcValue = crc.getValue();
        bytePos = writeInt4((int) crcValue, bytePos);
    }

    /**
     * Write the image data into the pngBytes array.
     * This will write one or more PNG "IDAT" chunks. In order
     * to conserve memory, this method grabs as many rows as will
     * fit into 32K bytes, or the whole image; whichever is less.
     *
     *
     * @return true if no errors; false if error grabbing pixels
     */
    @Override
    protected boolean writeImageData() {
        int rowsLeft = height;  // number of rows remaining to write
        int startRow = 0;       // starting row to process this time through
        int nRows;              // how many rows to grab at a time

        byte[] scanLines;       // the scan lines to be compressed
        int scanPos;            // where we are in the scan lines
        int startPos;           // where this line's actual pixels start (used for filtering)
        int readPos;            // position from which source pixels are read

        byte[] compressedLines; // the resultant compressed lines
        int nCompressed;        // how big is the compressed area?

        byte[] pixels;          // storage area for byte-sized pixels
        int[] iPixels;          // storage area for int-sized pixels
        short[] sPixels;		// for Win 2000/ME ushort pixels
        final int type = image.getType();
        // TYPE_INT_RGB        = 1
        // TYPE_INT_ARGB       = 2
        // TYPE_INT_ARGB_PRE   = 3
        // TYPE_INT_BGR        = 4
        // TYPE_3BYTE_BGR      = 5
        // TYPE_4BYTE_ABGR     = 6
        // TYPE_4BYTE_ABGR_PRE = 7
        // TYPE_BYTE_GRAY      = 10
        // TYPE_BYTE_BINARY    = 12
        // TYPE_BYTE_INDEXED   = 13
        // TYPE_USHORT_GRAY    = 11
        // TYPE_USHORT_565_RGB = 8
        // TYPE_USHORT_555_RGB = 9
        // TYPE_CUSTOM         = 0.

        Deflater scrunch = new Deflater(compressionLevel);
        ByteArrayOutputStream outBytes =
            new ByteArrayOutputStream(1024);

        DeflaterOutputStream compBytes = new DeflaterOutputStream(outBytes, scrunch);

        if (bytesPerPixel == 1) {
            writePalette((IndexColorModel) image.getColorModel());
            writeAlpha((IndexColorModel) image.getColorModel());
        }

        try {
            while (rowsLeft > 0) {
                nRows = Math.min(32767 / (width*(bytesPerPixel+1)), rowsLeft);
                nRows = Math.max(nRows, 1);

                /*
                 * Create a data chunk. scanLines adds "nRows" for
                 * the filter bytes.
                 */
                scanLines = new byte[width * nRows * bytesPerPixel +  nRows];

                if (filter == FILTER_SUB) {
                    leftBytes = new byte[16];
                }
                if (filter == FILTER_UP) {
                    priorRow = new byte[width*bytesPerPixel];
                }

                final Object data =
                    wRaster.getDataElements(0, startRow, width, nRows, null);

                pixels = null;
                iPixels = null;
                sPixels = null;
                if (tType == DataBuffer.TYPE_BYTE) {
                    pixels = (byte[]) data;
                } else if (tType == DataBuffer.TYPE_INT) {
                    iPixels = (int[]) data;
                } else if (tType == DataBuffer.TYPE_USHORT) {
                    sPixels = (short[]) data;
                }

                scanPos = 0;
                readPos = 0;
                startPos = 1;
                for (int i=0; i<width*nRows; i++) {
                    if (i % width == 0) {
                        scanLines[scanPos++] = (byte) filter;
                        startPos = scanPos;
                    }

                    if (bytesPerPixel == 1) { // assume TYPE_BYTE, indexed
                        scanLines[scanPos++] = pixels[readPos++];
                    } else if (tType == DataBuffer.TYPE_BYTE) {
                        scanLines[scanPos++] = pixels[readPos++];
                        scanLines[scanPos++] = pixels[readPos++];
                        scanLines[scanPos++] = pixels[readPos++];
                        if (encodeAlpha) {
                            scanLines[scanPos++] = pixels[readPos++];
                        } else {
                            readPos++;
                        }
                    } else if (tType == DataBuffer.TYPE_USHORT) {
                        short pxl = sPixels[readPos++];
                        if (type == BufferedImage.TYPE_USHORT_565_RGB) {
                            scanLines[scanPos++] = (byte) ((pxl >> 8) & 0xf8);
                            scanLines[scanPos++] = (byte) ((pxl >> 2) & 0xfc);
                        } else {                // assume USHORT_555_RGB
                            scanLines[scanPos++] = (byte) ((pxl >> 7) & 0xf8);
                            scanLines[scanPos++] = (byte) ((pxl >> 2) & 0xf8);
                        }
                        scanLines[scanPos++] = (byte) ((pxl << 3) & 0xf8);
                    } else {      // assume tType INT and type RGB or ARGB
                        int pxl = iPixels[readPos++];
                        scanLines[scanPos++] = (byte) ((pxl >> 16) & 0xff);
                        scanLines[scanPos++] = (byte) ((pxl >>  8) & 0xff);
                        scanLines[scanPos++] = (byte) ((pxl      ) & 0xff);
                        if (encodeAlpha) {
                            scanLines[scanPos++] = (byte) ((pxl >> 24) & 0xff);
                        }
                    }

                    if ((i % width == width-1) && (filter != FILTER_NONE)) {
                        if (filter == FILTER_SUB) {
                            filterSub(scanLines, startPos, width);
                        }
                        if (filter == FILTER_UP) {
                            filterUp(scanLines, startPos, width);
                        }
                    }
                }

                /*
                 * Write these lines to the output area
                 */
                compBytes.write(scanLines, 0, scanPos);

                startRow += nRows;
                rowsLeft -= nRows;
            }
            compBytes.close();

            /*
             * Write the compressed bytes
             */
            compressedLines = outBytes.toByteArray();
            nCompressed = compressedLines.length;

            crc.reset();
            bytePos = writeInt4(nCompressed, bytePos);
            bytePos = writeBytes(IDAT, bytePos);
            crc.update(IDAT);
            bytePos = writeBytes(compressedLines, nCompressed, bytePos);
            crc.update(compressedLines, 0, nCompressed);

            crcValue = crc.getValue();
            bytePos = writeInt4((int) crcValue, bytePos);
            scrunch.finish();
            return true;
        } catch (IOException e) {
            System.err.println(e.toString());
            return false;
        }
    }
}
