-- Insert default permissions for User management
INSERT INTO permissions(
    id,
    name,
    description,
    created_at,
    updated_at
)
VALUES
    (gen_random_uuid(), 'USER_READ', 'Permission to read user information', NOW(), NOW()),
    (gen_random_uuid(), 'USER_WRITE', 'Permission to create new users', NOW(), NOW()),
    (gen_random_uuid(), 'USER_UPDATE', 'Permission to update user information', NOW(), NOW()),
    (gen_random_uuid(), 'USER_DELETE', 'Permission to delete users', NOW(), NOW());

-- Insert default permissions for Role management
INSERT INTO permissions(
    id,
    name,
    description,
    created_at,
    updated_at
)
VALUES
    (gen_random_uuid(), 'ROLE_READ', 'Permission to read role information', NOW(), NOW()),
    (gen_random_uuid(), 'ROLE_WRITE', 'Permission to create new roles', NOW(), NOW()),
    (gen_random_uuid(), 'ROLE_UPDATE', 'Permission to update role information', NOW(), NOW()),
    (gen_random_uuid(), 'ROLE_DELETE', 'Permission to delete roles', NOW(), NOW());

-- Insert default permissions for Property management
INSERT INTO permissions(
    id,
    name,
    description,
    created_at,
    updated_at
)
VALUES
    (gen_random_uuid(), 'PROPERTY_READ', 'Permission to read property information', NOW(), NOW()),
    (gen_random_uuid(), 'PROPERTY_WRITE', 'Permission to create new properties', NOW(), NOW()),
    (gen_random_uuid(), 'PROPERTY_UPDATE', 'Permission to update property information', NOW(), NOW()),
    (gen_random_uuid(), 'PROPERTY_DELETE', 'Permission to delete properties', NOW(), NOW());
