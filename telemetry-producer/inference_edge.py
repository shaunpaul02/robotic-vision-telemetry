import cv2
import json
import time
import os
import numpy as np
from redis import Redis
from datetime import datetime, timezone
from datasets import load_dataset

# Docker Environment Router (Uses service names if in container, else localhost)
REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
redis_client = Redis(host=REDIS_HOST, port=6379, decode_responses=True)
STREAM_KEY = "robot:armbench:telemetry"

print("=================================================================")
print(f"  CONNECTING TO EVENT BUS AT: {REDIS_HOST}                      ")
print("=================================================================\n")

# Connect to the Amazon ARMBench streaming data pointer
dataset = load_dataset("correll/armbench-segmentation-mix-object-tote", split="train", streaming=True)

# Session flag to ensure the diagnostic image is only generated ONCE per run
diagnostic_saved = False

# --- NETWORK RESILIENCE LAYER ---
# Outer loop keeps the edge node alive indefinitely, even if the cloud drops connection
while True:
    try:
        print("\n[NETWORK] Connecting to Amazon ARMBench upstream dataset...")
        dataset = load_dataset("correll/armbench-segmentation-mix-object-tote", split="train", streaming=True)
        
        for i, record in enumerate(dataset):
            try:
                pil_image = record['rgb']
                frame = cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
                
                # --- Upgraded Computer Vision Processing Matrix ---
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                blurred = cv2.GaussianBlur(gray, (7, 7), 0) 
                edged = cv2.Canny(blurred, 20, 120)
                
                # MORPHOLOGICAL CLOSING
                kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (25, 25))
                closed_edges = cv2.morphologyEx(edged, cv2.MORPH_CLOSE, kernel)
                
                contours, _ = cv2.findContours(closed_edges.copy(), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
                # --------------------------------------------------
                
                detected_item_count = 0
                max_bounding_area = 0.0
                diagnostic_canvas = frame.copy()
                
                for c in contours:
                    area = cv2.contourArea(c)
                    if area > 5000: # Threshold optimized for morphological closing
                        detected_item_count += 1
                        max_bounding_area = max(max_bounding_area, area)
                        x, y, w, h = cv2.boundingRect(c)
                        cv2.rectangle(diagnostic_canvas, (x, y), (x + w, y + h), (0, 255, 0), 2)
                
                anomaly_flag = detected_item_count > 14 or max_bounding_area > 95000
                
                # --- OPTION C VISUALIZATION: GENERATE ONCE PER SESSION ---
                if not diagnostic_saved and detected_item_count >= 3:
                    try:
                        print("[VISUALIZATION] Rendering session portfolio diagnostic snapshot...")
                        preview_raw = cv2.resize(frame, (400, 300))
                        preview_diag = cv2.resize(diagnostic_canvas, (400, 300))
                        
                        comparison_panel = np.hstack((preview_raw, preview_diag))
                        cv2.putText(comparison_panel, "Raw Bin", (10, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
                        cv2.putText(comparison_panel, "CV Target Grid", (410, 25), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
                        
                        share_dir = "/workspace_share"
                        target_path = os.path.join(share_dir, "armbench_diagnostic.png") if os.path.exists(share_dir) else "armbench_diagnostic.png"
                        
                        cv2.imwrite(target_path, comparison_panel)
                        print(f"[SUCCESS] Saved static portfolio example to shared volume: {target_path}")
                        diagnostic_saved = True 
                        
                    except Exception as write_error:
                        print(f"[WARNING] Shared volume write failed ({write_error}). Invoking self-healing fallback...")
                        try:
                            cv2.imwrite("armbench_diagnostic.png", comparison_panel)
                            print("[SUCCESS] Saved fallback portfolio image safely to local container space.")
                            diagnostic_saved = True
                        except Exception as local_error:
                            print(f"[ERROR] Visual diagnostic bypass triggered: {local_error}")
                            diagnostic_saved = True
                # --------------------------------------------------------

                telemetry_payload = {
                    "activityId": f"act_armbench_{i}_{int(time.time())}",
                    "cellId": "workcell_sparrow_04",
                    "productId": f"sku_amzn_tote_item_{i}",
                    "hasDefect": anomaly_flag,
                    "defectType": "multi_pick" if anomaly_flag else "none",
                    "visionConfidence": round(float(min(max_bounding_area / 180000.0, 1.0)), 3),
                    "itemCount": detected_item_count,
                    "timestamp": datetime.now(timezone.utc).isoformat()
                }
                
                redis_client.xadd(STREAM_KEY, {"payload": json.dumps(telemetry_payload)})
                print(f"[STREAMING LOG] Frame #{i} | Items Found: {detected_item_count} | Anomaly: {anomaly_flag}")
                
                time.sleep(2)
                
            except Exception as e:
                print(f"[ERROR] Stream pipeline anomaly: {e}")
                time.sleep(5)
                
    except Exception as network_error:
        # Catches the 504 Gateway Timeouts and re-loops instead of crashing!
        print(f"\n[CRITICAL WARNING] Upstream connection lost: {network_error}")
        print("[NETWORK] Engaging self-healing protocols. Retrying connection in 10 seconds...\n")
        time.sleep(10)
        
