-- =========================================================================
-- V14: DROP PROPERTY EMBEDDINGS & HNSW INDEX (REMOVAL OF AI FEATURES)
-- =========================================================================

-- 1. Drop HNSW index on properties embedding
DROP INDEX IF EXISTS idx_properties_embedding_hnsw;

-- 2. Drop embedding column from properties table
ALTER TABLE properties DROP COLUMN IF EXISTS embedding;

