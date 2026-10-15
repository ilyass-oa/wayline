import data_loader.NetworkLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class NetworkLoaderTest {
    @TempDir Path temp;
    private Path operator(String name) throws Exception {
        Path d=Files.createDirectories(temp.resolve(name));
        Files.writeString(d.resolve("stops.csv"),"stop_id,stop_name,stop_lat,stop_lon\nA,Park,50,4\nB,Square,50.1,4.1\n");
        Files.writeString(d.resolve("routes.csv"),"route_id,route_short_name,route_long_name,route_type\nR,1,Bus,3\n");
        Files.writeString(d.resolve("trips.csv"),"trip_id,route_id\nT,R\n");
        Files.writeString(d.resolve("stop_times.csv"),"trip_id,departure_time,stop_id,stop_sequence\nT,08:00:00,A,1\nT,08:20:00,B,2\n");
        return d;
    }
    @Test void localOperatorIdsAreNamespacedWithoutOverwriting() throws Exception {
        operator("CITY");operator("RAIL");var g=NetworkLoader.load(temp,0);
        assertEquals(4,g.getStops().size());assertEquals(2,g.getTrips().size());
        assertEquals("CITY-T",g.getOutgoingEdges("CITY-A").get(0).getTripId());
        assertEquals("RAIL-B",g.getOutgoingEdges("RAIL-A").get(0).getTo().getId());
    }
    @Test void missingDirectoryAndEmptyNetworkFail() {
        assertThrows(IOException.class,()->NetworkLoader.load(temp.resolve("missing"),0));
        assertThrows(IOException.class,()->NetworkLoader.load(temp,0));
    }
    @Test void missingFileFailsClearly() throws Exception {
        Path d=operator("CITY");Files.delete(d.resolve("trips.csv"));
        var e=assertThrows(IOException.class,()->NetworkLoader.load(temp,0));assertTrue(e.getMessage().contains("trips.csv"));
    }
    @Test void danglingRouteAndStopReferencesAreRejected() throws Exception {
        Path d=operator("CITY");
        Files.writeString(d.resolve("trips.csv"),"trip_id,route_id\nT,missing\n");
        assertThrows(IOException.class,()->NetworkLoader.load(temp,0));
        Files.writeString(d.resolve("trips.csv"),"trip_id,route_id\nT,R\n");
        Files.writeString(d.resolve("stop_times.csv"),"trip_id,departure_time,stop_id,stop_sequence\nT,08:00:00,missing,1\n");
        assertThrows(IOException.class,()->NetworkLoader.load(temp,0));
    }
    @Test void namespaceCollisionsAreRejected() throws Exception {
        Path d=operator("CITY");
        Files.writeString(d.resolve("stops.csv"),"stop_id,stop_name,stop_lat,stop_lon\nA,Park,50,4\nCITY-A,Square,50.1,4.1\n");
        var e=assertThrows(IOException.class,()->NetworkLoader.load(temp,0));assertTrue(e.getMessage().contains("Duplicate namespaced stop"));
    }
    @Test void headerOnlyTimetableIsNotReportedAsUsable() throws Exception {
        Path d=operator("CITY");Files.writeString(d.resolve("stop_times.csv"),"trip_id,departure_time,stop_id,stop_sequence\n");
        assertThrows(IOException.class,()->NetworkLoader.load(temp,0));
    }
}
