package data_loader;

import graph.TransportGraph;
import model.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Loads operator folders without allowing their local IDs to overwrite each other. */
public final class NetworkLoader {
    private NetworkLoader() {}
    public static TransportGraph load(Path directory, double walkingMeters) throws IOException {
        if (!Files.isDirectory(directory)) throw new IOException("Data directory not found: " + directory);
        List<Path> companies;
        try (var paths = Files.list(directory)) { companies = paths.filter(Files::isDirectory).sorted().toList(); }
        if (companies.isEmpty()) throw new IOException("No operator folders in " + directory);
        Map<String, Stop> stops = new LinkedHashMap<>();
        Map<String, Route> routes = new LinkedHashMap<>();
        Map<String, Trip> trips = new LinkedHashMap<>();
        Map<String, List<StopTime>> times = new LinkedHashMap<>();
        for (Path company : companies) {
            String prefix = company.getFileName().toString().toUpperCase(Locale.ROOT);
            if (!prefix.matches("[A-Z0-9_]+")) throw new IOException("Operator folder must use letters, digits or underscores: " + company);
            Map<String, Stop> rawStops = CsvLoader.loadStops(company.resolve("stops.csv").toString());
            Map<String, Route> rawRoutes = CsvLoader.loadRoutes(company.resolve("routes.csv").toString());
            Map<String, Trip> rawTrips = CsvLoader.loadTrips(company.resolve("trips.csv").toString());
            Map<String, List<StopTime>> rawTimes = CsvLoader.loadStopTimes(company.resolve("stop_times.csv").toString());
            if (rawStops.isEmpty() || rawTrips.isEmpty()) throw new IOException("Empty stops or trips for " + prefix);
            for (Stop s : rawStops.values()) {
                String id = namespace(prefix, s.getId());
                if (stops.putIfAbsent(id, new Stop(id, s.getName(), s.getLatitude(), s.getLongitude())) != null)
                    throw new IOException("Duplicate namespaced stop ID: " + id);
            }
            for (Route r : rawRoutes.values()) {
                String id = namespace(prefix, r.getId());
                if (routes.putIfAbsent(id, new Route(id, r.getShortName(), r.getLongName(), r.getType())) != null)
                    throw new IOException("Duplicate namespaced route ID: " + id);
            }
            for (Trip t : rawTrips.values()) {
                if (!rawRoutes.containsKey(t.getRouteId())) throw new IOException("Unknown route for trip " + t.getId());
                String id = namespace(prefix, t.getId());
                if (trips.putIfAbsent(id, new Trip(id, namespace(prefix, t.getRouteId()))) != null)
                    throw new IOException("Duplicate namespaced trip ID: " + id);
            }
            for (var entry : rawTimes.entrySet()) {
                if (!rawTrips.containsKey(entry.getKey())) throw new IOException("Unknown trip in stop_times: " + entry.getKey());
                String trip = namespace(prefix, entry.getKey());
                List<StopTime> converted = new ArrayList<>();
                for (StopTime t : entry.getValue()) {
                    if (!rawStops.containsKey(t.getStopId())) throw new IOException("Unknown stop in stop_times: " + t.getStopId());
                    converted.add(new StopTime(trip, t.getArrivalTime(), t.getDepartureTime(), namespace(prefix, t.getStopId()), t.getStopSequence()));
                }
                if (times.putIfAbsent(trip, converted) != null) throw new IOException("Duplicate namespaced timetable ID: " + trip);
            }
        }
        try {
            TransportGraph graph = new TransportGraph(stops, times, trips, routes);
            if (graph.generateTransportEdges() == 0) throw new IOException("No scheduled connections in " + directory);
            graph.generateWalkingEdges(walkingMeters);
            return graph;
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid timetable: " + e.getMessage(), e);
        }
    }
    private static String namespace(String company, String id) {
        return id.startsWith(company + "-") ? id : company + "-" + id;
    }
}
