# Real-Time Industrial Vision & Telemetry Pipeline

An end-to-end, containerized microservices platform designed for warehouse automation. This system streams real-world robotic workcell data, uses computer vision to detect packages, and automatically triggers safety-halt protocols if it detects a sorting anomaly. 

## 🚀 Skills & Technologies Demonstrated

* **Machine Vision (Python, OpenCV):** Developed spatial filtering, morphological closing, and contour detection to isolate overlapping items in complex lighting.
* **Backend Orchestration (Java 21):** Built a distributed, self-healing orchestration engine that evaluates data streams for critical safety faults.
* **Distributed Systems (Redis Streams):** Implemented an asynchronous event broker to decouple the heavy computer vision processing from the backend database.
* **Database Design (PostgreSQL):** Automated the creation of structured, time-series tables to log long-term tracking metrics.
* **DevOps & Security (Docker & Docker Compose):** Packaged the entire multi-tier system into portable, reproducible containers utilizing secure, non-root user permissions.
* **Fault Tolerance:** Engineered auto-retry network protocols and self-healing local storage fallbacks to ensure the pipeline never crashes during cloud disconnects.

---

## 🏗️ System Architecture

This project is built using a decoupled microservices architecture. It consists of four isolated Docker containers working together in real-time:

1. **Edge Perception Node (Python):** Streams actual 2448x2048 bin images from the Amazon ARMBench dataset. It runs a custom OpenCV algorithm to draw precise bounding boxes around distinct packages while ignoring background structural noise.
2. **Event Bus (Redis):** Acts as a shock-absorber. It catches the fast, unpredictable telemetry outputs from the vision node and organizes them into a clean queue.
3. **Orchestrator Engine (Java):** Continuously polls the event bus. It parses the payload, counts the items, and tracks consecutive failures to issue a `[CRITICAL AUTOMATION ALERT]` if a robotic cell becomes unsafe.
4. **Persistence Layer (PostgreSQL):** Saves every single transaction into a structured relational database for long-term analytical reporting.

---

## 📊 Computer Vision Diagnostic Output

To verify the algorithm's accuracy, the Python edge node automatically generates a visual diagnostic image on its first successful pass. 

![Amazon ARMBench Vision Diagnostic Pipeline](armbench_diagnostic.png)

* **Left (Raw Bin):** The raw, unstructured image of an Amazon tote filled with diverse consumer packaging.
* **Right (CV Target Grid):** The output of the perception pipeline. The algorithm successfully ignores the blue bin and structural holes, drawing precise target coordinates exclusively around the packages.

---

## 🛠️ Quick Start Guide

Because the entire architecture is fully containerized, you can launch the complete, networked ecosystem with a single command. 

**Prerequisites:** * Docker Desktop installed and running.

**Execution:**
1. Clone this repository.
2. Open your terminal in the root folder.
3. Run the following command:

```bash
docker compose up --build
