import algorithms.*;
import graph.*;
import model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RoutingRegressionTest {
    private TransportGraph graph(String... ids) {
        Map<String, Stop> stops = new HashMap<>();
        for (String id : ids) stops.put(id, new Stop(id, id, 50, 4));
        return new TransportGraph(stops, Map.of(), Map.of(), Map.of());
    }
    private void ride(TransportGraph g, String from, String to, int departure, int arrival, String trip) {
        g.addEdge(new Edge(g.getStops().get(from), g.getStops().get(to), departure, arrival, "BUS", trip));
    }
    private void walk(TransportGraph g, String from, String to, long duration, double meters) {
        g.addEdge(new Edge(g.getStops().get(from), g.getStops().get(to), duration, meters));
    }
    private List<Dijkstra.Node> route(TransportGraph g, PathPreferences p) {
        return Dijkstra.findShortestPath("A", "D", LocalTime.MIDNIGHT, g, p);
    }
    private Dijkstra.Node last(List<Dijkstra.Node> nodes) { assertFalse(nodes.isEmpty()); return nodes.get(nodes.size() - 1); }

    @Test void usesTimetableArrivalAndWaitsForConnection() {
        var g = graph("A", "B", "D");
        ride(g, "A", "B", 10, 900, "first");
        ride(g, "B", "D", 899, 950, "missed");
        ride(g, "B", "D", 930, 1000, "second");
        var path = route(g, new PathPreferences());
        assertEquals(1000, last(path).arrivalTimeSeconds);
        assertEquals(30, last(path).waitingTimeSeconds);
        assertEquals(1, last(path).transfers);
    }
    @Test void doesNotRollMissedServicesIntoTomorrow() {
        var g = graph("A", "D"); ride(g, "A", "D", 0, 600, "trip");
        assertTrue(Dijkstra.findShortestPath("A", "D", LocalTime.of(0, 1), g, new PathPreferences()).isEmpty());
    }
    @Test void sameVehicleDoesNotCountAsTransfer() {
        var g = graph("A", "B", "D");
        ride(g,"A","B",10,100,"trip"); ride(g,"B","D",120,200,"trip");
        var p = new PathPreferences(); p.setMaxTransfers(0);
        assertEquals(0, last(route(g,p)).transfers);
        assertEquals(200, last(route(g,p)).arrivalTimeSeconds);
    }
    @Test void walkingDoesNotResetTransferCount() {
        var g = graph("A","B","C","D");
        ride(g,"A","B",0,100,"trip1"); walk(g,"B","C",60,50);
        ride(g,"C","D",160,200,"trip2");
        var p = new PathPreferences(); p.setMaxTransfers(0);
        assertTrue(route(g,p).isEmpty());
        p.setMaxTransfers(1);
        assertEquals(1,last(route(g,p)).transfers);
    }
    @Test void destinationCannotExceedTransferLimit() {
        var g = graph("A","B","D");
        ride(g,"A","B",0,100,"first"); ride(g,"B","D",100,200,"second");
        var p = new PathPreferences(); p.setMaxTransfers(0);
        assertTrue(route(g,p).isEmpty());
    }
    @Test void laterArrivalWithLessWalkingRemainsFeasible() {
        var g = graph("A","B","D");
        walk(g,"A","B",60,700); walk(g,"A","B",120,100); walk(g,"B","D",60,200);
        var p = new PathPreferences(); p.setMaxWalkingDistance(0.8);
        var result = last(route(g,p));
        assertEquals(180,result.arrivalTimeSeconds); assertEquals(300,result.walkingDistance);
    }
    @Test void differentTripContextsAtSameStopAreKept() {
        var g = graph("A","B","D");
        ride(g,"A","B",0,100,"wrong"); ride(g,"A","B",0,150,"through");
        ride(g,"B","D",160,200,"through");
        var p = new PathPreferences(); p.setMaxTransfers(0);
        assertEquals(200,last(route(g,p)).arrivalTimeSeconds);
    }
    @Test void laterLabelWithFewerTransfersIsKeptForSameTrip() {
        var g = graph("A","X","B","D");
        ride(g,"A","X",0,10,"early"); ride(g,"X","B",10,20,"shared");
        ride(g,"A","B",0,30,"shared"); ride(g,"B","D",40,50,"final");
        var p = new PathPreferences(); p.setMaxTransfers(1);
        assertEquals(1,last(route(g,p)).transfers);
    }
    @Test void operatorChangeCountsOnlyOnce() {
        var g = graph("A","B","D");
        ride(g,"A","B",0,100,"CITY-one"); ride(g,"B","D",100,200,"RAIL-two");
        var p = new PathPreferences(); p.setMaxTransfers(1);
        assertEquals(1,last(route(g,p)).transfers);
        p.setCompanyAllowed("RAIL",false); assertTrue(route(g,p).isEmpty());
    }
    @Test void rejectsDisabledModesAndOverBudgetWalking() {
        var g=graph("A","D");ride(g,"A","D",0,100,"trip");walk(g,"A","D",60,1);
        var p=new PathPreferences();p.setMaxWalkingDistance(0);p.setModeAllowed(PathPreferences.TransportMode.BUS,false);
        assertTrue(route(g,p).isEmpty());
    }
    @Test void zeroLengthRouteAndUnreachableStopAreSafe() {
        var g=graph("A","D");
        assertEquals(1,Dijkstra.findShortestPath("A","A",LocalTime.NOON,g,new PathPreferences()).size());
        assertTrue(route(g,new PathPreferences()).isEmpty());
        assertTrue(Dijkstra.findShortestPath("missing","D",LocalTime.NOON,g,new PathPreferences()).isEmpty());
    }
    @Test void searchHonoursTwelveHourHorizon() {
        var g=graph("A","D");ride(g,"A","D",0,43201,"long");
        assertTrue(route(g,new PathPreferences()).isEmpty());
    }
    @Test void preferenceLimitsMustBeFiniteAndNonnegative() {
        var p=new PathPreferences();
        for(double n:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY})
            assertThrows(IllegalArgumentException.class,()->p.setMaxWalkingDistance(n));
        assertThrows(IllegalArgumentException.class,()->p.setMaxTransfers(-1));
        assertTrue(p.isCompanyAllowed("new_operator"));
    }
}
