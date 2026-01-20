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
