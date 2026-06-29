package com.fleet.matrix.orchestration;

import com.fleet.matrix.db.TelemetryRepository;
import com.fleet.matrix.model.ArmBenchActivityPacket;
import java.util.concurrent.atomic.AtomicInteger;

public class RoboticFulfillmentOrchestrator {
    private final TelemetryRepository repository;
    private final AtomicInteger consecutiveFaults = new AtomicInteger(0);
    private static final int EMERGENCY_THRESHOLD = 3;

    public RoboticFulfillmentOrchestrator(TelemetryRepository repository) {
        this.repository = repository;
    }

    public synchronized void processActivity(ArmBenchActivityPacket packet) {
        // 1. Commit the stream packet directly to PostgreSQL
        repository.saveRecord(packet);

        // 2. Real-time Anomaly Parsing Logic
        if (packet.hasDefect()) {
            int currentFaults = consecutiveFaults.incrementAndGet();
            System.out.printf("[WARNING] Anomaly identified at workcell %s! Defect Type: %s. Consecutive Count: %d%n",
                    packet.cellId(), packet.defectType(), currentFaults);

            if (currentFaults >= EMERGENCY_THRESHOLD) {
                triggerSafetyHaltProtocol(packet.cellId(), packet.defectType());
            }
        } else {
            // Reset state loop metrics on passing scans
            consecutiveFaults.set(0);
        }
    }

    private void triggerSafetyHaltProtocol(String cellId, String defectType) {
        System.err.printf("[CRITICAL AUTOMATION ALERT] Emergency halt issued on cell %s! %d consecutive '%s' failures reached. Rerouting incoming tote lines downstream.%n",
                cellId, EMERGENCY_THRESHOLD, defectType);
    }
}