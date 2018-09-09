package com.chubbybuttons.seasonalsnapshots;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

@EnableAutoConfiguration
@SpringBootApplication
@EnableScheduling
@EnableEncryptableProperties
@RestController
public class SeasonalSnapshotsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeasonalSnapshotsApplication.class, args);
    }

    @Scheduled(fixedRate = 30000)
    public void checkForSnapshot() throws Exception {
        LocalDateTime dateTime = LocalDateTime.now();
        for (Snapshot.Phase phase : Snapshot.Phase.values()) {
            LocalDateTime phaseTime = seasonalTimeService.getPhaseTime(phase);
            if (equalsSameTime(dateTime, phaseTime)) {
                LOG.info("Archiving image for phase " + phase);
                archiveSnapshot(phaseTime, phase);
                break;
            }
        }
    }

    private void archiveSnapshot(LocalDateTime dateTime, Snapshot.Phase phase) throws Exception {
        Collection<Camera> cameras = snapshotService.getCameras();
        for (Camera camera : cameras) {
            LOG.info(camera.toString());
            if (!snapshotService.getArchiveFile(camera, phase, dateTime).exists()) {
                Snapshot snapshot = snapshotService.getSnapshot(camera, phase);
                snapshotService.archiveSnapshot(camera, snapshot);
            } else {
                LOG.info("Snapshot already archived");
            }
        }
    }

    private boolean equalsSameTime(LocalDateTime current, LocalDateTime comparison) {
        return Duration.between(current, comparison).toMinutes() == 0 && current.getMinute() == comparison.getMinute();
    }

    @RequestMapping(value = "/callback")
    public void callback(@RequestParam(value = "code") String code) {
        System.out.println(code);
    }

    @RequestMapping(value = "/test")
    public void test(@RequestParam(value = "phase") String phase) throws Exception {
        archiveSnapshot(LocalDateTime.now(), Snapshot.Phase.valueOf(phase));
    }

    @RequestMapping(value = "/getCameras")
    public Collection<Camera> getCameras() {
        return snapshotService.getCameras();
    }

    @RequestMapping(value = "/getSnapshotPaths")
    public Collection<Snapshot> getSnapshotPaths(@RequestParam(value = "camera") String cameraName,
                                                 @RequestParam(value = "phase", required = false) Snapshot.Phase[] phases,
                                                 @RequestParam(value = "start") @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate start,
                                                 @RequestParam(value = "end", required = false) @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate end) throws IOException {
        Camera camera = new Camera(cameraName);
        return snapshotService.getArchiveSnapshots(camera, start, end, phases);
    }


    @Autowired
    private SeasonalTimeService seasonalTimeService;
    @Autowired
    private SnapshotService snapshotService;

    private static final Logger LOG = LoggerFactory.getLogger(SeasonalSnapshotsApplication.class);
}
