package graph;

import model.*;
import util.Utils;
import java.util.*;

public final class TransportGraph {
    private final Map<String, Stop> stops;
    private final Map<String, List<StopTime>> times;
    private final Map<String, Trip> trips;
    private final Map<String, Route> routes;
    private final Map<String, Integer> indices = new LinkedHashMap<>();
    private final Map<Integer, String> stopIds = new LinkedHashMap<>();
    private final List<List<Edge>> outgoing = new ArrayList<>();
    private final Map<Cell, List<Stop>> spatial = new HashMap<>();
    private final Set<WalkingPair> walkingPairs = new HashSet<>();
    private boolean transportBuilt;
    private record Cell(int lat, int lon) {}
    private record WalkingPair(String from, String to) {}
    private static final double CELL_DEGREES = 0.01;

    public TransportGraph(Map<String, Stop> stops, Map<String, List<StopTime>> times,
                          Map<String, Trip> trips, Map<String, Route> routes) {
        this.stops = Collections.unmodifiableMap(new TreeMap<>(stops));
        this.trips = Map.copyOf(trips);
        this.routes = Map.copyOf(routes);
        Map<String, List<StopTime>> copy = new TreeMap<>();
        times.forEach((id, values) -> copy.put(id, List.copyOf(values)));
        this.times = Collections.unmodifiableMap(copy);
        for (Map.Entry<String, Stop> entry : this.stops.entrySet()) {
            Stop stop = entry.getValue();
            if (!entry.getKey().equals(stop.getId())) throw new IllegalArgumentException("Stop key differs from ID");
            validateCoordinates(stop);
            int index = indices.size();
            indices.put(stop.getId(), index);
            stopIds.put(index, stop.getId());
            outgoing.add(new ArrayList<>());
            spatial.computeIfAbsent(cell(stop.getLatitude(), stop.getLongitude()), k -> new ArrayList<>()).add(stop);
        }
    }

    private static void validateCoordinates(Stop stop) {
        if (!Double.isFinite(stop.getLatitude()) || !Double.isFinite(stop.getLongitude()) ||
                Math.abs(stop.getLatitude()) > 90 || Math.abs(stop.getLongitude()) > 180) {
            throw new IllegalArgumentException("Invalid coordinates for stop " + stop.getId());
        }
    }
    private static Cell cell(double lat, double lon) {
        return new Cell((int) Math.floor(lat / CELL_DEGREES), (int) Math.floor(lon / CELL_DEGREES));
    }
    public void addEdge(Edge edge) {
        Integer from = indices.get(edge.getFrom().getId());
        if (from == null || !indices.containsKey(edge.getTo().getId())) throw new IllegalArgumentException("Unknown edge endpoint");
        outgoing.get(from).add(edge);
    }

    public int generateTransportEdges() {
        if (transportBuilt) return 0;
        List<Edge> pending = new ArrayList<>();
        for (Map.Entry<String, List<StopTime>> entry : times.entrySet()) {
            Trip trip = trips.get(entry.getKey());
            if (trip == null) throw new IllegalArgumentException("Unknown trip: " + entry.getKey());
            Route route = routes.get(trip.getRouteId());
            if (route == null) throw new IllegalArgumentException("Unknown route: " + trip.getRouteId());
            String mode = transportMode(route.getType());
            List<StopTime> events = new ArrayList<>(entry.getValue());
            events.sort(Comparator.comparingInt(StopTime::getStopSequence));
            for (int i = 0; i < events.size(); i++) {
                StopTime current = events.get(i);
                if (!current.getTripId().equals(entry.getKey()) || !stops.containsKey(current.getStopId())) {
                    throw new IllegalArgumentException("Unknown stop or mismatched trip in stop_times");
                }
                if (i == 0) continue;
                StopTime previous = events.get(i - 1);
                if (previous.getStopSequence() == current.getStopSequence() ||
                        current.getArrivalTimeSeconds() < previous.getDepartureTimeSeconds()) {
                    throw new IllegalArgumentException("Trip " + trip.getId() + " has duplicate sequence or backwards times; use 24+ hours after midnight");
                }
                pending.add(new Edge(stops.get(previous.getStopId()), stops.get(current.getStopId()),
                        previous.getDepartureTimeSeconds(), current.getArrivalTimeSeconds(), mode, trip.getId()));
            }
        }
        pending.forEach(this::addEdge);
        transportBuilt = true;
        return pending.size();
    }

    private static String transportMode(String type) {
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "0", "TRAM" -> "TRAM";
            case "1", "METRO" -> "METRO";
            case "2", "TRAIN" -> "TRAIN";
            case "3", "BUS" -> "BUS";
            default -> throw new IllegalArgumentException("Unsupported route_type: " + type);
        };
    }

    public int generateWalkingEdges(double meters) {
        if (!Double.isFinite(meters) || meters < 0) throw new IllegalArgumentException("Walking radius must be finite and nonnegative");
        if (meters == 0) return 0;
        double km = meters / 1000;
        int added = 0;
        for (Stop from : stops.values()) {
            double latRadius = Math.min(180, km / 110.0);
            double cos = Math.cos(Math.toRadians(Math.min(90, Math.abs(from.getLatitude()) + latRadius)));
            double lonRadius = cos < 1e-9 ? 360 : Math.min(360, km / (110.0 * cos));
            Cell min = cell(Math.max(-90, from.getLatitude() - latRadius), Math.max(-180, from.getLongitude() - lonRadius));
            Cell max = cell(Math.min(90, from.getLatitude() + latRadius), Math.min(180, from.getLongitude() + lonRadius));
            Collection<List<Stop>> groups;
            long cells = (long) (max.lat - min.lat + 1) * (max.lon - min.lon + 1);
            if (cells > spatial.size() * 4L || from.getLongitude() - lonRadius < -180 || from.getLongitude() + lonRadius > 180) {
                groups = spatial.values();
            } else {
                List<List<Stop>> nearby = new ArrayList<>();
                for (int lat = min.lat; lat <= max.lat; lat++) {
                    for (int lon = min.lon; lon <= max.lon; lon++) {
                        List<Stop> group = spatial.get(new Cell(lat, lon));
                        if (group != null) nearby.add(group);
                    }
                }
                groups = nearby;
            }
            for (List<Stop> group : groups) for (Stop to : group) {
                if (from.getId().equals(to.getId())) continue;
                double distance = Utils.haversine(from.getLatitude(), from.getLongitude(), to.getLatitude(), to.getLongitude());
                if (distance <= km && walkingPairs.add(new WalkingPair(from.getId(), to.getId()))) {
                    addEdge(new Edge(from, to, Utils.walkingTimeInSeconds(distance), distance * 1000));
                    added++;
                }
            }
        }
        return added;
    }

    public List<Edge> getOutgoingEdges(String stop) {
        Integer index = indices.get(stop);
        return index == null ? List.of() : getOutgoingEdgesByIndex(index);
    }
    public List<Edge> getOutgoingEdgesByIndex(int index) {
        return index < 0 || index >= outgoing.size() ? List.of() : Collections.unmodifiableList(outgoing.get(index));
    }
    public Map<String, Integer> getStopIdToIndex() { return Collections.unmodifiableMap(indices); }
    public Map<Integer, String> getIndexToStopId() { return Collections.unmodifiableMap(stopIds); }
    public Map<String, Stop> getStops() { return stops; }
    public Map<String, List<StopTime>> getStopTimesByTrip() { return times; }
    public Map<String, Trip> getTrips() { return trips; }
    public Map<String, Route> getRoutes() { return routes; }
    public List<Map.Entry<String, String>> findStopsByPartialName(String query) {
        String pattern = Objects.requireNonNull(query).toLowerCase(Locale.ROOT).strip();
        return stops.values().stream().filter(s -> s.getName().toLowerCase(Locale.ROOT).contains(pattern))
                .sorted(Comparator.comparing((Stop s) -> !s.getName().toLowerCase(Locale.ROOT).startsWith(pattern))
                        .thenComparing(Stop::getName).thenComparing(Stop::getId))
                .map(s -> Map.entry(s.getId(), s.getName())).toList();
    }
    @Override public String toString() {
        return "TransportGraph{stops=" + stops.size() + ", trips=" + trips.size() + ", edges=" + outgoing.stream().mapToInt(List::size).sum() + "}";
    }
}
