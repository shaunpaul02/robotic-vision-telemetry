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

-- Indexing optimized for rapid time-series analytical searches
CREATE INDEX IF NOT EXISTS idx_recorded_at ON robotic_pick_history(recorded_at);
