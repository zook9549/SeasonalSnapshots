package com.chubbybuttons.seasonalsnapshots;

import java.time.LocalDateTime;

public class Snapshot implements Comparable<Snapshot> {

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public LocalDateTime getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(LocalDateTime snapshotTime) {
        this.snapshotTime = snapshotTime;
    }

    public Phase getSnapshotPhase() {
        return snapshotPhase;
    }

    public void setSnapshotPhase(Phase snapshotPhase) {
        this.snapshotPhase = snapshotPhase;
    }

    public String getArchivePath() {
        return archivePath;
    }

    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    @Override
    public String toString() {
        return "Snapshot{" +
                "snapshotTime=" + snapshotTime +
                ", snapshotPhase=" + snapshotPhase +
                ", archivePath='" + archivePath + '\'' +
                ", imageName='" + imageName + '\'' +
                '}';
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    enum Phase {
        SUNRISE(1), SOLAR_NOON(2), SUNSET(3);

        private int val;

        Phase(int val) {
            this.val = val;
        }

        public int getValue() {
            return val;
        }
    }

    @Override
    public int compareTo(Snapshot o) {
        return imageName.compareTo(o.getImageName());
    }

    private byte[] image;
    private LocalDateTime snapshotTime;
    private Phase snapshotPhase;
    private String archivePath;
    private String imageName;
}
