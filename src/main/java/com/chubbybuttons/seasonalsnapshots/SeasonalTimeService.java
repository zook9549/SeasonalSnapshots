package com.chubbybuttons.seasonalsnapshots;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@PropertySource("classpath:application.properties")
public class SeasonalTimeService {

    public LocalDateTime getPhaseTime(Snapshot.Phase phase) {
        switch (phase) {
            case SUNRISE:
                return getSunrise();
            case SOLAR_NOON:
                return getSolarNoon();
            case SUNSET:
                return getSunset();
            default:
                return null;
        }
    }

    public LocalDateTime getSunrise() {
        return getTimes(LocalDate.now())[0];
    }

    public LocalDateTime getSolarNoon() {
        return getTimes(LocalDate.now())[1];
    }

    public LocalDateTime getSunset() {
        return getTimes(LocalDate.now())[2];
    }

    private LocalDateTime[] getTimes(LocalDate date) {
        if (localDateTimes[0] == null || !date.equals(localDateTimes[0].toLocalDate())) {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(apiURL, Map.class);
            Map results = (Map) response.getBody().get("results");
            String sunrise = (String) results.get("sunrise");
            String solarNoon = (String) results.get("solar_noon");
            String sunset = (String) results.get("sunset");
            localDateTimes[0] = getLocalDateTime(sunrise).plusMinutes(sunriseOffsetMinutes);
            localDateTimes[1] = getLocalDateTime(solarNoon);
            localDateTimes[2] = getLocalDateTime(sunset).plusMinutes(sunsetOffsetMinutes);

            LOG.info("Setting new times for snapshots...");
            LOG.info("  Sunrise: " + localDateTimes[0].format(pattern));
            LOG.info("  Solar Noon: " + localDateTimes[1].format(pattern));
            LOG.info("  Sunset: " + localDateTimes[2].format(pattern));
        }
        return localDateTimes;
    }

    private LocalDateTime getLocalDateTime(String timestamp) {
        LocalTime ldt = LocalTime.parse(timestamp, pattern);
        ZonedDateTime zdt = ZonedDateTime.ofLocal(ldt.atDate(LocalDate.now()), ZoneId.of("UTC"), null);
        return zdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Value("${time.api.url}")
    private String apiURL;

    @Value("${time.sunrise.offset.min}")
    private int sunriseOffsetMinutes;

    @Value("${time.sunset.offset.min}")
    private int sunsetOffsetMinutes;

    private LocalDateTime[] localDateTimes = new LocalDateTime[3];
    private static final Logger LOG = LoggerFactory.getLogger(SeasonalTimeService.class);
    private static final DateTimeFormatter pattern = DateTimeFormatter.ofPattern("h:mm:ss a");

}
