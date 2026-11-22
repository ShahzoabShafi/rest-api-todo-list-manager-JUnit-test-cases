package api;

import java.util.List;

record Measurement(String entity, String operation, int objectCount, long setupMillis,
                   long elapsedSinceStartMillis, List<Double> durationsMillis, ResourceUsage usage) {

    double meanMillis() {
        return durationsMillis.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    double medianMillis() {
        return percentile(0.50);
    }

    double percentile95Millis() {
        return percentile(0.95);
    }

    double maxMillis() {
        return durationsMillis.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    private double percentile(double fraction) {
        List<Double> sorted = durationsMillis.stream().sorted().toList();
        int index = (int) Math.min(sorted.size() - 1, Math.floor(fraction * sorted.size()));
        return sorted.get(index);
    }
}
