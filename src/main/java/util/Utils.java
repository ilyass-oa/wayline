package util;

public final class Utils {
    private Utils() {}
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        if (!Double.isFinite(lat1) || !Double.isFinite(lat2) || !Double.isFinite(lon1) || !Double.isFinite(lon2) ||
                Math.abs(lat1) > 90 || Math.abs(lat2) > 90 || Math.abs(lon1) > 180 || Math.abs(lon2) > 180) {
            throw new IllegalArgumentException("Invalid coordinates");
        }
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 6371 * 2 * Math.asin(Math.sqrt(Math.max(0, Math.min(1, a))));
    }
    public static long walkingTimeInSeconds(double km) {
        if (!Double.isFinite(km) || km < 0) throw new IllegalArgumentException("Invalid walking distance");
        return Math.max(60, (long) Math.ceil(km / 5 * 3600));
    }
}
