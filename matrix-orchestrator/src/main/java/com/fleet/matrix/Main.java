package com.fleet.matrix;

import com.fleet.matrix.db.TelemetryRepository;
import com.fleet.matrix.model.ArmBenchActivityPacket;
import com.fleet.matrix.orchestration.RoboticFulfillmentOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.XReadParams;
import redis.clients.jedis.resps.StreamEntry;
import redis.clients.jedis.StreamEntryID;

import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("  LAUNCHING DISTRIBUTED STREAM PROCESSING BACKEND ENGINE  ");
        System.out.println("=========================================================\n");

        TelemetryRepository repository = new TelemetryRepository();
        RoboticFulfillmentOrchestrator orchestrator = new RoboticFulfillmentOrchestrator(repository);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

        String streamKey = "robot:armbench:telemetry";

// 1. Resolve the Redis service container address dynamically 
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "localhost");

// 2. Pass the resolved variable instead of the hardcoded literal string
        try (Jedis jedis = new Jedis(redisHost, 6379)) {
            System.out.println("[CONNECTED] Ingestion core successfully polling from Redis Stream: " + streamKey);
        
            StreamEntryID lastSeenId = new StreamEntryID("0-0");

            while (true) {
                // Poll any unread items off the messaging bus
                List<Map.Entry<String, List<StreamEntry>>> streams = jedis.xread(
                    XReadParams.xReadParams().block(1000).count(10), 
                    Map.of(streamKey, lastSeenId)
                );

                if (streams == null) continue;

                for (Map.Entry<String, List<StreamEntry>> stream : streams) {
                    for (StreamEntry entry : stream.getValue()) {
                        String rawJson = entry.getFields().get("payload");
                        
                        // JSON string conversion to Object mapping
                        ArmBenchActivityPacket packet = mapper.readValue(rawJson, ArmBenchActivityPacket.class);
                        
                        // Route object into processing layer
                        orchestrator.processActivity(packet);
                        
                        lastSeenId = entry.getID();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[PIPELINE CRITICAL EXCEPTION] Core loop crash: " + e.getMessage());
        }
    }
}
