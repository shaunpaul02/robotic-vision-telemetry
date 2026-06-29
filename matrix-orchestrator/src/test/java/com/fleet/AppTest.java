package com.fleet;

import com.fleet.matrix.model.ArmBenchActivityPacket;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;

/**
 * Unit test suite for the Matrix Telemetry Core.
 * Uses JUnit 5 Jupiter API to match our pom.xml configuration.
 */
public class AppTest {

    @Test
    public void testArmBenchPacketImmutability() {
        // Create a real-world model instance representing an Amazon ARMBench anomaly packet
        ArmBenchActivityPacket packet = new ArmBenchActivityPacket(
            "act_test_100",
            "workcell_sparrow_04",
            "sku_amzn_tote_item_demo",
            true,
            "multi_pick",
            0.985,
            15,
            Instant.now().toString()
        );

        // Verify the Object-Oriented integrity of our Data Record
        assertTrue(packet.hasDefect(), "The vision system should flag a true defect state.");
        assertEquals("multi_pick", packet.defectType(), "The defect type should map exactly to multi_pick.");
        assertEquals(15, packet.itemCount(), "The parsed item count should accurately reflect physical elements.");
    }
}
