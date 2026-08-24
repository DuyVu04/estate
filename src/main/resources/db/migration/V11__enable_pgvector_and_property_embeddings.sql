-- =========================================================================
-- V11: PGVECTOR EXTENSION & PROPERTY EMBEDDINGS WITH HNSW INDEX
-- =========================================================================

-- 1. Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Add embedding column to properties table
-- 768 dimensions matches Google Gemini text-embedding-004 & Ollama nomic-embed-text
ALTER TABLE properties 
ADD COLUMN IF NOT EXISTS embedding vector(768);

-- 3. Create HNSW (Hierarchical Navigable Small World) Index for ultra-fast Cosine search
-- m = 16 (max links per node), ef_construction = 64 (build search depth)
CREATE INDEX IF NOT EXISTS idx_properties_embedding_hnsw 
ON properties USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
