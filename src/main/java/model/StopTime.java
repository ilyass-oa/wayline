package model;

import java.time.LocalTime;
import java.util.Objects;

/** A stop event on one service-day timeline, including hours greater than 24. */
public final class StopTime implements Comparable<StopTime> {
    private final String tripId;
    private final String arrivalTime;
    private final String departureTime;
    private final String stopId;
    private final int stopSequence;
    private final int arrivalSeconds;
    private final int departureSeconds;

    public StopTime(String tripId, String departureTime, String stopId, int stopSequence) {
        this(tripId, departureTime, departureTime, stopId, stopSequence);
    }

    public StopTime(String tripId, String arrivalTime, String departureTime, String stopId, int stopSequence) {
        this.tripId = requireId(tripId);
        this.stopId = requireId(stopId);
        this.arrivalSeconds = parseTime(arrivalTime);
        this.departureSeconds = parseTime(departureTime);
        if (arrivalSeconds > departureSeconds || stopSequence < 0) {
            throw new IllegalArgumentException("Arrival must precede departure; sequence must be nonnegative");
        }
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopSequence = stopSequence;
    }

    private static String requireId(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("An ID must not be blank");
        return value;
    }

    public static int parseTime(String time) {
        if (time == null || !time.matches("[0-9]+:[0-5][0-9]:[0-5][0-9]")) {
            throw new IllegalArgumentException("Expected HH:mm:ss, allowing hours beyond 24: " + time);
        }
        String[] parts = time.split(":");
        try {
            long seconds = Math.addExact(Math.multiplyExact(Long.parseLong(parts[0]), 3600),
                    Integer.parseInt(parts[1]) * 60L + Integer.parseInt(parts[2]));
            if (seconds > Integer.MAX_VALUE) throw new IllegalArgumentException("Time is too large");
            return (int) seconds;
        } catch (ArithmeticException | NumberFormatException e) {
            throw new IllegalArgumentException("Time is too large", e);
        }
    }

    public String getTripId() { return tripId; }
    public String getArrivalTime() { return arrivalTime; }
    public String getDepartureTime() { return departureTime; }
    public String getStopId() { return stopId; }
    public int getStopSequence() { return stopSequence; }
    public int getArrivalTimeSeconds() { return arrivalSeconds; }
    public int getDepartureTimeSeconds() { return departureSeconds; }
    public LocalTime getDepartureLocalTime() { return LocalTime.ofSecondOfDay(departureSeconds % 86400); }
    public LocalTime getArrivalLocalTime() { return LocalTime.ofSecondOfDay(arrivalSeconds % 86400); }

    @Override
    public int compareTo(StopTime other) {
        return Integer.compare(departureSeconds, Objects.requireNonNull(other).departureSeconds);
    }

    @Override
    public String toString() {
        return "StopTime{" + tripId + ", " + stopId + ", " + departureTime + ", " + stopSequence + "}";
    }
}
