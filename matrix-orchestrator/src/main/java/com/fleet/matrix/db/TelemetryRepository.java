package com.fleet.matrix.db;

import com.fleet.matrix.model.ArmBenchActivityPacket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class TelemetryRepository {
    
    // 1. Read the host name from Docker's environment, fallback to localhost if running outside Docker
    private static final String DB_HOST = System.getenv().getOrDefault("DB_HOST", "localhost");

// 2. Inject the dynamic host variable into the connection URL string
    private final String url = "jdbc:postgresql://" + DB_HOST + ":5432/fleet_matrix";
    private final String user = "fleet_admin";
    private final String password = "password123";

    // Constructor forces automatic validation/seeding on startup
    public TelemetryRepository() {
        ensureTablesExist();
    }

    private void ensureTablesExist() {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS robotic_pick_history (
                id SERIAL PRIMARY KEY,
                activity_id VARCHAR(100) UNIQUE NOT NULL,
                cell_id VARCHAR(50) NOT NULL,
                product_id VARCHAR(100) NOT NULL,
                has_defect BOOLEAN NOT NULL,
                defect_type VARCHAR(50),
                vision_confidence NUMERIC(4, 3),
                item_count INT NOT NULL,
                recorded_at TIMESTAMP NOT NULL
            );
            CREATE INDEX IF NOT EXISTS idx_recorded_at ON robotic_pick_history(recorded_at);
            """;

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createTableSQL);
            System.out.println("[DATABASE SETUP] Successfully verified or created table structure 'robotic_pick_history'.");
            
        } catch (SQLException e) {
            System.err.println("[DATABASE CRITICAL ERROR] Initialization sequence failed: " + e.getMessage());
        }
    }

    public void saveRecord(ArmBenchActivityPacket packet) {
        String query = "INSERT INTO robotic_pick_history (activity_id, cell_id, product_id, has_defect, defect_type, vision_confidence, item_count, recorded_at) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (activity_id) DO NOTHING";
        
        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, packet.activityId());
            stmt.setString(2, packet.cellId());
            stmt.setString(3, packet.productId());
            stmt.setBoolean(4, packet.hasDefect());
            stmt.setString(5, packet.defectType());
            stmt.setDouble(6, packet.visionConfidence());
            stmt.setInt(7, packet.itemCount());
            
            String rawTimestamp = packet.timestamp();
            if (!rawTimestamp.contains("Z") && !rawTimestamp.contains("+")) {
                rawTimestamp += "Z";
            }
            Instant instant = Instant.parse(rawTimestamp);
            stmt.setTimestamp(8, Timestamp.from(instant));
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DATABASE ERROR] Failed to log tracking node metrics: " + e.getMessage());
        }
    }
}