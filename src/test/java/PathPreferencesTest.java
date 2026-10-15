import algorithms.PathPreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour la classe PathPreferences qui gère les préférences de l'utilisateur
 * pour le filtrage des modes de transport et des compagnies
 */
public class PathPreferencesTest {

    private PathPreferences prefs;

    @BeforeEach
    void setUp() {
        prefs = new PathPreferences();
    }

    @Test
    void testDefaultPreferences() {
        // Par défaut, tous les modes de transport sont autorisés
        assertTrue(prefs.isModeAllowed(PathPreferences.TransportMode.BUS));
        assertTrue(prefs.isModeAllowed(PathPreferences.TransportMode.TRAM));
        assertTrue(prefs.isModeAllowed(PathPreferences.TransportMode.METRO));
        assertTrue(prefs.isModeAllowed(PathPreferences.TransportMode.TRAIN));
        
        // Par défaut, toutes les compagnies sont autorisées
        assertTrue(prefs.isCompanyAllowed("STIB"));
        assertTrue(prefs.isCompanyAllowed("TEC"));
        assertTrue(prefs.isCompanyAllowed("DELIJN"));
        assertTrue(prefs.isCompanyAllowed("SNCB"));
        
        // Vérifier la casse insensible pour les compagnies
        assertTrue(prefs.isCompanyAllowed("stib"));
        assertTrue(prefs.isCompanyAllowed("Delijn"));
    }
    
    @Test
    void testDisablingTransportModes() {
        // Désactiver un mode de transport
        prefs.setModeAllowed(PathPreferences.TransportMode.METRO, false);
        
        // Vérifier que seulement METRO est désactivé
        assertTrue(prefs.isModeAllowed(PathPreferences.TransportMode.BUS));
        assertTrue(prefs.isModeAllowed(PathPreferences.TransportMode.TRAM));
        assertFalse(prefs.isModeAllowed(PathPreferences.TransportMode.METRO));
        assertTrue(prefs.isModeAllowed(PathPreferences.TransportMode.TRAIN));
        
        // Désactiver tous les modes
        prefs.setModeAllowed(PathPreferences.TransportMode.BUS, false);
        prefs.setModeAllowed(PathPreferences.TransportMode.TRAM, false);
        prefs.setModeAllowed(PathPreferences.TransportMode.TRAIN, false);
        
        // Vérifier que tous les modes sont désactivés
        assertFalse(prefs.isModeAllowed(PathPreferences.TransportMode.BUS));
        assertFalse(prefs.isModeAllowed(PathPreferences.TransportMode.TRAM));
        assertFalse(prefs.isModeAllowed(PathPreferences.TransportMode.METRO));
        assertFalse(prefs.isModeAllowed(PathPreferences.TransportMode.TRAIN));
        
        // Réactiver un mode
        prefs.setModeAllowed(PathPreferences.TransportMode.BUS, true);
        
        // Vérifier que seulement BUS est activé
        assertTrue(prefs.isModeAllowed(PathPreferences.TransportMode.BUS));
        assertFalse(prefs.isModeAllowed(PathPreferences.TransportMode.TRAM));
        assertFalse(prefs.isModeAllowed(PathPreferences.TransportMode.METRO));
        assertFalse(prefs.isModeAllowed(PathPreferences.TransportMode.TRAIN));
    }
    
    @Test
    void testDisablingCompanies() {
        // Désactiver une compagnie
        prefs.setCompanyAllowed("DELIJN", false);
        
        // Vérifier que seulement DELIJN est désactivé
        assertTrue(prefs.isCompanyAllowed("STIB"));
        assertTrue(prefs.isCompanyAllowed("TEC"));
        assertFalse(prefs.isCompanyAllowed("DELIJN"));
        assertTrue(prefs.isCompanyAllowed("SNCB"));
        
        // La vérification doit être insensible à la casse
        assertFalse(prefs.isCompanyAllowed("delijn"));
        
        // Désactiver toutes les compagnies
        prefs.setCompanyAllowed("STIB", false);
        prefs.setCompanyAllowed("TEC", false);
        prefs.setCompanyAllowed("SNCB", false);
        
        // Vérifier que toutes les compagnies sont désactivées
        assertFalse(prefs.isCompanyAllowed("STIB"));
        assertFalse(prefs.isCompanyAllowed("TEC"));
        assertFalse(prefs.isCompanyAllowed("DELIJN"));
        assertFalse(prefs.isCompanyAllowed("SNCB"));
        
        // Réactiver une compagnie
        prefs.setCompanyAllowed("stib", true); // Test avec casse différente
        
        // Vérifier que seulement STIB est activé
        assertTrue(prefs.isCompanyAllowed("STIB"));
        assertFalse(prefs.isCompanyAllowed("TEC"));
        assertFalse(prefs.isCompanyAllowed("DELIJN"));
        assertFalse(prefs.isCompanyAllowed("SNCB"));
    }
    
    @Test
    void testMaxWalkingDistance() {
        // Valeur par défaut
        assertEquals(0.8, prefs.getMaxWalkingDistance());
        
        // Changer la valeur
        prefs.setMaxWalkingDistance(1.2);
        assertEquals(1.2, prefs.getMaxWalkingDistance());
    }
    
    @Test
    void testMaxTransfers() {
        // Valeur par défaut (illimité)
        assertEquals(Integer.MAX_VALUE, prefs.getMaxTransfers());
        
        // Limiter à 2 changements
        prefs.setMaxTransfers(2);
        assertEquals(2, prefs.getMaxTransfers());
    }
}
