-- institutions
UPDATE institutions SET type = 'University' WHERE type IS NULL;
UPDATE institutions SET country = 'Unknown' WHERE country IS NULL;
UPDATE institutions SET university_name = name WHERE university_name IS NULL;
UPDATE institutions SET address = 'Unknown' WHERE address IS NULL;
UPDATE institutions SET city = 'Unknown' WHERE city IS NULL;
UPDATE institutions SET state = 'Unknown' WHERE state IS NULL;
UPDATE institutions SET pincode = '000000' WHERE pincode IS NULL;

ALTER TABLE institutions
    ADD COLUMN mobile_number VARCHAR(20),
    ADD COLUMN email VARCHAR(180),
    ADD COLUMN institution_code VARCHAR(20),
    ADD CONSTRAINT uk_institutions_code UNIQUE (institution_code),
    MODIFY COLUMN type VARCHAR(50) NOT NULL,
    MODIFY COLUMN country VARCHAR(100) NOT NULL,
    MODIFY COLUMN university_name VARCHAR(200) NOT NULL,
    MODIFY COLUMN address VARCHAR(500) NOT NULL,
    MODIFY COLUMN city VARCHAR(100) NOT NULL,
    MODIFY COLUMN state VARCHAR(100) NOT NULL,
    MODIFY COLUMN pincode VARCHAR(10) NOT NULL;

-- sponsors
UPDATE sponsors SET type = 'Government' WHERE type IS NULL;
UPDATE sponsors SET contact_email = 'unknown@example.com' WHERE contact_email IS NULL;
UPDATE sponsors SET phone = '0000000000' WHERE phone IS NULL;
UPDATE sponsors SET address = 'Unknown' WHERE address IS NULL;
UPDATE sponsors SET website = 'https://example.com' WHERE website IS NULL;

ALTER TABLE sponsors
    MODIFY COLUMN type VARCHAR(50) NOT NULL,
    MODIFY COLUMN contact_email VARCHAR(180) NOT NULL,
    MODIFY COLUMN phone VARCHAR(20) NOT NULL,
    MODIFY COLUMN address VARCHAR(500) NOT NULL,
    MODIFY COLUMN website VARCHAR(250) NOT NULL;

-- funding_schemes
ALTER TABLE funding_schemes
    DROP COLUMN upi_reference,
    ADD COLUMN description TEXT;

-- grant_calls
ALTER TABLE grant_calls
    DROP COLUMN expected_awards,
    DROP COLUMN total_budget_allocated;
