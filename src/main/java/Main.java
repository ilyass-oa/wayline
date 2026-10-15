import algorithms.PathPreferences;
import data_loader.NetworkLoader;
import graph.TransportGraph;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Command-line entry point. */
public final class Main {
    private Main() {}
    public static void main(String[] args) {
        int status = run(args, System.in, System.out, System.err);
        if (status != 0) System.exit(status);
    }
    static int run(String[] args, InputStream input, PrintStream out, PrintStream err) {
        try {
            Map<String, String> options = parse(args);
            if (options.containsKey("--help")) { help(out); return 0; }
            boolean routeQuery = options.containsKey("--from") || options.containsKey("--to") || options.containsKey("--at");
            if (routeQuery && !(options.containsKey("--from") && options.containsKey("--to") && options.containsKey("--at")))
                throw new IllegalArgumentException("Use --from, --to and --at together.");
            PathPreferences prefs = new PathPreferences();
            prefs.setMaxWalkingDistance(Double.parseDouble(options.getOrDefault("--max-walk", "800")) / 1000);
            prefs.setMaxTransfers(Integer.parseInt(options.getOrDefault("--max-transfers", "2147483647")));
            if (options.containsKey("--modes")) {
                for (var mode : PathPreferences.TransportMode.values()) prefs.setModeAllowed(mode, false);
                for (String mode : options.get("--modes").split(",", -1))
                    prefs.setModeAllowed(PathPreferences.TransportMode.valueOf(mode.strip().toUpperCase(Locale.ROOT)), true);
            }
            if (options.containsKey("--exclude-company"))
                prefs.setCompanyAllowed(options.get("--exclude-company"), false);
            LocalTime departure = routeQuery ? time(options.get("--at")) : null;
            TransportGraph graph = NetworkLoader.load(Path.of(options.getOrDefault("--data", "examples/demo")),
                    prefs.getMaxWalkingDistance() * 1000);
            TransportPlanner planner = new TransportPlanner(graph);
            if (routeQuery) {
                requireStop(graph, options.get("--from")); requireStop(graph, options.get("--to"));
                var route = planner.findRoute(options.get("--from"), options.get("--to"), departure, prefs);
                planner.printRoute(route, out);
                return route.isEmpty() ? 1 : 0;
            }
            Scanner scanner = new Scanner(input, StandardCharsets.UTF_8);
            out.println("Wayline | Enter an ID or station name. Type q to quit.");
            while (true) {
                String from = station(scanner, out, graph, "From");
                if (from == null) return 0;
                String to = station(scanner, out, graph, "To");
                if (to == null) return 0;
                while (true) {
                    String value = prompt(scanner, out, "Departure (HH:mm): ");
                    if (value == null) return 0;
                    try { departure = time(value); break; }
                    catch (IllegalArgumentException e) { out.println(e.getMessage()); }
                }
                planner.printRoute(planner.findRoute(from, to, departure, prefs), out);
            }
        } catch (IOException | IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            return 2;
        }
    }
    private static Map<String, String> parse(String[] args) {
        Set<String> valued = Set.of("--data", "--from", "--to", "--at", "--max-walk", "--max-transfers", "--modes", "--exclude-company");
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String key = args[i];
            if (result.containsKey(key)) throw new IllegalArgumentException("Duplicate option: " + key);
            if (key.equals("--help")) result.put(key, "");
            else if (!valued.contains(key)) throw new IllegalArgumentException("Unknown option: " + key);
            else {
                if (++i == args.length || args[i].startsWith("--") || args[i].isBlank())
                    throw new IllegalArgumentException("Missing value for " + key);
                result.put(key, args[i]);
            }
        }
        return result;
    }
    private static LocalTime time(String value) {
        try {
            if (!value.matches("[0-2][0-9]:[0-5][0-9]")) throw new DateTimeParseException("Invalid time", value, 0);
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) { throw new IllegalArgumentException("Departure must be HH:mm between 00:00 and 23:59."); }
    }
    private static void requireStop(TransportGraph graph, String id) {
        if (!graph.getStops().containsKey(id)) throw new IllegalArgumentException("Unknown stop ID: " + id);
    }
    private static String prompt(Scanner scanner, PrintStream out, String text) {
        out.print(text); out.flush();
        if (!scanner.hasNextLine()) return null;
        String value = scanner.nextLine().strip();
        return value.equalsIgnoreCase("q") ? null : value;
    }
    private static String station(Scanner scanner, PrintStream out, TransportGraph graph, String label) {
        while (true) {
            String query = prompt(scanner, out, label + ": ");
            if (query == null) return null;
            if (graph.getStops().containsKey(query)) return query;
            if (query.isBlank()) { out.println("Enter a station name or ID."); continue; }
            var matches = graph.findStopsByPartialName(query);
            if (matches.size() == 1) return matches.get(0).getKey();
            if (matches.isEmpty()) out.println("No station found.");
            else {
                out.println("Enter one of these IDs:");
                matches.stream().limit(20).forEach(m -> out.println(m.getKey() + "  " + m.getValue()));
            }
        }
    }
    private static void help(PrintStream out) {
        out.println("""
                Wayline — timetable route planner
                java -jar target/wayline-1.0-SNAPSHOT.jar [options]
                  --data PATH            Operator folders (default: examples/demo)
                  --from ID --to ID --at HH:mm   Search once; otherwise interactive
                  --max-walk METERS      Total walking limit (default: 800)
                  --max-transfers N      Vehicle changes (default: unlimited)
                  --modes BUS,TRAM,METRO,TRAIN   Allowed modes (default: all)
                  --exclude-company NAME        Disable an operator's vehicles
                  --help                 Show this help
                Exit codes: 0 success/quit, 1 no route, 2 invalid input or data.
                """);
    }
}
