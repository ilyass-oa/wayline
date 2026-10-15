import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.Stop;

/**
 * Tests pour la classe Stop qui représente un arrêt de transport
 */
public class StopTest {
    
    /**
     * Teste la création d'un objet Stop avec ses attributs
     */
    @Test
    public void testStopCreation() {
        Stop stop = new Stop("stop123", "Gare Centrale", 50.8466, 4.3528);
        
        assertEquals("stop123", stop.getId());
        assertEquals("Gare Centrale", stop.getName());
        assertEquals(50.8466, stop.getLatitude(), 0.0001);
        assertEquals(4.3528, stop.getLongitude(), 0.0001);
    }
    
    /**
     * Teste l'égalité entre différents objets Stop
     */
    @Test
    public void testStopEquality() {
        Stop stop1 = new Stop("stop123", "Gare Centrale", 50.8466, 4.3528);
        Stop stop2 = new Stop("stop123", "Gare Centrale", 50.8466, 4.3528);
        Stop stop3 = new Stop("stop456", "Gare du Nord", 50.8466, 4.3528);
        
        assertEquals(stop1.getId(), stop2.getId());
        assertNotEquals(stop1.getId(), stop3.getId());
    }
}
