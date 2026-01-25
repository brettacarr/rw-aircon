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

-- Season table for scheduling periods with date ranges
CREATE TABLE IF NOT EXISTS season (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    start_month INTEGER NOT NULL,
    start_day INTEGER NOT NULL,
    end_month INTEGER NOT NULL,
    end_day INTEGER NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1
);

-- Schedule entry table for time periods within a season's weekly schedule
CREATE TABLE IF NOT EXISTS schedule_entry (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    season_id INTEGER NOT NULL,
    day_of_week INTEGER NOT NULL,
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    mode TEXT NOT NULL,
    FOREIGN KEY (season_id) REFERENCES season(id) ON DELETE CASCADE
);

-- Index for schedule_entry
CREATE INDEX IF NOT EXISTS idx_schedule_entry_season_day ON schedule_entry(season_id, day_of_week);

-- Zone schedule table for per-zone settings within a schedule entry
CREATE TABLE IF NOT EXISTS zone_schedule (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    schedule_entry_id INTEGER NOT NULL,
    zone_id INTEGER NOT NULL,
    target_temp INTEGER NOT NULL,
    enabled INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (schedule_entry_id) REFERENCES schedule_entry(id) ON DELETE CASCADE,
    FOREIGN KEY (zone_id) REFERENCES zone(id)
);

-- Index for zone_schedule
CREATE INDEX IF NOT EXISTS idx_zone_schedule_entry ON zone_schedule(schedule_entry_id);

-- Override table for temporary manual overrides of scheduled settings
-- Only one override should be active at a time; old overrides are cleaned up automatically
CREATE TABLE IF NOT EXISTS override (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    mode TEXT,
    system_temp INTEGER,
    zone_overrides TEXT
);

-- Index for finding active overrides efficiently
CREATE INDEX IF NOT EXISTS idx_override_expires_at ON override(expires_at);

-- System configuration table for global settings like control mode
CREATE TABLE IF NOT EXISTS system_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    control_mode TEXT NOT NULL DEFAULT 'MANUAL',
    mode_changed_at TEXT NOT NULL
);

-- Insert default system configuration if not exists
INSERT OR IGNORE INTO system_config (id, control_mode, mode_changed_at)
VALUES (1, 'MANUAL', datetime('now'));

-- Auto Mode configuration table (singleton - only one record)
CREATE TABLE IF NOT EXISTS auto_mode_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    active INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    priority_zone_id INTEGER,
    FOREIGN KEY (priority_zone_id) REFERENCES zone(id)
);

-- Insert default Auto Mode configuration if not exists
INSERT OR IGNORE INTO auto_mode_config (id, active, created_at, updated_at, priority_zone_id)
VALUES (1, 0, datetime('now'), datetime('now'), NULL);

-- Auto Mode zone configuration table
-- Each zone can have its own min/max temperature range for Auto Mode
CREATE TABLE IF NOT EXISTS auto_mode_zone (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    zone_id INTEGER NOT NULL UNIQUE,
    enabled INTEGER NOT NULL DEFAULT 1,
    min_temp REAL NOT NULL DEFAULT 20.0,
    max_temp REAL NOT NULL DEFAULT 24.0,
    FOREIGN KEY (zone_id) REFERENCES zone(id) ON DELETE CASCADE
);

-- Index for auto_mode_zone
CREATE INDEX IF NOT EXISTS idx_auto_mode_zone_zone_id ON auto_mode_zone(zone_id);

-- Initialize Auto Mode zone configurations for existing zones if not exists
INSERT OR IGNORE INTO auto_mode_zone (zone_id, enabled, min_temp, max_temp) VALUES (1, 1, 20.0, 24.0);
INSERT OR IGNORE INTO auto_mode_zone (zone_id, enabled, min_temp, max_temp) VALUES (2, 0, 20.0, 24.0);
INSERT OR IGNORE INTO auto_mode_zone (zone_id, enabled, min_temp, max_temp) VALUES (3, 1, 20.0, 24.0);
