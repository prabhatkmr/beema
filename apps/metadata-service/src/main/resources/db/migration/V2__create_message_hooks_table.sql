-- Create sys_message_hooks table for dynamic JEXL-based message transformation
CREATE TABLE IF NOT EXISTS sys_message_hooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hook_name VARCHAR(255) NOT NULL UNIQUE,
    message_type VARCHAR(100) NOT NULL,
    script TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    priority INT NOT NULL DEFAULT 0,
    description TEXT,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for common queries
CREATE INDEX idx_message_hooks_message_type ON sys_message_hooks(message_type);
CREATE INDEX idx_message_hooks_enabled ON sys_message_hooks(enabled);
CREATE INDEX idx_message_hooks_priority ON sys_message_hooks(priority);

-- Add comments
COMMENT ON TABLE sys_message_hooks IS 'Stores JEXL transformation scripts for message processing';
COMMENT ON COLUMN sys_message_hooks.hook_name IS 'Unique identifier for the hook';
COMMENT ON COLUMN sys_message_hooks.message_type IS 'Type of message this hook applies to (e.g., SUBMISSION, ENDORSEMENT)';
COMMENT ON COLUMN sys_message_hooks.script IS 'JEXL script for message transformation';
COMMENT ON COLUMN sys_message_hooks.enabled IS 'Whether this hook is active';
COMMENT ON COLUMN sys_message_hooks.priority IS 'Execution priority (lower number = higher priority)';

-- Create notification function for Kafka control stream
CREATE OR REPLACE FUNCTION notify_message_hook_change()
RETURNS TRIGGER AS $$
DECLARE
    payload JSON;
BEGIN
    IF (TG_OP = 'DELETE') THEN
        payload = json_build_object(
            'operation', 'DELETE',
            'hookId', OLD.id::text,
            'messageType', OLD.message_type,
            'script', OLD.script,
            'enabled', OLD.enabled,
            'updatedAt', CURRENT_TIMESTAMP
        );
        PERFORM pg_notify('message_hook_changed', payload::text);
        RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
        payload = json_build_object(
            'operation', 'UPDATE',
            'hookId', NEW.id::text,
            'messageType', NEW.message_type,
            'script', NEW.script,
            'enabled', NEW.enabled,
            'updatedAt', CURRENT_TIMESTAMP
        );
        PERFORM pg_notify('message_hook_changed', payload::text);
        RETURN NEW;
    ELSIF (TG_OP = 'INSERT') THEN
        payload = json_build_object(
            'operation', 'INSERT',
            'hookId', NEW.id::text,
            'messageType', NEW.message_type,
            'script', NEW.script,
            'enabled', NEW.enabled,
            'updatedAt', CURRENT_TIMESTAMP
        );
        PERFORM pg_notify('message_hook_changed', payload::text);
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Create trigger on sys_message_hooks
CREATE TRIGGER message_hook_change_trigger
    AFTER INSERT OR UPDATE OR DELETE ON sys_message_hooks
    FOR EACH ROW
    EXECUTE FUNCTION notify_message_hook_change();

COMMENT ON FUNCTION notify_message_hook_change() IS 'Notifies listeners when message hooks are changed';
COMMENT ON TRIGGER message_hook_change_trigger ON sys_message_hooks IS 'Triggers notification for control stream updates';
