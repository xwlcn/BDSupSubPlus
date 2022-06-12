package bdsup2sub.supstream.bd;

import bdsup2sub.core.Configuration;
import bdsup2sub.core.Core;
import bdsup2sub.core.CoreException;
import bdsup2sub.core.Logger;
import bdsup2sub.supstream.ImageObject;
import bdsup2sub.supstream.ImageObjectFragment;
import bdsup2sub.supstream.PCSObject;
import bdsup2sub.supstream.PaletteInfo;
import bdsup2sub.tools.FileBuffer;
import bdsup2sub.tools.FileBufferException;
import bdsup2sub.utils.ToolBox;

import java.util.ArrayList;
import java.util.List;

import static bdsup2sub.utils.TimeUtils.ptsToTimeStr;

public class SupBDParser {

    private static final Configuration configuration = Configuration.getInstance();
    private static final Logger logger = Logger.getInstance();

    private static final int PGSSUP_FILE_MAGIC = 0x5047;
    private static final int PGSSUP_PALETTE_SEGMENT = 0x14;
    private static final int PGSSUP_PICTURE_SEGMENT = 0x15;
    private static final int PGSSUP_PRESENTATION_SEGMENT = 0x16;
    private static final int PGSSUP_WINDOW_SEGMENT = 0x17;
    private static final int PGSSUP_DISPLAY_SEGMENT = 0x80;

    private static class PCSSegment {
        int type;
        int size;
        long pts;
        int offset; // file offset of segment
    }

    private FileBuffer buffer;
    private List<SubPictureBD> subPictures = new ArrayList<SubPictureBD>();
    private int forcedFrameCount;

    public SupBDParser(String filename) throws CoreException {
        try {
            buffer = new FileBuffer(filename);
        } catch (FileBufferException ex) {
            throw new CoreException(ex.getMessage());
        }
        parse();
    }

    private void parse() throws CoreException {
        int index = 0;
        long bufferSize = buffer.getSize();
        PCSSegment segment;
        SubPictureBD subPictureBD = null;
        int odsCounter = 0;
        int pdsCounter = 0;
        boolean paletteUpdate = false;
        PGSCompositionState compositionState = PGSCompositionState.INVALID;

        try {
            while (index < bufferSize) {
                // for threaded version
                if (Core.isCanceled()) {
                    throw new CoreException("Canceled by user!");
                }
                Core.setProgress(index);
                segment = readPCSSegment(index);
                switch (segment.type) {
                    case PGSSUP_PALETTE_SEGMENT:
                        StringBuffer message = new StringBuffer("PDS offset: ").append(ToolBox.toHexLeftZeroPadded(index, 8)).append(", size: ").append(ToolBox.toHexLeftZeroPadded(segment.size, 4));
                            if (subPictureBD != null) {
                                StringBuffer result = new StringBuffer();
                                int paletteSize = parsePDS(segment, subPictureBD, result, paletteUpdate);
                                if (paletteSize >= 0) {
                                    logger.trace(message + ", " + result + "\n");
                                    if (paletteSize > 0) {
                                        pdsCounter++;
                                    }
                                } else {
                                    logger.trace(message + "\n");
                                    logger.warn(result + "\n");
                                }
                            } else {
                                logger.trace(message + "\n");
                                logger.warn("Missing PTS start -> ignored\n");
                            }
                        break;
                    case PGSSUP_PICTURE_SEGMENT:
                        message = new StringBuffer("ODS offset: ").append(ToolBox.toHexLeftZeroPadded(index, 8)).append(", size: ").append(ToolBox.toHexLeftZeroPadded(segment.size, 4));
                        if (!paletteUpdate) {
                            if (subPictureBD != null) {
                                StringBuffer result = new StringBuffer();
                                parseODS(segment, subPictureBD, result);
                            }
                        }
                        break;
                    case PGSSUP_PRESENTATION_SEGMENT:   //PCS
                        compositionState = getCompositionState(segment);
                        paletteUpdate = getPaletteUpdateFlag(segment);
                        if (subPictureBD != null) {
                            subPictures.add(subPictureBD);
                        }
                        switch (compositionState) {
                            case EPOCH_START:
                            case ACQU_POINT:
                                subPictureBD = new SubPictureBD();
                                subPictureBD.setStartTime(segment.pts);
                                StringBuffer result = new StringBuffer();
                                parsePCS(segment, subPictureBD, result);
                                break;
                            case NORMAL:
                                /*subPictureBD = new SubPictureBD(subPictures.get(subPictures.size() - 1));
                                subPictureBD.setStartTime(segment.pts);*/
                                break;
                        }
                        if (subPictures.size() > 0 && subPictures.get(subPictures.size() - 1).getEndTime() == 0) {
                            subPictures.get(subPictures.size() - 1).setEndTime(segment.pts);
                        }
                        break;
                    case PGSSUP_WINDOW_SEGMENT:
                        message = new StringBuffer("WDS offset: ").append(ToolBox.toHexLeftZeroPadded(index, 8)).append(", size: ").append(ToolBox.toHexLeftZeroPadded(segment.size, 4));
                        if (subPictureBD != null) {
                            parseWDS(segment, subPictureBD);
                            logger.trace(message + ", dim: " + subPictureBD.getWindowWidth() + "*" + subPictureBD.getWindowHeight() + "\n");
                        } else {
                            logger.trace(message + "\n");
                            logger.warn("Missing PTS start -> ignored\n");
                        }
                        break;
                    case PGSSUP_DISPLAY_SEGMENT:
                        logger.trace("END offset: " + ToolBox.toHexLeftZeroPadded(index, 8) + "\n");
                        if (subPictureBD != null) {
                            subPictures.add(subPictureBD);
                            subPictureBD = null;
                        }
                        break;
                    default:
                        logger.warn("<unknown> " + ToolBox.toHexLeftZeroPadded(segment.type, 2) + " ofs:" + ToolBox.toHexLeftZeroPadded(index, 8) + "\n");
                        break;
                }
                index += 13; // header size
                index += segment.size;
            }
        } catch (CoreException ex) {
            if (subPictures.size() == 0) {
                throw ex;
            }
            logger.error(ex.getMessage() + "\n");
            logger.trace("Probably not all caption imported due to error.\n");
        } catch (FileBufferException ex) {
            if (subPictures.size() == 0) {
                throw new CoreException(ex.getMessage());
            }
            logger.error(ex.getMessage() + "\n");
            logger.trace("Probably not all caption imported due to error.\n");
        }

        removeLastFrameIfInvalid(odsCounter, pdsCounter);
        Core.setProgress(bufferSize);
        countForcedFrames();

    }

    private void removeLastFrameIfInvalid(int odsCounter, int pdsCounter) {
        if (subPictures.size() > 0 && (odsCounter == 0 || pdsCounter == 0)) {
            logger.warn("Missing PDS/ODS: last epoch is discarded\n");
            subPictures.remove(subPictures.size() - 1);
        }
    }

    private void countForcedFrames() {
        forcedFrameCount = 0;
        for (SubPictureBD p : subPictures) {
            if (p.isForced()) {
                forcedFrameCount++;
            }
        }
        logger.info("\nDetected " + forcedFrameCount + " forced captions.\n");
    }

    private PCSSegment readPCSSegment(int offset) throws FileBufferException, CoreException {
        PCSSegment pcsSegment = new PCSSegment();
        if (buffer.getWord(offset) != PGSSUP_FILE_MAGIC) {
            throw new CoreException("PG missing at index " + ToolBox.toHexLeftZeroPadded(offset, 8) + "\n");
        }
        pcsSegment.pts = buffer.getDWord(offset += 2);
        offset += 4; /* ignore DTS */
        pcsSegment.type = buffer.getByte(offset += 4);
        pcsSegment.size = buffer.getWord(offset += 1);
        pcsSegment.offset = offset + 2;
        return pcsSegment;
    }

    private int getCompositionNumber(PCSSegment segment) throws FileBufferException {
        return buffer.getWord(segment.offset + 5);
    }

    private PGSCompositionState getCompositionState(PCSSegment segment) throws FileBufferException {
        int type = buffer.getByte(segment.offset + 7);
        for (PGSCompositionState state : PGSCompositionState.values()) {
            if (type == state.getType()) {
                return state;
            }
        }
        return PGSCompositionState.INVALID;
    }

    /**
     * Retrieve palette (only) update flag from PCS segment
     * @return true: this is only a palette update - ignore ODS
     */
    private boolean getPaletteUpdateFlag(PCSSegment segment) throws FileBufferException {
        return buffer.getByte(segment.offset + 8) == 0x80;
    }

    /**
     * parse an PCS packet which contains width/height info
     *
     * @param segment object containing info about the current segment
     * @param subPictureBD SubPicture object containing info about the current caption
     * @param message
     * @throws FileBufferException
     */
    private void parsePCS(PCSSegment segment, SubPictureBD subPictureBD, StringBuffer message) throws FileBufferException {
        int index = segment.offset;
        if (segment.size >= 4) {
            subPictureBD.setWidth(buffer.getWord(index));               // video_width
            subPictureBD.setHeight(buffer.getWord(index + 2));          // video_height
            int type = buffer.getByte(index + 4);                       // hi nibble: frame_rate, lo nibble: reserved
            int compositionNumber = buffer.getWord(index + 5);         // composition_number
            // skipped:
            // 8bit  composition_state: 0x00: normal, 0x40: acquisition point, 0x80: epoch start,  0xC0: epoch continue, 6bit reserved
            // 8bit  palette_update_flag (0x80), 7bit reserved
            int paletteId = buffer.getByte(index + 9);                  // 8bit  palette_id_ref
            int compositionObjectCount = buffer.getByte(index + 10);    // 8bit  number_of_composition_objects (0..2)
            if (compositionObjectCount > 0) {
                // composition_object:
                int objectId = buffer.getWord(index + 11); // 16bit object_id_ref
                message.append("paletteId: ").append(paletteId).append(", objectId: ").append(objectId);
                ImageObject imageObject;
                if (!subPictureBD.getImageObjectMap().containsKey(objectId)) {
                    imageObject = new ImageObject();
                    imageObject.setObjectId(objectId);
                    subPictureBD.getImageObjectMap().put(objectId, imageObject);
                } else {
                    imageObject = subPictureBD.getImageObject(objectId);
                }
                imageObject.setPaletteID(paletteId);
                subPictureBD.setObjectID(objectId);

                // skipped:  8bit  window_id_ref
                if (segment.size >= 0x13) {
                    subPictureBD.setType(type);
                    int forcedCropped = buffer.getByte(index + 14);
                    subPictureBD.setCompositionNumber(compositionNumber);
                    subPictureBD.setForced(((forcedCropped & 0x40) == 0x40));
                    int offset = 0;
                    for (int i = 0; i < compositionObjectCount; i++) {
                        PCSObject po = new PCSObject();
                        po.setObjectId(buffer.getWord(index + 11 + offset));
                        po.setWindowId(buffer.getWord(index + 12 + offset));
                        // object_cropped_flag: 0x80, forced_on_flag = 0x040, 6bit reserved
                        forcedCropped = buffer.getByte(index + 14 + offset);
                        po.setForcedCropped(forcedCropped);
                        po.setxOffset(buffer.getWord(index + 15 + offset));
                        po.setyOffset(buffer.getWord(index + 17 + offset));
                        subPictureBD.getPcsObjectMap().put(po.getObjectId(), po);
                        offset += 8;
                    }
                    // if (object_cropped_flag==1)
                    //      16bit object_cropping_horizontal_position
                    //      16bit object_cropping_vertical_position
                    //      16bit object_cropping_width
                    //      object_cropping_height
                }
            }
        }
    }

    private void parseWDS(PCSSegment pcsSegment, SubPictureBD subPictureBD) throws FileBufferException {
        int index = pcsSegment.offset;
        if (pcsSegment.size >= 10) {
            // skipped:
            // 8bit: number of windows (currently assumed 1, 0..2 is legal)
            // 8bit: window id (0..1)
            subPictureBD.setXWindowOffset(buffer.getWord(index + 2));    // window_horizontal_position
            subPictureBD.setYWindowOffset(buffer.getWord(index + 4));    // window_vertical_position
            subPictureBD.setWindowWidth(buffer.getWord(index + 6));      // window_width
            subPictureBD.setWindowHeight(buffer.getWord(index + 8));     // window_height
        }
    }

    private boolean parseODS(PCSSegment pcsSegment, SubPictureBD subPictureBD, StringBuffer message) throws FileBufferException {
        int index = pcsSegment.offset;
        int objectId = buffer.getWord(index);                 // 16bit object_id
        int objectVersion = buffer.getByte(index+1);          // object_version_number
        int objectSequenceOrder = buffer.getByte(index+3);    // 8bit  first_in_sequence (0x80), last_in_sequence (0x40), 6bits reserved
        boolean first = (objectSequenceOrder & 0x80) == 0x80;
        boolean last = (objectSequenceOrder & 0x40) == 0x40;

        ImageObject imageObject;
        if (!subPictureBD.getImageObjectMap().containsKey(objectId)) {
            imageObject = new ImageObject();
            imageObject.setObjectId(objectId);
            subPictureBD.getImageObjectMap().put(objectId, imageObject);
        } else {
            imageObject = subPictureBD.getImageObject(objectId);
        }

        ImageObjectFragment imageObjectFragment;
        if (imageObject.getFragmentList().isEmpty() || first) {  // 8bit  object_version_number
            // skipped:
            // 24bit object_data_length - full RLE buffer length (including 4 bytes size info)
            int width  = buffer.getWord(index + 7);       // object_width
            int height = buffer.getWord(index + 9);       // object_height

            if (width <= subPictureBD.getWidth() && height <= subPictureBD.getHeight()) {
                imageObjectFragment = new ImageObjectFragment(index + 11, pcsSegment.size - (index + 11 - pcsSegment.offset));
                imageObject.getFragmentList().add(imageObjectFragment);
                imageObject.setBufferSize(imageObjectFragment.getImagePacketSize());
                imageObject.setHeight(height);
                imageObject.setWidth(width);
                imageObject.setXOffset(subPictureBD.getPcsObjectMap().get(objectId).getxOffset());
                imageObject.setYOffset(subPictureBD.getPcsObjectMap().get(objectId).getyOffset());
                message.append("ID: ").append(objectId).append(", update: ").append(objectVersion).append(", seq: ").append((first ? "first" : "")).append(((first && last) ? "/" : "")).append((last ? "" + "last" : ""));
                return true;
            } else {
                logger.warn("Invalid image size - ignored\n");
                return false;
            }
        } else {
            // object_data_fragment
            // skipped:
            //  16bit object_id
            //  8bit  object_version_number
            //  8bit  first_in_sequence (0x80), last_in_sequence (0x40), 6bits reserved
            imageObjectFragment = new ImageObjectFragment(index + 4, pcsSegment.size - (index + 4 - pcsSegment.offset));
            imageObject.getFragmentList().add(imageObjectFragment);
            imageObject.setBufferSize(imageObject.getBufferSize() + imageObjectFragment.getImagePacketSize());
            message.append("ID: ").append(objectId).append(", update: ").append(objectVersion).append(", seq: ").append((first ? "first" : "")).append(((first && last) ? "/" : "")).append((last ? "" + "last" : ""));
            return false;
        }
    }

    private int parsePDS(PCSSegment pcsSegment, SubPictureBD subPictureBD, StringBuffer message, boolean paletteUpdate) throws FileBufferException {
        int index = pcsSegment.offset;
        int paletteID = buffer.getByte(index);  // 8bit palette ID (0..7)
        // 8bit palette version number (incremented for each palette change)
        int version = buffer.getByte(index + 1);
        if (paletteID > 7) {
            message.append("Illegal palette id at offset ").append(ToolBox.toHexLeftZeroPadded(index, 8));
            return -1;
        }

        if (paletteUpdate) {
            subPictureBD.getPalettes().get(paletteID).remove(subPictureBD.getPalettes().size() - 1);
        }

        PaletteInfo paletteInfo = new PaletteInfo(paletteID, index + 2, (pcsSegment.size - 2) / 5);
        subPictureBD.getPalettes().get(paletteID).add(paletteInfo);
        message.append("ID: ").append(paletteID).append(", update: ").append(version).append(", ").append(paletteInfo.getPaletteSize()).append(" entries");
        return paletteInfo.getPaletteSize();
    }

    public FileBuffer getBuffer() {
        return buffer;
    }

    public List<SubPictureBD> getSubPictures() {
        return subPictures;
    }

    public int getForcedFrameCount() {
        return forcedFrameCount;
    }
}
