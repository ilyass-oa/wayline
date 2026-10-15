import data_loader.CsvLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class CsvLoaderTest {
    @TempDir Path temp;
    private String file(String csv) throws IOException {
        Path path=Files.createTempFile(temp,"data",".csv");Files.writeString(path,csv);return path.toString();
    }
    @Test void readsReorderedHeadersQuotedCommasQuotesAndNewlines() throws Exception {
        var stops=CsvLoader.loadStops(file("\uFEFFstop_lon,stop_name,stop_id,stop_lat\r\n4,\"Park, \"\"Central\"\"\nNorth\",A,50\r\n"));
        assertEquals("Park, \"Central\"\nNorth",stops.get("A").getName());
        assertEquals(50,stops.get("A").getLatitude());
    }
    @Test void supportsArrivalDepartureAndExtendedHours() throws Exception {
        var times=CsvLoader.loadStopTimes(file("trip_id,arrival_time,departure_time,stop_id,stop_sequence\nT,24:01:00,24:02:00,A,1\n"));
        assertEquals(86460,times.get("T").get(0).getArrivalTimeSeconds());
        assertEquals(86520,times.get("T").get(0).getDepartureTimeSeconds());
    }
    @Test void departureOnlySchemaStillWorksAndSequencesAreSorted() throws Exception {
        var times=CsvLoader.loadStopTimes(file("trip_id,departure_time,stop_id,stop_sequence\nT,08:10:00,B,2\nT,08:00:00,A,1\n"));
        assertEquals("A",times.get("T").get(0).getStopId());
        assertEquals(28800,times.get("T").get(0).getArrivalTimeSeconds());
    }
    @Test void malformedCsvAndDuplicateIdsFailWithContext() throws Exception {
        for(String csv:new String[]{"", "trip_id\nT\n", "trip_id,route_id\nT,R\nT,R\n", "trip_id,route_id\nT\n", "trip_id,route_id\n\"T,R\n", "trip_id,route_id\n\"T\"extra,R\n", "trip_id,trip_id,route_id\nT,T,R\n"}) {
            String path=file(csv);
            var error=assertThrows(IOException.class,()->CsvLoader.loadTrips(path));
            assertTrue(error.getMessage().contains(path));assertTrue(error.getMessage().contains("record"));
        }
    }
    @Test void invalidCoordinatesAndBlankIdsAreRejected() throws Exception {
        for(String row:new String[]{"A,Park,NaN,4", "A,Park,91,4", "A,Park,50,181", ",Park,50,4", "A,,50,4"}) {
            String path=file("stop_id,stop_name,stop_lat,stop_lon\n"+row+"\n");
            assertThrows(IOException.class,()->CsvLoader.loadStops(path));
        }
    }
    @Test void invalidTimesAndDepartureBeforeArrivalAreRejected() throws Exception {
        for(String row:new String[]{"T,08:60:00,09:00:00,A,1", "T,09:00:00,08:00:00,A,1", "T,,08:00:00,A,1", "T,08:00:00,08:00:00,A,-1"}) {
            String path=file("trip_id,arrival_time,departure_time,stop_id,stop_sequence\n"+row+"\n");
            assertThrows(IOException.class,()->CsvLoader.loadStopTimes(path));
        }
    }
}
