import algorithms.Dijkstra;
import algorithms.PathPreferences;
import graph.TransportGraph;
import java.io.PrintStream;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/** Route search and plain-text itinerary formatting. */
public final class TransportPlanner {
    private final TransportGraph graph;
    public TransportPlanner(TransportGraph graph) { this.graph = graph; }
    public List<Dijkstra.Node> findRoute(String from, String to, LocalTime departure) {
        return findRoute(from, to, departure, new PathPreferences());
    }
    public List<Dijkstra.Node> findRoute(String from, String to, LocalTime departure, PathPreferences prefs) {
        return Dijkstra.findShortestPath(from, to, departure, graph, prefs);
    }
    static String formatTime(int seconds) {
        int day = seconds / 86400;
        return String.format(Locale.ROOT, "%02d:%02d:%02d%s", seconds / 3600 % 24, seconds / 60 % 60,
                seconds % 60, day == 0 ? "" : " (+" + day + "d)");
    }
    public void printRoute(List<Dijkstra.Node> route) { printRoute(route, System.out); }
    public void printRoute(List<Dijkstra.Node> route, PrintStream out) {
        if (route.isEmpty()) { out.println("No route matches these preferences within 12 hours."); return; }
        for (int i = 1; i < route.size(); i++) {
            Dijkstra.Node from = route.get(i - 1), to = route.get(i);
            out.printf(Locale.ROOT, "%s  %s %s -> %s %s", to.transport,
                    graph.getStops().get(from.stopId).getName(),
                    formatTime(from.arrivalTimeSeconds + to.waitingTimeSeconds),
                    graph.getStops().get(to.stopId).getName(), formatTime(to.arrivalTimeSeconds));
            if (to.waitingTimeSeconds > 0) out.printf(Locale.ROOT, " (wait %ds)", to.waitingTimeSeconds);
            out.println();
        }
        Dijkstra.Node first = route.get(0), last = route.get(route.size() - 1);
        out.printf(Locale.ROOT, "Arrival %s | Duration %ds | Transfers %d | Walking %.0fm%n",
                formatTime(last.arrivalTimeSeconds), last.arrivalTimeSeconds - first.arrivalTimeSeconds,
                last.transfers, last.walkingDistance);
    }
}
