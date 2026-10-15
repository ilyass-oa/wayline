import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import model.Route;

/**
 * Tests pour la classe Route qui représente une ligne de transport
 */
public class RouteTest {
    
    /**
     * Teste la création d'un objet Route avec ses attributs
     */
    @Test
    public void testRouteCreation() {
        Route route = new Route("route123", "1A", "Gare Centrale - Stockel", "BUS");
        
        assertEquals("route123", route.getId());
        assertEquals("1A", route.getShortName());
        assertEquals("Gare Centrale - Stockel", route.getLongName());
        assertEquals("BUS", route.getType());
    }
    
    /**
     * Teste l'égalité entre différents objets Route
     */
    @Test
    public void testRouteEquality() {
        Route route1 = new Route("route123", "1A", "Gare Centrale - Stockel", "BUS");
        Route route2 = new Route("route123", "1A", "Gare Centrale - Stockel", "BUS");
        Route route3 = new Route("route456", "2B", "Gare du Nord - Uccle", "BUS");
        
        assertEquals(route1.getId(), route2.getId());
        assertNotEquals(route1.getId(), route3.getId());
    }
}
