package com.chubbybuttons.seasonalsnapshots;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

@SpringBootApplication
@EnableScheduling
@EnableEncryptableProperties
@RestController
public class SeasonalSnapshotsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeasonalSnapshotsApplication.class, args);
    }

    @Scheduled(fixedDelay = 30000)
    public void checkForSnapshot() throws Exception {
        for (Snapshot.Phase phase : Snapshot.Phase.values()) {
            if (seasonalTimeService.isPhaseExpired(phase)) {
                LocalDateTime phaseTime = seasonalTimeService.getPhaseTime(phase);
                archiveSnapshot(phaseTime, phase);
                seasonalTimeService.updatePhaseSchedules();
                break;
            }
        }
    }

    private void archiveSnapshot(LocalDateTime dateTime, Snapshot.Phase phase) throws Exception {
        Collection<Camera> cameras = snapshotService.getCameras();
        for (Camera camera : cameras) {
            LOG.debug(camera.toString());
            if (!snapshotService.isPhaseArchived(camera, phase, dateTime)) {
                Snapshot snapshot = snapshotService.getSnapshot(camera, phase);
                snapshotService.archiveSnapshot(camera, snapshot);
                LOG.info("Archived image for phase " + phase);
            } else {
                LOG.debug("Snapshot already archived");
            }
        }
    }

    @RequestMapping(value = "/callback")
    public void callback(@RequestParam(value = "code") String code) {
        LOG.info("Callback code found: " + code);
    }

    @RequestMapping(value = "/test")
    public void test(@RequestParam(value = "phase", required = false) Snapshot.Phase phase) throws Exception {
        archiveSnapshot(LocalDateTime.now(), phase);
    }

    @RequestMapping(value = "/getCameras")
    public Collection<Camera> getCameras() {
        return snapshotService.getCameras();
    }

    @RequestMapping(value = "/getSnapshotPaths")
    public Collection<Snapshot> getSnapshotPaths(@RequestParam(value = "camera") String cameraName,
                                                 @RequestParam(value = "phase", required = false) Snapshot.Phase[] phases,
                                                 @RequestParam(value = "start") @DateTimeFormat(pattern = "MM/dd/yyyy") LocalDate start,
                                                 @RequestParam(value = "end", required = false) @DateTimeFormat(pattern = "MM/dd/yyyy") LocalDate end) {
        Camera camera = new Camera(cameraName);
        return snapshotService.getArchiveSnapshots(camera, start, end, phases);
    }

    @RequestMapping(value = "/getSnapshotPathsStitched")
    public String getSnapshotPathsStitched(@RequestParam(value = "camera") String cameraName,
                                           @RequestParam(value = "phase", required = false) Snapshot.Phase[] phases,
                                           @RequestParam(value = "start") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate start,
                                           @RequestParam(value = "end", required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate end) throws IOException {
        Camera camera = new Camera(cameraName);
        return snapshotService.getStitchedArchive(camera, start, end, phases);
    }


    @Autowired
    private SeasonalTimeService seasonalTimeService;
    @Autowired
    private SnapshotService snapshotService;

    private static final Logger LOG = LoggerFactory.getLogger(SeasonalSnapshotsApplication.class);
}
