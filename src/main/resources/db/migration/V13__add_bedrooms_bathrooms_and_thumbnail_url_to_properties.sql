-- V13: Add bedrooms, bathrooms, and thumbnail_url to properties table
ALTER TABLE properties
ADD COLUMN IF NOT EXISTS bedrooms INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS bathrooms INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(500);

COMMENT ON COLUMN properties.bedrooms IS 'Number of bedrooms in property';
COMMENT ON COLUMN properties.bathrooms IS 'Number of bathrooms in property';
COMMENT ON COLUMN properties.thumbnail_url IS 'Main cover/thumbnail image URL or object key';
