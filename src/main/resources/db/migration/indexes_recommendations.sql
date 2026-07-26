-- =========================================================================
-- DATABASE INDEXES RECOMMENDATIONS
-- =========================================================================
-- This file contains recommended indexes for performance optimization.
-- These indexes are NOT automatically applied to the database.
-- 
-- USAGE:
-- 1. Review the indexes based on your query patterns and data volume
-- 2. Copy the needed CREATE INDEX statements
-- 3. Create a new migration file (e.g., V5__add_performance_indexes.sql)
-- 4. Paste the selected indexes into the new migration file
-- 
-- WHEN TO ADD INDEXES:
-- - When you notice slow queries on specific columns
-- - When your application has grown and needs performance optimization
-- - After analyzing query execution plans (EXPLAIN ANALYZE)
-- 
-- TRADE-OFFS:
-- (+) Faster SELECT, WHERE, JOIN queries
-- (-) Slower INSERT, UPDATE, DELETE operations
-- (-) Additional storage space required
-- =========================================================================


-- -------------------------------------------------------------------------
-- USERS TABLE INDEXES
-- -------------------------------------------------------------------------
-- Recommended for: Login queries, user lookup by email/username
-- CREATE INDEX idx_users_email ON users(email);
-- CREATE INDEX idx_users_username ON users(username);

-- Recommended for: Filtering active/inactive users
-- CREATE INDEX idx_users_status ON users(status);

-- Recommended for: Combined search by status and enabled flag
-- CREATE INDEX idx_users_status_enabled ON users(status, enabled);

-- Recommended for: User search by name (full-text search alternative)
-- CREATE INDEX idx_users_first_name ON users(first_name);
-- CREATE INDEX idx_users_last_name ON users(last_name);

-- Recommended for: Composite index for common user listing queries
-- CREATE INDEX idx_users_status_created_at ON users(status, created_at DESC);


-- -------------------------------------------------------------------------
-- ROLES TABLE INDEXES
-- -------------------------------------------------------------------------
-- Recommended for: Role lookup by name
-- CREATE INDEX idx_roles_name ON roles(name);


-- -------------------------------------------------------------------------
-- PERMISSIONS TABLE INDEXES
-- -------------------------------------------------------------------------
-- Recommended for: Permission lookup by name
-- CREATE INDEX idx_permissions_name ON permissions(name);


-- -------------------------------------------------------------------------
-- REFRESH_TOKENS TABLE INDEXES
-- -------------------------------------------------------------------------
-- Recommended for: Token validation, lookup by token string
-- CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Recommended for: Finding all tokens for a user, user's active sessions
-- CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Recommended for: Token expiration cleanup jobs
-- CREATE INDEX idx_refresh_tokens_expiry_date ON refresh_tokens(expiry_date);

-- Recommended for: Finding active (non-revoked) tokens
-- CREATE INDEX idx_refresh_tokens_revoked ON refresh_tokens(revoked);

-- Recommended for: Composite index for token validation queries
-- CREATE INDEX idx_refresh_tokens_token_revoked_expiry ON refresh_tokens(token, revoked, expiry_date);

-- Recommended for: Finding user's active tokens
-- CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens(user_id, revoked);


-- -------------------------------------------------------------------------
-- USER_ROLES JUNCTION TABLE INDEXES
-- -------------------------------------------------------------------------
-- Recommended for: Finding all roles for a user
-- CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- Recommended for: Finding all users with a specific role
-- CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);


-- -------------------------------------------------------------------------
-- ROLE_PERMISSIONS JUNCTION TABLE INDEXES
-- -------------------------------------------------------------------------
-- Recommended for: Finding all permissions for a role
-- CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);

-- Recommended for: Finding all roles that have a specific permission
-- CREATE INDEX idx_role_permissions_permission_id ON role_permissions(permission_id);


-- =========================================================================
-- MAINTENANCE QUERIES
-- =========================================================================
-- Use these queries to monitor index usage and performance

-- Check index usage statistics:
-- SELECT 
--     schemaname,
--     tablename,
--     indexname,
--     idx_scan as index_scans,
--     idx_tup_read as tuples_read,
--     idx_tup_fetch as tuples_fetched
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public'
-- ORDER BY idx_scan DESC;

-- Find unused indexes:
-- SELECT
--     schemaname,
--     tablename,
--     indexname
-- FROM pg_stat_user_indexes
-- WHERE idx_scan = 0
--   AND schemaname = 'public'
--   AND indexname NOT LIKE '%_pkey';

-- Check table sizes:
-- SELECT
--     tablename,
--     pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
-- FROM pg_tables
-- WHERE schemaname = 'public'
-- ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- =========================================================================
-- EXAMPLE MIGRATION FILE TEMPLATE
-- =========================================================================
-- When you're ready to add indexes, create a new file like:
-- V5__add_performance_indexes.sql
--
-- And include content like:
-- 
-- -- Add indexes for user authentication queries
-- CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
-- CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
-- 
-- -- Add indexes for token validation
-- CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
-- CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
-- =========================================================================
