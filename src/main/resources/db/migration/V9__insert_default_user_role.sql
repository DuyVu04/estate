INSERT INTO roles (
    id,
    name,
    description,
    created_at,
    updated_at
)
VALUES (
    gen_random_uuid(),
    'ROLE_USER',
    'Standard User',
    NOW(),
    NOW()
)
ON CONFLICT (name) DO NOTHING;
