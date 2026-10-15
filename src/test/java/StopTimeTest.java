import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.StopTime;
import java.time.LocalTime;

/**
 * Teste pour la classe StopTime qui représente un horaire
 */
public class StopTimeTest {
    
    /**
     * Teste la creation d'un objet StopTime
     */
    @Test
    public void testStopTimeCreation() {
        StopTime stopTime = new StopTime("trip123", "14:30:00", "stop456", 1);
        
        assertEquals("trip123", stopTime.getTripId());
        assertEquals("14:30:00", stopTime.getDepartureTime());
        assertEquals("stop456", stopTime.getStopId());
        assertEquals(1, stopTime.getStopSequence());
    }
    
    /**
     * Teste la conversion de l'heure de départ en secondes et en LocalTime
     */
    @Test
    public void testDepartureTimeConversion() {
        StopTime stopTime = new StopTime("trip123", "14:30:45", "stop456", 1);
        
        // Test conversion to seconds
        int expectedSeconds = 14 * 3600 + 30 * 60 + 45; // 14h30m45s
        assertEquals(expectedSeconds, stopTime.getDepartureTimeSeconds());
        
        // Test conversion to LocalTime
        LocalTime expectedTime = LocalTime.of(14, 30, 45);
        assertEquals(expectedTime, stopTime.getDepartureLocalTime());
    }
    
    /**
     * Teste la comparaison entre différents objets StopTime
     */
    @Test
    public void testStopTimeComparison() {
        StopTime stopTime1 = new StopTime("trip123", "14:30:00", "stop456", 1);
        StopTime stopTime2 = new StopTime("trip123", "14:30:00", "stop789", 2);
        StopTime stopTime3 = new StopTime("trip123", "15:00:00", "stop012", 3);
        
        // La méthode compareTo compare les heures de départ, pas les séquences
        assertEquals(0, stopTime1.compareTo(stopTime2)); // Même heure de départ
        assertTrue(stopTime2.compareTo(stopTime3) < 0);  // 14:30 < 15:00
        assertTrue(stopTime1.compareTo(stopTime3) < 0);  // 14:30 < 15:00
    }
    @Test void extendedTimesAndOverflowValidation() {
        assertEquals(90000, StopTime.parseTime("25:00:00"));
        for (String value : new String[]{"-1:00:00", "08:60:00", "08:00:60", "08:00", "99999999999999999:00:00", "9999999:00:00"})
            assertThrows(IllegalArgumentException.class, () -> StopTime.parseTime(value));
        assertThrows(IllegalArgumentException.class, () -> new StopTime("T", "09:00:00", "08:00:00", "A", 1));
    }
}
