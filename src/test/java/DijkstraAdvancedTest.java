import algorithms.Dijkstra;
import algorithms.PathPreferences;
import graph.Edge;
import graph.TransportGraph;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test des cas limites de l'algorithme de Dijkstra
 */
public class DijkstraAdvancedTest {

    private TransportGraph graph;
    private PathPreferences prefs;
    private Map<String, Stop> stops;

    /**
     * Initialisation avant chaque test
     * Crée un graphe de test avec des arrêts, des routes, des trajets et des arêtes de marche
     */
    @BeforeEach
    void setUp() {
        // Création d'un graphe de test pour les cas limites
        stops = new HashMap<>();
        // Arrêts formant un petit réseau
        stops.put("A", new Stop("A", "Station A", 50.8503, 4.3517));
        stops.put("B", new Stop("B", "Station B", 50.8603, 4.3617));
        stops.put("C", new Stop("C", "Station C", 50.8703, 4.3717));
        stops.put("D", new Stop("D", "Station D", 50.8803, 4.3817));
        stops.put("E", new Stop("E", "Station E", 50.8903, 4.3917));
        
        Map<String, Route> routes = new HashMap<>();
        routes.put("R1", new Route("R1", "1", "Bus 1", "3")); // Bus
        routes.put("R2", new Route("R2", "2", "Metro 2", "1")); // Metro
        
        Map<String, Trip> trips = new HashMap<>();
        trips.put("T1", new Trip("T1", "R1")); // Bus
        trips.put("T2", new Trip("T2", "R2")); // Metro
        trips.put("T3", new Trip("T3", "R1")); // Bus (après minuit)
        
        Map<String, List<StopTime>> stopTimes = new HashMap<>();
        
        // Trip T1 : Bus normal en journée
        List<StopTime> tripT1 = new ArrayList<>();
        tripT1.add(new StopTime("T1", "14:00:00", "A", 1));
        tripT1.add(new StopTime("T1", "14:10:00", "B", 2));
        tripT1.add(new StopTime("T1", "14:20:00", "C", 3));
        stopTimes.put("T1", tripT1);
        
        // Trip T2 : Metro normal
        List<StopTime> tripT2 = new ArrayList<>();
        tripT2.add(new StopTime("T2", "14:05:00", "B", 1));
        tripT2.add(new StopTime("T2", "14:15:00", "D", 2));
        stopTimes.put("T2", tripT2);
        
        // Trip T3 : Bus franchissant minuit
        List<StopTime> tripT3 = new ArrayList<>();
        tripT3.add(new StopTime("T3", "23:50:00", "D", 1));
        tripT3.add(new StopTime("T3", "24:10:00", "E", 2)); // 10 minutes après minuit
        stopTimes.put("T3", tripT3);

        graph = new TransportGraph(stops, stopTimes, trips, routes);
        
        // Ajout manuel des arêtes
        graph.generateTransportEdges();
        
        // Ajouter manuellement des arêtes de marche
        addWalkingEdge("A", "B", 500); // 6 minutes de marche
        addWalkingEdge("B", "C", 500);
        addWalkingEdge("C", "D", 500);
        addWalkingEdge("D", "E", 500);
        addWalkingEdge("A", "C", 900); // 11 minutes de marche
        
        // Configuration des préférences par défaut
        prefs = new PathPreferences();
    }
    
    /**
     * Ajouter une arête de marche manuellement avec une distance spécifiée en mètres
     */
    private void addWalkingEdge(String fromId, String toId, double distanceMeters) {
        Stop from = stops.get(fromId);
        Stop to = stops.get(toId);
        double distanceKm = distanceMeters / 1000.0;
        long durationSeconds = (long)(distanceKm / 5.0 * 3600); // 5 km/h
        
        Edge edge1 = new Edge(from, to, durationSeconds, distanceMeters);
        Edge edge2 = new Edge(to, from, durationSeconds, distanceMeters);
        
        graph.addEdge(edge1);
        graph.addEdge(edge2);
    }

    /**
     * Teste le calcul d'un trajet qui traverse minuit
     * Vérifie que l'algorithme gère correctement le passage minuit
     */
    @Test
    void testCrossingMidnight() {
        prefs.setMaxWalkingDistance(0);
        List<Dijkstra.Node> path = Dijkstra.findShortestPath(
            "D", "E", LocalTime.of(23, 45), graph, prefs
        );
        
        assertNotNull(path);
        assertFalse(path.isEmpty());
        
        // Le trajet doit avoir 2 points: D et E
        assertTrue(path.size() >= 2);
        assertEquals("D", path.get(0).stopId);
        assertEquals("E", path.get(path.size() - 1).stopId);
        
        assertEquals(87000, path.get(path.size() - 1).arrivalTimeSeconds);
    }
    
    /**
     * Teste le respect de la distance maximale de marche
     * Vérifie que l'algorithme choisit un chemin plus long mais respectant la limite
     */
    @Test
    void testMaxWalkingDistance() {
        // Configurer une distance de marche maximale restrictive
        prefs.setMaxWalkingDistance(0.6); // 600m max
        
        // Ce trajet ne peut pas être fait uniquement en marchant
        // (900m > 600m)
        List<Dijkstra.Node> path = Dijkstra.findShortestPath(
            "A", "C", LocalTime.of(13, 55), graph, prefs
        );
        
        assertNotNull(path);
        assertFalse(path.isEmpty());
        
        // Le trajet doit passer par B plutôt que faire A->C directement
        assertEquals(3, path.size());
        assertEquals("A", path.get(0).stopId);
        assertEquals("B", path.get(1).stopId);
        assertEquals("C", path.get(2).stopId);
        
        // La distance de marche totale ne doit pas dépasser le maximum
        assertTrue(path.get(path.size()-1).walkingDistance <= prefs.getMaxWalkingDistance() * 1000);
    }
    
    /**
     * Teste le filtrage des modes de transport
     * Vérifie que l'algorithme respecte les préférences de mode de transport
     */
    @Test
    void testTransportModeFiltering() {
        // S'assurer que tous les modes sont d'abord activés
        prefs.setModeAllowed(PathPreferences.TransportMode.BUS, true);
        prefs.setModeAllowed(PathPreferences.TransportMode.TRAM, true);
        prefs.setModeAllowed(PathPreferences.TransportMode.METRO, true);
        prefs.setModeAllowed(PathPreferences.TransportMode.TRAIN, true);
        
        // Trouver un chemin avec tous les modes activés
        List<Dijkstra.Node> fullPath = Dijkstra.findShortestPath(
            "A", "D", LocalTime.of(13, 55), graph, prefs
        );
        
        // Maintenant désactiver le bus
        prefs.setModeAllowed(PathPreferences.TransportMode.BUS, false);
        
        // Chercher un nouveau chemin
        List<Dijkstra.Node> pathNoBus = Dijkstra.findShortestPath(
            "A", "D", LocalTime.of(13, 55), graph, prefs
        );
        
        // Vérifier que les chemins sont toujours possibles
        assertNotNull(fullPath);
        assertFalse(pathNoBus.isEmpty());
        assertTrue(pathNoBus.stream().noneMatch(n -> "BUS".equals(n.transport)));
        assertFalse(fullPath.isEmpty());
        
    }
    
    /**
     * Teste le respect du nombre maximum de changements
     * Vérifie que l'algorithme respecte la limite de changements configurée
     */
    @Test
    void testMaxTransfers() {
        // Configurer un nombre maximum de changements restrictif
        prefs.setMaxTransfers(0);
        
        // Ce trajet nécessite normalement un changement à B pour aller de A à D
        List<Dijkstra.Node> path = Dijkstra.findShortestPath(
            "A", "D", LocalTime.of(13, 55), graph, prefs
        );
        
        assertNotNull(path);
        assertFalse(path.isEmpty());
        
        // Compter le nombre de changements
        int transfers = 0;
        String lastTripId = null;
        
        for (Dijkstra.Node node : path) {
            if (node.tripId != null) {
                if (lastTripId != null && !node.tripId.equals(lastTripId)) {
                    transfers++;
                }
                lastTripId = node.tripId;
            }
        }
        
        assertTrue(transfers <= prefs.getMaxTransfers(), 
            "Le nombre de changements doit être inférieur ou égal au maximum autorisé");
    }
    
    /**
     * Teste le comportement de l'algorithme face à un trajet impossible
     * Vérifie que l'algorithme gère correctement les cas d'erreur
     */
    @Test
    void testImpossiblePath() {
        // Essayer un trajet impossible avec un ID d'arrêt qui n'existe pas dans le graphe
        List<Dijkstra.Node> path = Dijkstra.findShortestPath(
            "A", "NONEXISTENT", LocalTime.of(14, 0), graph, prefs
        );
        assertTrue(path.isEmpty());
    }
}
