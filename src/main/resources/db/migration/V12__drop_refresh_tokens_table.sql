-- Drop refresh_tokens table as refresh token management has migrated to Redis In-Memory
DROP TABLE IF EXISTS refresh_tokens CASCADE;
