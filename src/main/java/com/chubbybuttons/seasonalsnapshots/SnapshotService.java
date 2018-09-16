package com.chubbybuttons.seasonalsnapshots;

import com.chubbybuttons.seasonalsnapshots.util.GifSequenceWriter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@PropertySource("classpath:application.properties")
public class SnapshotService {

    public Collection<Camera> getCameras() {
        ArrayList<Camera> results = new ArrayList<>();
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<Map> response = restTemplate.exchange(apiURL, HttpMethod.GET, entity, Map.class);
        Collection<Map> vals = (Collection) ((Map) ((Map) response.getBody().get("devices")).get("cameras")).values();
        for (Map val : vals) {
            results.add(new Camera((String) val.get("name"), (String) val.get("snapshot_url")));
        }
        return results;
    }

    public Snapshot getSnapshot(Camera camera, Snapshot.Phase phase) throws IOException {
        try (InputStream in = new URL(camera.getSnapshotURL()).openStream()) {
            Snapshot snapshot = new Snapshot();
            snapshot.setSnapshotPhase(phase);
            snapshot.setSnapshotTime(LocalDateTime.now());
            snapshot.setImage(IOUtils.toByteArray(in));
            return snapshot;
        }
    }

    public Snapshot archiveSnapshot(Camera camera, Snapshot snapshot) throws IOException {
        File pic = getArchiveFile(camera, snapshot.getSnapshotPhase(), snapshot.getSnapshotTime());
        FileUtils.writeByteArrayToFile(pic, snapshot.getImage());
        snapshot.setArchivePath(pic.getPath());
        snapshot.setImageName(pic.getName());
        return snapshot;
    }

    public String getStitchedArchive(Camera camera, LocalDate startDate, LocalDate endDate, Snapshot.Phase[] phases) throws IOException {
        Collection<Snapshot> snapshots = getArchiveSnapshots(camera, startDate, endDate, phases);
        ImageOutputStream output = null;
        GifSequenceWriter writer = null;
        File result = new File(getBaseArchivePath(camera) + "/animated.gif");// todo make the file name unique
        for (Snapshot snapshot : snapshots) {
            File snapshotFile = getArchiveFile(camera, snapshot.getSnapshotPhase(), snapshot.getSnapshotTime());
            BufferedImage image = ImageIO.read(snapshotFile);
            if (writer == null) {
                output = new FileImageOutputStream(result);
                writer = new GifSequenceWriter(output, image.getType(), 1, true);
            }
            writer.writeToSequence(image);
        }
        if (writer != null) {
            writer.close();
            output.close();
        }
        return "/snapshots/" + camera.getName() + "/animated.gif";
    }

    public Collection<Snapshot> getArchiveSnapshots(Camera camera, LocalDate startDate, LocalDate endDate, Snapshot.Phase[] phases) {
        IOFileFilter startFilter = FileFilterUtils.ageFileFilter(Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()), false);
        IOFileFilter endFilter = FileFilterUtils.ageFileFilter(Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()), true);
        IOFileFilter extFilter = FileFilterUtils.suffixFileFilter(".jpg");
        IOFileFilter combinedFilter = FileFilterUtils.and(startFilter, endFilter, extFilter);
        Collection<File> files = FileUtils.listFiles(new File(getBaseArchivePath(camera)), combinedFilter, TrueFileFilter.INSTANCE);
        Collection<Snapshot> snapshots = new TreeSet<>();
        for (File file : filterFilesByPhase(files, phases)) {
            snapshots.add(mapSnapShot(file));
        }
        return snapshots;
    }

    private Collection<File> filterFilesByPhase(Collection<File> files, Snapshot.Phase[] phases) {
        if (phases != null && phases.length > 0) {
            TreeSet<File> prunedFiles = new TreeSet<>();
            for (File file : files) {
                for (Snapshot.Phase phase : phases) {
                    if (file.getPath().contains(phase.toString())) {
                        prunedFiles.add(file);
                    }
                }
            }
            return prunedFiles;
        } else {
            return files;
        }
    }

    private Snapshot mapSnapShot(File file) {
        Snapshot snapshot = new Snapshot();
        snapshot.setSnapshotTime(LocalDateTime.parse(FilenameUtils.getBaseName(file.getName()), formatter));
        snapshot.setSnapshotPhase(mapPhase(file));
        snapshot.setImageName(file.getName());
        String normalizedPath = file.getPath().replace('\\', '/');
        normalizedPath = normalizedPath.substring(normalizedPath.indexOf(snapshotBaseDirectory) + snapshotBaseDirectory.length());
        snapshot.setArchivePath(normalizedPath);
        return snapshot;
    }


    public File getArchiveFile(Camera camera, Snapshot.Phase phase, LocalDateTime snapshotTime) {
        String path = getBaseArchivePath(camera) + '/';
        if (phase != null) {
            path += phase.toString() + '/';
        }
        path += snapshotTime.format(formatter) + ".jpg";
        return new File(path);
    }

    private String getBaseArchivePath(Camera camera) {
        String basePath = getClass().getClassLoader().getResource(snapshotBaseDirectory + '/' + snapshotDirectoryName).getPath();
        return basePath + '/' + camera.getName();
    }

    private Snapshot.Phase mapPhase(File file) {
        for (Snapshot.Phase phase : Snapshot.Phase.values()) {
            if (file.getPath().contains(phase.toString())) {
                return phase;
            }
        }
        return null;
    }

    @Value("${camera.api.access_token}")
    private String accessToken;

    @Value("${camera.api.url}")
    private String apiURL;

    @Value("${snapshot.dir}")
    private String snapshotDirectoryName;

    @Value("${snapshot.base.dir}")
    private String snapshotBaseDirectory;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm");
}
