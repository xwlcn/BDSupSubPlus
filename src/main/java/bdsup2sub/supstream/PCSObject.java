package bdsup2sub.supstream;

public class PCSObject {

    private int objectId;
    private int windowId;
    private int forcedCropped;
    private int xOffset;
    private int yOffset;

    public PCSObject() {
    }

    public PCSObject(PCSObject other) {
        this.objectId = other.objectId;
        this.windowId = other.windowId;
        this.forcedCropped = other.forcedCropped;
        this.xOffset = other.xOffset;
        this.yOffset = other.yOffset;
    }

    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(int objectId) {
        this.objectId = objectId;
    }

    public int getWindowId() {
        return windowId;
    }

    public void setWindowId(int windowId) {
        this.windowId = windowId;
    }

    public int getForcedCropped() {
        return forcedCropped;
    }

    public void setForcedCropped(int forcedCropped) {
        this.forcedCropped = forcedCropped;
    }

    public int getxOffset() {
        return xOffset;
    }

    public void setxOffset(int xOffset) {
        this.xOffset = xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }

    public void setyOffset(int yOffset) {
        this.yOffset = yOffset;
    }
}
