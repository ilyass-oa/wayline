package data_loader;

import model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Header-based UTF-8 CSV loading. Malformed input fails with a file and record number. */
public final class CsvLoader {
    private CsvLoader() {}
    @FunctionalInterface private interface RowConsumer { void accept(Map<String, String> row); }

    public static Map<String, Stop> loadStops(String path) throws IOException {
        Map<String, Stop> result = new LinkedHashMap<>();
        read(path, List.of("stop_id", "stop_name", "stop_lat", "stop_lon"), row -> {
            String id = id(row, "stop_id");
            double lat = Double.parseDouble(row.get("stop_lat"));
            double lon = Double.parseDouble(row.get("stop_lon"));
            if (!Double.isFinite(lat) || !Double.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180)
                throw new IllegalArgumentException("Invalid coordinates");
            String name = id(row, "stop_name");
            unique(result, id, new Stop(id, name, lat, lon));
        });
        return result;
    }
    public static Map<String, Route> loadRoutes(String path) throws IOException {
        Map<String, Route> result = new LinkedHashMap<>();
        read(path, List.of("route_id", "route_short_name", "route_long_name", "route_type"), row -> {
            String id = id(row, "route_id");
            unique(result, id, new Route(id, row.get("route_short_name"), row.get("route_long_name"), id(row, "route_type")));
        });
        return result;
    }
    public static Map<String, Trip> loadTrips(String path) throws IOException {
        Map<String, Trip> result = new LinkedHashMap<>();
        read(path, List.of("trip_id", "route_id"), row -> {
            String id = id(row, "trip_id");
            unique(result, id, new Trip(id, id(row, "route_id")));
        });
        return result;
    }
    public static Map<String, List<StopTime>> loadStopTimes(String path) throws IOException {
        Map<String, List<StopTime>> result = new LinkedHashMap<>();
        read(path, List.of("trip_id", "departure_time", "stop_id", "stop_sequence"), row -> {
            String trip = id(row, "trip_id");
            String departure = id(row, "departure_time");
            String arrival = row.containsKey("arrival_time") ? id(row, "arrival_time") : departure;
            StopTime time = new StopTime(trip, arrival, departure, id(row, "stop_id"), Integer.parseInt(row.get("stop_sequence")));
            result.computeIfAbsent(trip, k -> new ArrayList<>()).add(time);
        });
        result.values().forEach(list -> list.sort(Comparator.comparingInt(StopTime::getStopSequence)));
        return result;
    }
    private static String id(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Blank " + key);
        return value;
    }
    private static <T> void unique(Map<String, T> result, String id, T value) {
        if (result.putIfAbsent(id, value) != null) throw new IllegalArgumentException("Duplicate ID: " + id);
    }
    private static void read(String path, List<String> required, RowConsumer consumer) throws IOException {
        int record = 1;
        try (CsvReader csv = new CsvReader(Path.of(path))) {
            List<String> header = csv.next();
            if (header == null) throw new IOException("Missing CSV header");
            header.set(0, header.get(0).replaceFirst("^\uFEFF", ""));
            if (new HashSet<>(header).size() != header.size() || header.stream().anyMatch(String::isBlank))
                throw new IOException("Duplicate or blank CSV header");
            if (!header.containsAll(required)) throw new IOException("Required columns: " + String.join(", ", required));
            List<String> values;
            while ((values = csv.next()) != null) {
                record++;
                if (values.size() == 1 && values.get(0).isBlank()) continue;
                if (values.size() != header.size()) throw new IOException("Wrong number of CSV fields");
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < header.size(); i++) row.put(header.get(i), values.get(i));
                consumer.accept(row);
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new IOException(path + ": record " + record + ": " + e.getMessage(), e);
        }
    }

    private static final class CsvReader implements AutoCloseable {
        private final PushbackReader reader;
        CsvReader(Path path) throws IOException {
            reader = new PushbackReader(Files.newBufferedReader(path, StandardCharsets.UTF_8), 1);
        }
        List<String> next() throws IOException {
            List<String> values = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean quoted = false, closed = false, wasQuoted = false, consumed = false;
            int value;
            while ((value = reader.read()) != -1) {
                consumed = true;
                char c = (char) value;
                if (quoted) {
                    if (c == '"') {
                        int following = reader.read();
                        if (following == '"') field.append('"');
                        else {
                            quoted = false; closed = true;
                            if (following != -1) reader.unread(following);
                        }
                    } else field.append(c);
                } else if (c == ',' || c == '\n' || c == '\r') {
                    values.add(wasQuoted ? field.toString() : field.toString().strip());
                    field.setLength(0); closed = false; wasQuoted = false;
                    if (c != ',') {
                        if (c == '\r') {
                            int following = reader.read();
                            if (following != '\n' && following != -1) reader.unread(following);
                        }
                        return values;
                    }
                } else if (closed) {
                    if (c != ' ' && c != '\t') throw new IOException("Unexpected text after closing quote");
                } else if (c == '"') {
                    if (!field.toString().isBlank()) throw new IOException("Quote in unquoted field");
                    field.setLength(0); quoted = true; wasQuoted = true;
                } else field.append(c);
                if (field.length() > 1_000_000) throw new IOException("CSV field is too long");
            }
            if (quoted) throw new IOException("Unclosed CSV quote");
            if (!consumed && values.isEmpty()) return null;
            values.add(wasQuoted ? field.toString() : field.toString().strip());
            return values;
        }
        @Override public void close() throws IOException { reader.close(); }
    }
}
