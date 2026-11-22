package api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class PerformanceRecorder {

    private static final Path OUTPUT_DIRECTORY = Path.of("target", "performance");
    private static final String HEADER = "entity,operation,objectCount,setupMillis,elapsedSinceStartMillis,"
            + "meanMs,medianMs,p95Ms,maxMs,avgCpuPercent,peakCpuPercent,avgFreeMemoryMb,minFreeMemoryMb";

    private final List<Measurement> measurements = new ArrayList<>();

    void record(Measurement measurement) {
        measurements.add(measurement);
        System.out.printf("    N=%-6d %-7s mean %.2f ms, median %.2f ms, cpu %.1f%%, free memory %d MB%n",
                measurement.objectCount(), measurement.operation(), measurement.meanMillis(),
                measurement.medianMillis(), measurement.usage().averageCpuPercent(),
                measurement.usage().averageFreeMemoryMb());
    }

    Path writeCsv(String fileName) {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        measurements.stream().map(this::toCsvLine).forEach(lines::add);
        try {
            Files.createDirectories(OUTPUT_DIRECTORY);
            return Files.write(OUTPUT_DIRECTORY.resolve(fileName), lines);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write " + fileName, exception);
        }
    }

    private String toCsvLine(Measurement m) {
        ResourceUsage usage = m.usage();
        return String.format(Locale.ROOT, "%s,%s,%d,%d,%d,%.3f,%.3f,%.3f,%.3f,%.2f,%.2f,%d,%d",
                m.entity(), m.operation(), m.objectCount(), m.setupMillis(), m.elapsedSinceStartMillis(),
                m.meanMillis(), m.medianMillis(), m.percentile95Millis(), m.maxMillis(),
                usage.averageCpuPercent(), usage.peakCpuPercent(),
                usage.averageFreeMemoryMb(), usage.minimumFreeMemoryMb());
    }
}
