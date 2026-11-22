package api;

record ResourceUsage(double averageCpuPercent, double peakCpuPercent,
                     long averageFreeMemoryMb, long minimumFreeMemoryMb) {
}
