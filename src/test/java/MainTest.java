import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
class MainTest {
    private record Result(int code,String output,String error) {}
    private Result run(String input,String...args) {
        var out=new ByteArrayOutputStream();var err=new ByteArrayOutputStream();
        int code=Main.run(args,new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),new PrintStream(out,true,StandardCharsets.UTF_8),new PrintStream(err,true,StandardCharsets.UTF_8));
        return new Result(code,out.toString(StandardCharsets.UTF_8),err.toString(StandardCharsets.UTF_8));
    }
    @Test void helpNeedsNoData() {
        var r=run("","--help","--data","missing");assertEquals(0,r.code);assertTrue(r.output.contains("Wayline"));
    }
    @Test void demoArrivesAtScheduledTimeWithOneTransfer() {
        var r=run("","--from","CITY-A","--to","CITY-D","--at","08:00","--max-walk","0");
        assertEquals(0,r.code,r.error);assertTrue(r.output.contains("Arrival 08:30:00"));
        assertTrue(r.output.contains("Transfers 1"));assertTrue(r.output.contains("wait 180s"));
    }
    @Test void midnightDisplayRetainsDayOffset() {
        var r=run("","--from","CITY-A","--to","CITY-C","--at","23:45","--max-walk","0");
        assertEquals(0,r.code,r.error);assertTrue(r.output.contains("Arrival 00:12:00 (+1d)"));
        assertTrue(r.output.contains("Duration 1620s"));assertTrue(r.output.contains("wait 300s"));
        assertEquals("00:10:00",TransportPlanner.formatTime(600));
    }
    @Test void impossibleRouteReturnsOne() {
        var r=run("","--from","CITY-A","--to","CITY-D","--at","08:00","--max-transfers","0","--max-walk","0");
        assertEquals(1,r.code);assertTrue(r.output.contains("No route"));
    }
    @Test void malformedOptionsFailWithoutStackTrace() {
        for(String[] args:new String[][]{
                {"--from","CITY-A"},{"--at","08:00"},{"--nonsense"},{"--data"},
                {"--max-walk","NaN"},{"--max-walk","-1"},{"--max-transfers","-1"},
                {"--modes","spaceship"},{"--data","missing"},{"--help","--help"},
                {"--from","CITY-A","--to","CITY-C","--at","24:00"},
                {"--from","missing","--to","CITY-C","--at","08:00"}}) {
            var r=run("",args);assertEquals(2,r.code,java.util.Arrays.toString(args));
            assertTrue(r.error.startsWith("Error:"));assertFalse(r.error.contains("at Main."));
        }
    }
    @Test void eofAndQuitAreSafeAtEveryPrompt() {
        for(String input:new String[]{"","q\n","CITY-A\n","CITY-A\nq\n","CITY-A\nCITY-C\n","CITY-A\nCITY-C\nq\n"})
            assertEquals(0,run(input).code);
    }
    @Test void interactiveInputRecoversFromBadTimeAndUnknownStation() {
        var r=run("missing\nWest Park\nEast Campus\ninvalid\n08:00\nq\n","--max-walk","0");
        assertEquals(0,r.code,r.error);assertTrue(r.output.contains("No station found"));
        assertTrue(r.output.contains("Departure must be HH:mm"));assertTrue(r.output.contains("Arrival 08:22:00"));
    }
    @Test void modesAndCompanyExclusionAreApplied() {
        assertEquals(1,run("","--from","CITY-A","--to","CITY-C","--at","08:00","--modes","TRAIN","--max-walk","0").code);
        assertEquals(1,run("","--from","CITY-A","--to","CITY-C","--at","08:00","--exclude-company","city","--max-walk","0").code);
    }
}
