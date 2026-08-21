-- =========================================================================
-- V10: PROPERTY SEARCH & DETAIL PERFORMANCE INDEXES
-- =========================================================================

-- 1. Enable pg_trgm extension for GIN Trigram full-text keyword search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Partial Composite Index for searching available properties (ESR Rule)
-- Supports filtering by city, district, property_type, price range, and sorting by created_at
CREATE INDEX IF NOT EXISTS idx_properties_search_optimized 
ON properties (city, district, property_type, created_at DESC, price) 
WHERE status = 'AVAILABLE';

-- 3. GIN Index for fast title keyword searching (e.g. LIKE '%ven sông%', '%villa%')
CREATE INDEX IF NOT EXISTS idx_properties_title_gin 
ON properties USING gin (title gin_trgm_ops);

-- 4. Foreign Key Index on property_images to load property gallery fast
CREATE INDEX IF NOT EXISTS idx_property_images_property_id 
ON property_images (property_id);
