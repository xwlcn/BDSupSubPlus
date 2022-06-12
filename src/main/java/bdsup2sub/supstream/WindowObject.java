package bdsup2sub.supstream;

public class WindowObject {

    private int id;
    //相对于视频x偏移
    private int xOffset;
    //相对于视频y偏移
    private int yOffset;
    //image width
    private int width;
    //image height
    private int height;

    public WindowObject() {
    }

    public WindowObject(int id, int xOffset, int yOffset, int width, int height) {
        this.id = id;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
