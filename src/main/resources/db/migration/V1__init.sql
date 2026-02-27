CREATE TABLE IF NOT EXISTS lands (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  world_uuid TEXT NOT NULL,
  min_x INTEGER NOT NULL,
  max_x INTEGER NOT NULL,
  min_z INTEGER NOT NULL,
  max_z INTEGER NOT NULL,
  owner_uuid TEXT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS land_members (
  land_id INTEGER NOT NULL,
  member_uuid TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'MEMBER',
  created_at INTEGER NOT NULL,
  PRIMARY KEY(land_id, member_uuid),
  FOREIGN KEY(land_id) REFERENCES lands(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS land_chunks (
  land_id INTEGER NOT NULL,
  world_uuid TEXT NOT NULL,
  chunk_x INTEGER NOT NULL,
  chunk_z INTEGER NOT NULL,
  PRIMARY KEY (land_id, world_uuid, chunk_x, chunk_z),
  FOREIGN KEY(land_id) REFERENCES lands(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lands_world_owner
  ON lands(world_uuid, owner_uuid);

CREATE INDEX IF NOT EXISTS idx_land_chunks_lookup
  ON land_chunks(world_uuid, chunk_x, chunk_z);
