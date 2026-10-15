package algorithms;

import graph.Edge;
import graph.TransportGraph;
import java.time.LocalTime;
import java.util.*;

/** Earliest arrival with nondominated labels for time, transfers and total walking. */
public final class Dijkstra {
    public static final int MAX_DURATION_SECONDS = 12 * 3600;
    private Dijkstra() {}

    public static final class Node {
        public final int stopIdx;
        public final String stopId;
        public final int arrivalTimeSeconds;
        public final LocalTime arrivalTime;
        public final String tripId;
        public final String transport;
        public final Node previous;
        public final int transfers;
        public final double walkingDistance;
        public final int waitingTimeSeconds;
        private final String lastBoardedTrip;

        public Node(int stopIdx, String stopId, int arrivalTimeSeconds, String tripId, String transport,
                    Node previous, int transfers, double walkingDistance, int waitingTimeSeconds) {
            this.stopIdx = stopIdx;
            this.stopId = stopId;
            this.arrivalTimeSeconds = arrivalTimeSeconds;
            this.arrivalTime = LocalTime.ofSecondOfDay(Math.floorMod(arrivalTimeSeconds, 86400));
            this.tripId = tripId;
            this.transport = transport;
            this.previous = previous;
            this.transfers = transfers;
            this.walkingDistance = walkingDistance;
            this.waitingTimeSeconds = waitingTimeSeconds;
            this.lastBoardedTrip = tripId != null ? tripId : previous == null ? null : previous.lastBoardedTrip;
        }
        public Node(int idx, String id, int seconds, String trip, String mode, Node previous, int transfers, double walk) {
            this(idx, id, seconds, trip, mode, previous, transfers, walk, 0);
        }
    }

    private record State(int stop, String lastTrip) {}

    public static List<Node> findShortestPath(String start, String end, LocalTime departure,
                                              TransportGraph graph, PathPreferences prefs) {
        Objects.requireNonNull(departure);
        Objects.requireNonNull(graph);
        Objects.requireNonNull(prefs);
        Map<String, Integer> indices = graph.getStopIdToIndex();
        if (!indices.containsKey(start) || !indices.containsKey(end)) return List.of();
        int startSeconds = departure.toSecondOfDay();
        long deadline = (long) startSeconds + MAX_DURATION_SECONDS;
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt((Node n) -> n.arrivalTimeSeconds)
                .thenComparingInt(n -> n.transfers).thenComparingDouble(n -> n.walkingDistance));
        Map<State, List<Node>> labels = new HashMap<>();
        Node first = new Node(indices.get(start), start, startSeconds, null, "START", null, 0, 0);
        accept(first, labels, queue);

        while (!queue.isEmpty()) {
            Node current = queue.remove();
            State key = new State(current.stopIdx, current.lastBoardedTrip);
            if (!labels.get(key).contains(current)) continue;
            if (current.stopId.equals(end)) return buildPath(current);
            for (Edge edge : graph.getOutgoingEdgesByIndex(current.stopIdx)) {
                boolean walk = "WALK".equals(edge.getMode());
                if (!walk && (!modeAllowed(edge, prefs) || !companyAllowed(edge, prefs))) continue;
                double distance = current.walkingDistance + (walk ? edge.getWalkingDistanceMeters() : 0);
                if (distance > prefs.getMaxWalkingDistance() * 1000) continue;
                int transfers = current.transfers;
                if (!walk && current.lastBoardedTrip != null && !current.lastBoardedTrip.equals(edge.getTripId())) transfers++;
                if (transfers > prefs.getMaxTransfers()) continue;
                long arrival;
                int waiting = 0;
                if (walk) {
                    arrival = current.arrivalTimeSeconds + edge.getDurationSeconds();
                } else {
                    if (edge.getDepartureSeconds() < current.arrivalTimeSeconds) continue;
                    waiting = edge.getDepartureSeconds() - current.arrivalTimeSeconds;
                    arrival = edge.getArrivalSeconds();
                }
                if (arrival > deadline || arrival < current.arrivalTimeSeconds) continue;
                Node next = new Node(indices.get(edge.getTo().getId()), edge.getTo().getId(), (int) arrival,
                        edge.getTripId(), edge.getMode(), current, transfers, distance, waiting);
                accept(next, labels, queue);
            }
        }
        return List.of();
    }

    private static boolean modeAllowed(Edge edge, PathPreferences prefs) {
        try { return prefs.isModeAllowed(PathPreferences.TransportMode.valueOf(edge.getMode())); }
        catch (IllegalArgumentException e) { return false; }
    }

    private static boolean companyAllowed(Edge edge, PathPreferences prefs) {
        // Loaded datasets namespace every ID with its operator. Manually built graphs can omit it.
        for (String id : List.of(edge.getTripId(), edge.getFrom().getId(), edge.getTo().getId())) {
            int dash = id.indexOf('-');
            if (dash > 0 && !prefs.isCompanyAllowed(id.substring(0, dash))) return false;
        }
        return true;
    }

    private static boolean dominates(Node a, Node b) {
        return a.arrivalTimeSeconds <= b.arrivalTimeSeconds && a.transfers <= b.transfers
                && a.walkingDistance <= b.walkingDistance;
    }

    private static void accept(Node candidate, Map<State, List<Node>> labels, PriorityQueue<Node> queue) {
        List<Node> atState = labels.computeIfAbsent(new State(candidate.stopIdx, candidate.lastBoardedTrip), k -> new ArrayList<>());
        if (atState.stream().anyMatch(n -> dominates(n, candidate))) return;
        atState.removeIf(n -> dominates(candidate, n));
        atState.add(candidate);
        queue.add(candidate);
    }

    private static List<Node> buildPath(Node end) {
        LinkedList<Node> path = new LinkedList<>();
        for (Node node = end; node != null; node = node.previous) path.addFirst(node);
        return List.copyOf(path);
    }
}
