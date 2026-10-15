import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.Trip;

/**
 * Tests pour la classe Trip qui représente un trajet
 */
public class TripTest {
    
    /**
     * Teste la création d'un objet Trip avec ses attributs
     */
    @Test
    public void testTripCreation() {
        Trip trip = new Trip("trip123", "route456");
        
        assertEquals("trip123", trip.getId());
        assertEquals("route456", trip.getRouteId());
    }
    
    /**
     * Teste l'égalité entre différents objets Trip
     */
    @Test
    public void testTripEquality() {
        Trip trip1 = new Trip("trip123", "route456");
        Trip trip2 = new Trip("trip123", "route456");
        Trip trip3 = new Trip("trip789", "route456");
        
        assertEquals(trip1.getId(), trip2.getId());
        assertNotEquals(trip1.getId(), trip3.getId());
    }
}
