package com.fleet.matrix.model;

//import java.time.Instant;

public record ArmBenchActivityPacket(
    String activityId,
    String cellId,
    String productId,
    boolean hasDefect,
    String defectType,
    double visionConfidence,
    int itemCount,
    String timestamp
) {

}