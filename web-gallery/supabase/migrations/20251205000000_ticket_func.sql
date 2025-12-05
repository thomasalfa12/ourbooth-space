-- Function to generate AND insert a ticket atomically
-- This prevents race conditions where two clients generate the same code before inserting.
CREATE OR REPLACE FUNCTION generate_ticket_code(target_device_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
    new_code TEXT;
    max_attempts INTEGER := 20;
    attempts INTEGER := 0;
    ticket_record RECORD;
BEGIN
    LOOP
        -- 1. Generate 6-digit code
        new_code := LPAD(FLOOR(RANDOM() * 1000000)::TEXT, 6, '0');
        
        -- 2. Try to insert
        BEGIN
            INSERT INTO tickets (code, device_id, status)
            VALUES (new_code, target_device_id, 'AVAILABLE')
            RETURNING * INTO ticket_record;
            
            -- If successful, return the record as JSON
            RETURN to_jsonb(ticket_record);
        EXCEPTION WHEN unique_violation THEN
            -- If code exists, loop again
            attempts := attempts + 1;
            IF attempts >= max_attempts THEN
                RAISE EXCEPTION 'Failed to generate unique ticket code after % attempts', max_attempts;
            END IF;
        END;
    END LOOP;
END;
$$;
