-- V4: Enhanced entities for GrantTrack Phase-2 requirements

-- ========================
-- SPONSOR enhancements
-- ========================
ALTER TABLE sponsors ADD COLUMN sponsor_code VARCHAR(20) UNIQUE;
ALTER TABLE sponsors ADD COLUMN phone VARCHAR(20);
ALTER TABLE sponsors ADD COLUMN address VARCHAR(500);
ALTER TABLE sponsors ADD COLUMN website VARCHAR(250);

-- backfill sponsor codes for existing rows
UPDATE sponsors SET sponsor_code = CONCAT('SP', LPAD(id, 6, '0')) WHERE sponsor_code IS NULL;

-- ========================
-- INSTITUTION enhancements
-- ========================
ALTER TABLE institutions ADD COLUMN university_name VARCHAR(200);
ALTER TABLE institutions ADD COLUMN address VARCHAR(500);
ALTER TABLE institutions ADD COLUMN city VARCHAR(100);
ALTER TABLE institutions ADD COLUMN state VARCHAR(100);
ALTER TABLE institutions ADD COLUMN pincode VARCHAR(10);

-- ========================
-- FUNDING SCHEME enhancements
-- ========================
ALTER TABLE funding_schemes ADD COLUMN scheme_code VARCHAR(20) UNIQUE;
ALTER TABLE funding_schemes ADD COLUMN category VARCHAR(100);
ALTER TABLE funding_schemes ADD COLUMN from_date DATE;
ALTER TABLE funding_schemes ADD COLUMN to_date DATE;
ALTER TABLE funding_schemes ADD COLUMN upi_reference VARCHAR(100);
ALTER TABLE funding_schemes ADD COLUMN document_path VARCHAR(500);

-- backfill scheme codes for existing rows
UPDATE funding_schemes SET scheme_code = CONCAT('FSC', LPAD(id, 5, '0')) WHERE scheme_code IS NULL;

-- ========================
-- GRANT CALL: add TERMINATED status support (enum already allows it via VARCHAR)
-- ========================
-- No schema change needed; CallStatus enum will be updated in Java.

-- ========================
-- GRANT CALL: add scheme document path reference
-- ========================
ALTER TABLE grant_calls ADD COLUMN scheme_document_path VARCHAR(500);
