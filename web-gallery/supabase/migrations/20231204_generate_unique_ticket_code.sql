-- Function to generate a unique 6-digit ticket code
-- This handles collisions atomically at the database level
CREATE OR REPLACE FUNCTION generate_unique_ticket_code()
RETURNS TEXT
LANGUAGE plpgsql
AS $$
DECLARE
    new_code TEXT;
    max_attempts INTEGER := 100;
    attempts INTEGER := 0;
    code_exists BOOLEAN;
BEGIN
    LOOP
        -- Generate a 6-digit code
        new_code := LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
        
        -- Check if code already exists
        SELECT EXISTS(SELECT 1 FROM tickets WHERE code = new_code) INTO code_exists;
        
        -- If code doesn't exist, return it
        IF NOT code_exists THEN
            RETURN new_code;
        END IF;
        
        -- Safety check to prevent infinite loop
        attempts := attempts + 1;
        IF attempts >= max_attempts THEN
            RAISE EXCEPTION 'Failed to generate unique ticket code after % attempts', max_attempts;
        END IF;
    END LOOP;
END;
$$;

-- Add index on ticket code for faster lookups
CREATE INDEX IF NOT EXISTS idx_tickets_code ON tickets(code);

-- Add unique constraint if not exists (should already exist)
ALTER TABLE tickets ADD CONSTRAINT IF NOT EXISTS tickets_code_unique UNIQUE (code);