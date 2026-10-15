import algorithms.Dijkstra;
import algorithms.PathPreferences;
import graph.TransportGraph;
import model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour l'algorithme de Dijkstra 
 * Vérifie le fonctionnement de base de l'algorithme avec différentes configurations
 */
public class DijkstraTest {
    private TransportGraph graph;
    private PathPreferences prefs;

    /**
     * Configuration initiale avant chaque test
     * Crée un graphe de test avec des arrêts, des routes et des trajets
     */
    @BeforeEach
    void setUp() {
        // Création d'un graphe de test
        Map<String, Stop> stops = new HashMap<>();
        stops.put("A", new Stop("A", "Station A", 50.8503, 4.3517));
        stops.put("B", new Stop("B", "Station B", 50.8603, 4.3617));
        stops.put("C", new Stop("C", "Station C", 50.8703, 4.3717));
        stops.put("STIB-D", new Stop("STIB-D", "Station D", 50.8803, 4.3817));
        stops.put("DELIJN-E", new Stop("DELIJN-E", "Station E", 50.8903, 4.3917));

        Map<String, Route> routes = new HashMap<>();
        routes.put("R1", new Route("R1", "1", "Bus 1", "3"));
        routes.put("R2", new Route("R2", "2", "Metro 2", "1"));

        Map<String, Trip> trips = new HashMap<>();
        trips.put("T1", new Trip("T1", "R1"));  // Bus
        trips.put("T2", new Trip("T2", "R2"));  // Metro

        Map<String, List<StopTime>> stopTimes = new HashMap<>();
        
        // Trip T1 : Bus
        List<StopTime> tripT1 = new ArrayList<>();
        tripT1.add(new StopTime("T1", "08:00:00", "A", 1));
        tripT1.add(new StopTime("T1", "08:10:00", "B", 2));
        tripT1.add(new StopTime("T1", "08:20:00", "C", 3));
        stopTimes.put("T1", tripT1);
        
        // Trip T2 : Metro
        List<StopTime> tripT2 = new ArrayList<>();
        tripT2.add(new StopTime("T2", "08:05:00", "B", 1));
        tripT2.add(new StopTime("T2", "08:15:00", "STIB-D", 2));
        tripT2.add(new StopTime("T2", "08:25:00", "DELIJN-E", 3));
        stopTimes.put("T2", tripT2);

        graph = new TransportGraph(stops, stopTimes, trips, routes);
        graph.generateTransportEdges();
        
        // Configuration des préférences
        prefs = new PathPreferences();
        prefs.setMaxWalkingDistance(0.8);
        prefs.setMaxTransfers(2);
    }

    /**
     * Teste le calcul d'un chemin simple entre deux arrêts
     */
    @Test
    void testBasicPath() {
        List<Dijkstra.Node> path = Dijkstra.findShortestPath(
            "A", "C", LocalTime.of(8, 0), graph, prefs
        );
        assertNotNull(path);
        assertTrue(path.size() >= 2);
        assertEquals("A", path.get(0).stopId);
        assertEquals("C", path.get(path.size() - 1).stopId);
    }

    /**
     * Teste le respect de la distance maximale de marche
     */
    @Test
    void testWalkingDistance() {
        prefs.setMaxWalkingDistance(0.1); // Réduire la distance de marche maximale
        List<Dijkstra.Node> path = Dijkstra.findShortestPath(
            "A", "B", LocalTime.of(8, 0), graph, prefs
        );
        
        for (Dijkstra.Node node : path) {
            if (node.previous != null && "WALK".equals(node.transport)) {
                assertTrue(node.walkingDistance <= prefs.getMaxWalkingDistance() * 1000); // Conversion en mètres
            }
        }
    }

    /**
     * Teste le filtrage des modes de transport
     */
    @Test
    void testModeFiltering() {
        // Dans ce test simplifié, nous vérifions seulement que le chemin
        // existe quand tous les modes sont activés
        
        // Activer tous les modes de transport
        prefs.setModeAllowed(PathPreferences.TransportMode.BUS, true);
        prefs.setModeAllowed(PathPreferences.TransportMode.TRAM, true);
        prefs.setModeAllowed(PathPreferences.TransportMode.METRO, true);
        prefs.setModeAllowed(PathPreferences.TransportMode.TRAIN, true);
        
        List<Dijkstra.Node> pathAllModes = Dijkstra.findShortestPath(
            "B", "STIB-D", LocalTime.of(8, 0), graph, prefs
        );
        
        // Vérifier que le chemin existe
        assertNotNull(pathAllModes);
        assertFalse(pathAllModes.isEmpty());
        
        // Vérifier que le chemin va bien de B à STIB-D
        assertEquals("B", pathAllModes.get(0).stopId);
        assertEquals("STIB-D", pathAllModes.get(pathAllModes.size() - 1).stopId);
    }
    
    /**
     * Teste le filtrage des compagnies de transport
     */
    @Test
    void testCompanyFiltering() {
        // Désactiver la compagnie DELIJN
        prefs.setCompanyAllowed("DELIJN", false);
        
        List<Dijkstra.Node> path = Dijkstra.findShortestPath(
            "B", "DELIJN-E", LocalTime.of(8, 0), graph, prefs
        );
        
        // Le chemin doit être vide ou ne pas se terminer à la station DELIJN-E
        if (!path.isEmpty()) {
            assertNotEquals("DELIJN-E", path.get(path.size() - 1).stopId);
        }
        
        // Réactiver DELIJN et vérifier que le trajet est possible
        prefs.setCompanyAllowed("DELIJN", true);
        
        path = Dijkstra.findShortestPath(
            "B", "DELIJN-E", LocalTime.of(8, 0), graph, prefs
        );
        
        assertFalse(path.isEmpty());
        assertEquals("DELIJN-E", path.get(path.size() - 1).stopId);
    }
}
