CREATE TABLE IF NOT EXISTS lands (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  world_id TEXT NOT NULL,
  min_x INTEGER NOT NULL,
  max_x INTEGER NOT NULL,
  min_y INTEGER NOT NULL,
  max_y INTEGER NOT NULL,
  min_z INTEGER NOT NULL,
  max_z INTEGER NOT NULL,
  owner_uuid TEXT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  grade INTEGER NOT NULL DEFAULT 0,
  flags TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS land_members (
  land_id INTEGER NOT NULL,
  member_uuid TEXT NOT NULL,
  role TEXT NOT NULL,
  added_at INTEGER NOT NULL,
  PRIMARY KEY (land_id, member_uuid),
  FOREIGN KEY (land_id) REFERENCES lands(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS land_chunks (
  world_id TEXT NOT NULL,
  chunk_x INTEGER NOT NULL,
  chunk_z INTEGER NOT NULL,
  land_id INTEGER NOT NULL,
  PRIMARY KEY (world_id, chunk_x, chunk_z, land_id),
  FOREIGN KEY (land_id) REFERENCES lands(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lands_world_bounds
  ON lands(world_id, min_x, max_x, min_z, max_z);

CREATE INDEX IF NOT EXISTS idx_lands_owner_uuid
  ON lands(owner_uuid);

CREATE INDEX IF NOT EXISTS idx_land_chunks_world_chunk
  ON land_chunks(world_id, chunk_x, chunk_z);
