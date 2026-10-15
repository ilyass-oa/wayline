package model;

/**
 * Represente une ligne de bus dans le fichier routes.csv
 */
public class Route {
    private String id;
    private String shortName;
    private String longName;
    private String type;
    
    public Route(String id, String shortName, String longName, String type) {
        this.id = id;
        this.shortName = shortName;
        this.longName = longName;
        this.type = type;
    }
    
    public String getId() {
        return id;
    }
    
    public String getShortName() {
        return shortName;
    }
    
    public String getLongName() {
        return longName;
    }
    
    public String getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return "Route{" +
                "id='" + id + '\'' +
                ", shortName='" + shortName + '\'' +
                ", longName='" + longName + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}