-- Create notification_logs table
CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    source VARCHAR(50) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    metadata JSONB,
    status VARCHAR(50) NOT NULL,
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_notification_logs_username ON notification_logs(username);
CREATE INDEX IF NOT EXISTS idx_notification_logs_status ON notification_logs(status);
CREATE INDEX IF NOT EXISTS idx_notification_logs_created_at ON notification_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_notification_logs_source ON notification_logs(source);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_notification_logs_updated_at
BEFORE UPDATE ON notification_logs
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
