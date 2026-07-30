-- Create properties table
CREATE TABLE properties (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    property_type VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    ward VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    area DECIMAL(10, 2) NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_properties_property_type CHECK (property_type IN ('APARTMENT', 'HOUSE', 'VILLA', 'LAND', 'SHOPHOUSE')),
    CONSTRAINT chk_properties_status CHECK (status IN ('AVAILABLE', 'RESERVED', 'SOLD')),
    CONSTRAINT chk_properties_area_positive CHECK (area > 0),
    CONSTRAINT chk_properties_price_positive CHECK (price > 0)
);

-- Create property_images table
CREATE TABLE property_images (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::VARCHAR,
    property_id VARCHAR(36) NOT NULL,
    url VARCHAR(500) NOT NULL,
    sort_order INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_property_images_property FOREIGN KEY (property_id) 
        REFERENCES properties(id) ON DELETE CASCADE
);



-- Add comments to tables
COMMENT ON TABLE properties IS 'Stores property listings';
COMMENT ON TABLE property_images IS 'Stores images associated with properties';

-- Add comments to important columns
COMMENT ON COLUMN properties.property_type IS 'Type of property: APARTMENT, HOUSE, VILLA, LAND, SHOPHOUSE';
COMMENT ON COLUMN properties.status IS 'Property status: AVAILABLE, RESERVED, SOLD';
COMMENT ON COLUMN properties.version IS 'Optimistic locking version';
COMMENT ON COLUMN properties.area IS 'Property area in square meters';
COMMENT ON COLUMN properties.price IS 'Property price in VND';
COMMENT ON COLUMN property_images.sort_order IS 'Display order of images';
