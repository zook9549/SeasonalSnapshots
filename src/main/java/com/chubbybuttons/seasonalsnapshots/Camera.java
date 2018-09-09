package com.chubbybuttons.seasonalsnapshots;

public class Camera {

    public Camera(String name) {
        this.name = name;
    }

    public Camera(String name, String snapshotURL) {
        this.name = name;
        this.snapshotURL = snapshotURL;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSnapshotURL() {
        return snapshotURL;
    }

    public void setSnapshotURL(String snapshotURL) {
        this.snapshotURL = snapshotURL;
    }

    @Override
    public String toString() {
        return "Camera{" +
                "name='" + name + '\'' +
                ", snapshotURL='" + snapshotURL + '\'' +
                '}';
    }

    private String name;
    private String snapshotURL;
}
