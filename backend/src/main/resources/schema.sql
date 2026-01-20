-- Zone configuration table
CREATE TABLE IF NOT EXISTS zone (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    my_air_zone_id TEXT NOT NULL UNIQUE
);

-- Seed initial zone data
INSERT OR IGNORE INTO zone (id, name, my_air_zone_id) VALUES (1, 'Living', 'z01');
INSERT OR IGNORE INTO zone (id, name, my_air_zone_id) VALUES (2, 'Guest', 'z02');
INSERT OR IGNORE INTO zone (id, name, my_air_zone_id) VALUES (3, 'Upstairs', 'z03');

-- Temperature log table for storing per-zone temperature history
CREATE TABLE IF NOT EXISTS temperature_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TEXT NOT NULL,
    zone_id INTEGER NOT NULL,
    current_temp REAL NOT NULL,
    target_temp REAL NOT NULL,
    zone_enabled INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (zone_id) REFERENCES zone(id)
);

-- Indexes for temperature_log
CREATE INDEX IF NOT EXISTS idx_temperature_log_zone_timestamp ON temperature_log(zone_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_temperature_log_timestamp ON temperature_log(timestamp);

-- System log table for storing system-level state history
CREATE TABLE IF NOT EXISTS system_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp TEXT NOT NULL,
    mode TEXT NOT NULL,
    outdoor_temp REAL,
    system_on INTEGER NOT NULL DEFAULT 0
);

-- Index for system_log
CREATE INDEX IF NOT EXISTS idx_system_log_timestamp ON system_log(timestamp);
