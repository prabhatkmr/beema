-- V20: Add skill tags and WIP limits to sys_users for smart routing
-- Enables intelligent task assignment based on user skills and capacity

-- Add skill tags column (JSONB array of skill strings)
ALTER TABLE sys_users
ADD COLUMN skill_tags JSONB DEFAULT '[]'::jsonb;

-- Add WIP (Work in Progress) limit column
ALTER TABLE sys_users
ADD COLUMN max_tasks INTEGER DEFAULT 10;

-- Add current task count column (maintained by router)
ALTER TABLE sys_users
ADD COLUMN current_tasks INTEGER DEFAULT 0;

-- Add availability status
ALTER TABLE sys_users
ADD COLUMN availability_status VARCHAR(50) DEFAULT 'AVAILABLE';

-- Add location for geographic routing
ALTER TABLE sys_users
ADD COLUMN location VARCHAR(100);

-- Create index on skill tags for fast skill matching
CREATE INDEX idx_sys_users_skill_tags_gin ON sys_users USING GIN (skill_tags);

-- Create index on availability for routing queries
CREATE INDEX idx_sys_users_availability ON sys_users(availability_status, current_tasks, max_tasks);

-- Comments
COMMENT ON COLUMN sys_users.skill_tags IS 'Array of skill identifiers (e.g., ["auto", "injury", "property"])';
COMMENT ON COLUMN sys_users.max_tasks IS 'Maximum number of concurrent tasks user can handle';
COMMENT ON COLUMN sys_users.current_tasks IS 'Current number of assigned tasks (updated by router)';
COMMENT ON COLUMN sys_users.availability_status IS 'AVAILABLE, BUSY, OFFLINE, ON_LEAVE';
COMMENT ON COLUMN sys_users.location IS 'Geographic location for location-based routing';

-- Sample data for testing
UPDATE sys_users
SET skill_tags = '["auto", "property"]'::jsonb,
    max_tasks = 10,
    current_tasks = 0,
    availability_status = 'AVAILABLE',
    location = 'US-EAST'
WHERE email = 'admin@beema.io';
