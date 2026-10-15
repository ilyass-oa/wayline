package model;

/**
 * Represente un voyage dans le fichier trips.csv
 */
public class Trip {
    private String id;
    private String routeId;
    
    public Trip(String id, String routeId) {
        this.id = id;
        this.routeId = routeId;
    }
    
    public String getId() {
        return id;
    }
    
    public String getRouteId() {
        return routeId;
    }
    
    @Override
    public String toString() {
        return "Trip{" +
                "id='" + id + '\'' +
                ", routeId='" + routeId + '\'' +
                '}';
    }
}