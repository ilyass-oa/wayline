package algorithms;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class PathPreferences {
    public enum TransportMode { BUS, TRAM, METRO, TRAIN }
    private double maxWalkingDistance = 0.8;
    private int maxTransfers = Integer.MAX_VALUE;
    private final Set<TransportMode> allowedModes = EnumSet.allOf(TransportMode.class);
    private final Set<String> blockedCompanies = new HashSet<>();

    public void setMaxWalkingDistance(double km) {
        if (!Double.isFinite(km) || km < 0) throw new IllegalArgumentException("Walking distance must be finite and nonnegative");
        maxWalkingDistance = km;
    }
    public void setMaxTransfers(int count) {
        if (count < 0) throw new IllegalArgumentException("Transfers must be nonnegative");
        maxTransfers = count;
    }
    public double getMaxWalkingDistance() { return maxWalkingDistance; }
    public int getMaxTransfers() { return maxTransfers; }
    public boolean isModeAllowed(TransportMode mode) { return allowedModes.contains(Objects.requireNonNull(mode)); }
    public void setModeAllowed(TransportMode mode, boolean allowed) {
        Objects.requireNonNull(mode);
        if (allowed) allowedModes.add(mode); else allowedModes.remove(mode);
    }
    public boolean isCompanyAllowed(String company) { return !blockedCompanies.contains(normalize(company)); }
    public void setCompanyAllowed(String company, boolean allowed) {
        String name = normalize(company);
        if (allowed) blockedCompanies.remove(name); else blockedCompanies.add(name);
    }
    private static String normalize(String company) {
        if (company == null || company.isBlank()) throw new IllegalArgumentException("Company must not be blank");
        return company.trim().toUpperCase(Locale.ROOT);
    }
}
