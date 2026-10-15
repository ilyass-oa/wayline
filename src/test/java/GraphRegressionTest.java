import graph.*;
import model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class GraphRegressionTest {
    private Map<String,Stop> stops(double lat1,double lon1,double lat2,double lon2) {
        return Map.of("A",new Stop("A","A",lat1,lon1),"B",new Stop("B","B",lat2,lon2));
    }
    @Test void coincidentCoordinatesDoNotCrashOrDuplicateWalks() {
        var g=new TransportGraph(stops(50,4,50,4),Map.of(),Map.of(),Map.of());
        assertEquals(2,g.generateWalkingEdges(800));assertEquals(0,g.generateWalkingEdges(800));
        assertEquals(1,g.getOutgoingEdges("A").size());
        assertTrue(g.getOutgoingEdges("A").get(0).getDurationSeconds()>0);
        assertThrows(UnsupportedOperationException.class,()->g.getOutgoingEdges("A").clear());
    }
    @Test void walkingRadiusIsExactAndWorksAcrossDateLine() {
        var g=new TransportGraph(stops(0,179.999,0,-179.999),Map.of(),Map.of(),Map.of());
        assertEquals(0,g.generateWalkingEdges(200));assertEquals(2,g.generateWalkingEdges(300));
        assertEquals(0,g.generateWalkingEdges(300));
    }
    @Test void zeroRadiusAndInvalidRadiusAreHandled() {
        var g=new TransportGraph(stops(50,4,50,4),Map.of(),Map.of(),Map.of());
        assertEquals(0,g.generateWalkingEdges(0));
        assertThrows(IllegalArgumentException.class,()->g.generateWalkingEdges(Double.NaN));
    }
    private TransportGraph scheduled(List<StopTime> times) {
        return new TransportGraph(stops(50,4,50.1,4.1),Map.of("T",times),Map.of("T",new Trip("T","R")),Map.of("R",new Route("R","1","Bus","3")));
    }
    @Test void scheduledEdgesUseActualArrivalAndAreIdempotent() {
        var g=scheduled(List.of(new StopTime("T","08:00:00","A",1),new StopTime("T","08:20:00","08:25:00","B",2)));
        assertEquals(1,g.generateTransportEdges());assertEquals(0,g.generateTransportEdges());
        var edge=g.getOutgoingEdges("A").get(0);
        assertEquals(30000,edge.getArrivalSeconds());assertEquals(1200,edge.getDurationSeconds());
    }
    @Test void backwardsTimesDuplicateSequencesAndUnknownStopsFail() {
        for(var times:List.of(
                List.of(new StopTime("T","23:50:00","A",1),new StopTime("T","00:10:00","B",2)),
                List.of(new StopTime("T","08:00:00","A",1),new StopTime("T","08:10:00","B",1)),
                List.of(new StopTime("T","08:00:00","missing",1)))) {
            var g=scheduled(times);assertThrows(IllegalArgumentException.class,g::generateTransportEdges);
            assertTrue(g.getOutgoingEdges("A").isEmpty());
        }
    }
    @Test void serviceDayTimesDoNotWrap() {
        var g=scheduled(List.of(new StopTime("T","23:50:00","A",1),new StopTime("T","24:10:00","B",2)));
        g.generateTransportEdges();assertEquals(87000,g.getOutgoingEdges("A").get(0).getArrivalSeconds());
    }
    @Test void invalidCoordinatesFailBeforeIndexing() {
        assertThrows(IllegalArgumentException.class,()->new TransportGraph(stops(Double.NaN,4,50,4),Map.of(),Map.of(),Map.of()));
    }
}
