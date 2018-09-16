package com.chubbybuttons.seasonalsnapshots;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class PhaseSchedule implements Comparable<PhaseSchedule>{

    public PhaseSchedule(Snapshot.Phase phase) {
        this.phase = phase;
    }

    public Snapshot.Phase getPhase() {
        return phase;
    }

    public void setPhase(Snapshot.Phase phase) {
        this.phase = phase;
    }

    public LocalDateTime getNextSnapshotDateTime() {
        return nextSnapshotDateTime;
    }

    public void setNextSnapshotDateTime(LocalDateTime nextSnapshotDateTime) {
        this.nextSnapshotDateTime = nextSnapshotDateTime;
    }

    public LocalDateTime getLastSnapshotDateTime() {
        return lastSnapshotDateTime;
    }

    public void setLastSnapshotDateTime(LocalDateTime lastSnaphotDateTime) {
        this.lastSnapshotDateTime = lastSnaphotDateTime;
    }

    @Override
    public int compareTo(PhaseSchedule o) {
        return Integer.valueOf(phase.getValue()).compareTo(o.phase.getValue());
    }

    private Snapshot.Phase phase;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd@HH:mm")
    private LocalDateTime nextSnapshotDateTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd@HH:mm")
    private LocalDateTime lastSnapshotDateTime;
}
