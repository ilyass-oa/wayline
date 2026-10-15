package graph;

import java.time.LocalTime;
import java.util.Locale;
import java.util.Objects;
import model.Stop;
import util.Utils;

/** A scheduled connection or a walking connection. Times retain the service-day offset. */
public final class Edge {
    private final Stop from;
    private final Stop to;
    private final int departureSeconds;
    private final int arrivalSeconds;
    private final String mode;
    private final String tripId;
    private final long durationSeconds;
    private final double walkingDistanceMeters;

    public Edge(Stop from, Stop to, LocalTime departure, LocalTime arrival, String mode, String tripId) {
        this(from, to, Objects.requireNonNull(departure).toSecondOfDay(),
                arrival.toSecondOfDay() + (arrival.isBefore(departure) ? 86400 : 0), mode, tripId);
    }

    public Edge(Stop from, Stop to, int departure, int arrival, String mode, String tripId) {
        if (departure < 0 || arrival < departure || tripId == null || tripId.isBlank()) {
            throw new IllegalArgumentException("Invalid scheduled connection");
        }
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.departureSeconds = departure;
        this.arrivalSeconds = arrival;
        this.mode = Objects.requireNonNull(mode).toUpperCase(Locale.ROOT);
        if ("WALK".equals(this.mode)) throw new IllegalArgumentException("Use the walking constructor");
        this.tripId = tripId;
        this.durationSeconds = (long) arrival - departure;
        this.walkingDistanceMeters = 0;
    }

    public Edge(Stop from, Stop to, long durationSeconds, double walkingDistanceMeters) {
        if (durationSeconds <= 0 || durationSeconds > Integer.MAX_VALUE ||
                !Double.isFinite(walkingDistanceMeters) || walkingDistanceMeters < 0) {
            throw new IllegalArgumentException("Invalid walking connection");
        }
        this.from = Objects.requireNonNull(from);
        this.to = Objects.requireNonNull(to);
        this.departureSeconds = 0;
        this.arrivalSeconds = (int) durationSeconds;
        this.mode = "WALK";
        this.tripId = null;
        this.durationSeconds = durationSeconds;
        this.walkingDistanceMeters = walkingDistanceMeters;
    }

    public Edge(Stop from, Stop to, long durationSeconds) {
        this(from, to, durationSeconds, Utils.haversine(from.getLatitude(), from.getLongitude(),
                to.getLatitude(), to.getLongitude()) * 1000);
    }

    public Stop getFrom() { return from; }
    public Stop getTo() { return to; }
    public LocalTime getDeparture() { return "WALK".equals(mode) ? null : LocalTime.ofSecondOfDay(departureSeconds % 86400); }
    public LocalTime getArrival() { return "WALK".equals(mode) ? null : LocalTime.ofSecondOfDay(arrivalSeconds % 86400); }
    public int getDepartureSeconds() { return departureSeconds; }
    public int getArrivalSeconds() { return arrivalSeconds; }
    public String getMode() { return mode; }
    public String getTripId() { return tripId; }
    public long getDurationSeconds() { return durationSeconds; }
    public double getWalkingDistanceMeters() { return walkingDistanceMeters; }
    public LocalTime getArrivalTime(LocalTime departure) {
        return "WALK".equals(mode) ? departure.plusSeconds(durationSeconds) : getArrival();
    }
}
