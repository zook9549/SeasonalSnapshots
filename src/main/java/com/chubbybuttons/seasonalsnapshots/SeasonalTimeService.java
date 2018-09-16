package com.chubbybuttons.seasonalsnapshots;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

@Service
@RestController
@PropertySource("classpath:application.properties")
public class SeasonalTimeService {


    public LocalDateTime getPhaseTime(Snapshot.Phase phase) {
        if (isDateTimeExpired(phase)) {
            updateExpiredDateTimes();
        }
        return phaseSchedules.get(phase).getNextSnapshotDateTime();
    }

    @RequestMapping(value = "/getTimes")
    public Collection<PhaseSchedule> getTimes() {
        return new TreeSet<>(phaseSchedules.values());
    }

    private Map<Snapshot.Phase, LocalDateTime> getTimesForDate(LocalDate date) {
        Map<Snapshot.Phase, LocalDateTime> timesForDate = new HashMap<>(3);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Map> response = restTemplate.getForEntity(MessageFormat.format(apiURL, java.sql.Date.valueOf(date)), Map.class);
        Map results = (Map) response.getBody().get("results");
        String sunrise = (String) results.get("sunrise");
        String solarNoon = (String) results.get("solar_noon");
        String sunset = (String) results.get("sunset");

        timesForDate.put(Snapshot.Phase.SUNRISE, getLocalDateTime(date, sunrise).plusMinutes(sunriseOffsetMinutes));
        timesForDate.put(Snapshot.Phase.SOLAR_NOON, getLocalDateTime(date, solarNoon));
        timesForDate.put(Snapshot.Phase.SUNSET, getLocalDateTime(date, sunset).plusMinutes(sunsetOffsetMinutes));
        return timesForDate;
    }

    private boolean isDateTimeExpired(Snapshot.Phase phase) {
        PhaseSchedule phaseSchedule = phaseSchedules.get(phase);
        LocalDateTime nextPhaseDateTime = phaseSchedule.getNextSnapshotDateTime();
        return nextPhaseDateTime == null || nextPhaseDateTime.isBefore(LocalDateTime.now());
    }

    private LocalDateTime getLocalDateTime(LocalDate date, String timestamp) {
        LocalTime ldt = LocalTime.parse(timestamp, pattern);
        ZonedDateTime zdt = ZonedDateTime.ofLocal(ldt.atDate(date), ZoneId.of("UTC"), null);
        return zdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private void updateExpiredDateTimes() {
        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime tomorrow = currentDate.plusDays(1);
        Map<Snapshot.Phase, LocalDateTime> dateTimes = getTimesForDate(tomorrow.toLocalDate());
        for (Snapshot.Phase phase : dateTimes.keySet()) {
            PhaseSchedule phaseSchedule = phaseSchedules.get(phase);
            LocalDateTime phaseDateTime = phaseSchedule.getNextSnapshotDateTime();
            if (phaseDateTime.isBefore(currentDate)) {
                LocalDateTime updatedDateTime = dateTimes.get(phase);
                phaseSchedule.setLastSnapshotDateTime(phaseDateTime);
                phaseSchedule.setNextSnapshotDateTime(updatedDateTime);
                LOG.info("Updated next phase snapshot time");
                logPhase(phase);
            }
        }
    }

    @PostConstruct
    public void init() {
        LocalDateTime currentDate = LocalDateTime.now();
        Map<Snapshot.Phase, LocalDateTime> dateTimes = getTimesForDate(currentDate.toLocalDate());
        boolean containsExpiredDates = false;
        for (Snapshot.Phase phase : dateTimes.keySet()) {
            PhaseSchedule phaseSchedule = new PhaseSchedule(phase);
            LocalDateTime phaseDateTime = dateTimes.get(phase);
            if (currentDate.isAfter(phaseDateTime)) {
                phaseSchedule.setLastSnapshotDateTime(phaseDateTime);
                containsExpiredDates = true;
            } else {
                phaseSchedule.setNextSnapshotDateTime(phaseDateTime);
            }
            phaseSchedules.put(phase, phaseSchedule);
        }
        if (containsExpiredDates) {
            LocalDateTime tomorrow = currentDate.plusDays(1);
            dateTimes = getTimesForDate(tomorrow.toLocalDate());
            for (Snapshot.Phase phase : dateTimes.keySet()) {
                if(isDateTimeExpired(phase)) {
                    PhaseSchedule phaseSchedule = phaseSchedules.get(phase);
                    phaseSchedule.setNextSnapshotDateTime(dateTimes.get(phase));
                }

            }
        }
        logTimes();
    }

    private void logTimes() {
        if (LOG.isInfoEnabled()) {
            LOG.info("Times established for snapshots:");
            for (Snapshot.Phase phase : Snapshot.Phase.values()) {
                logPhase(phase);
            }
        }
    }

    private void logPhase(Snapshot.Phase phase) {
        StringBuilder sb = new StringBuilder("  ").append(phase.toString()).append(":\t\t");
        sb.append(phaseSchedules.get(phase).getNextSnapshotDateTime().format(fullPattern));
        LocalDateTime previous = phaseSchedules.get(phase).getLastSnapshotDateTime();
        if(previous != null) {
            sb.append(" (Previous: ").append(previous.format(fullPattern)).append(')');
        }
        LOG.info(sb.toString());
    }

    @Value("${time.api.url}")
    private String apiURL;

    @Value("${time.sunrise.offset.min}")
    private int sunriseOffsetMinutes;

    @Value("${time.sunset.offset.min}")
    private int sunsetOffsetMinutes;

    private Map<Snapshot.Phase, PhaseSchedule> phaseSchedules = new HashMap<>(3);

    private static final Logger LOG = LoggerFactory.getLogger(SeasonalTimeService.class);
    private static final DateTimeFormatter fullPattern = DateTimeFormatter.ofPattern("yyyy-MM-dd h:mm:ss a");
    private static final DateTimeFormatter pattern = DateTimeFormatter.ofPattern("h:mm:ss a");

}
