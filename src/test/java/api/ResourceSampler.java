package api;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Samples system-wide CPU load and free physical memory in a background thread
 * while an experiment runs. The todo manager runs on the same machine, so its
 * load is included in the system-wide figures.
 */
class ResourceSampler {

    private static final long SAMPLING_INTERVAL_MILLIS = 50;
    private static final double PERCENT = 100.0;
    private static final long BYTES_PER_MB = 1024L * 1024L;

    private final OperatingSystemMXBean operatingSystem =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final List<Double> cpuSamples = Collections.synchronizedList(new ArrayList<>());
    private final List<Long> freeMemorySamples = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean running;
    private Thread thread;

    void start() {
        operatingSystem.getCpuLoad();
        running = true;
        thread = new Thread(this::sampleUntilStopped, "resource-sampler");
        thread.setDaemon(true);
        thread.start();
    }

    ResourceUsage stop() {
        running = false;
        try {
            thread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
        takeSample();
        return summarize();
    }

    private void sampleUntilStopped() {
        while (running) {
            try {
                Thread.sleep(SAMPLING_INTERVAL_MILLIS);
            } catch (InterruptedException exception) {
                return;
            }
            takeSample();
        }
    }

    private void takeSample() {
        double cpuLoad = operatingSystem.getCpuLoad();
        if (cpuLoad >= 0) {
            cpuSamples.add(cpuLoad * PERCENT);
        }
        freeMemorySamples.add(operatingSystem.getFreeMemorySize() / BYTES_PER_MB);
    }

    private ResourceUsage summarize() {
        double averageCpu = cpuSamples.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double peakCpu = cpuSamples.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        long averageFree = (long) freeMemorySamples.stream().mapToLong(Long::longValue).average().orElse(0);
        long minimumFree = freeMemorySamples.stream().mapToLong(Long::longValue).min().orElse(0);
        return new ResourceUsage(averageCpu, peakCpu, averageFree, minimumFree);
    }
}
