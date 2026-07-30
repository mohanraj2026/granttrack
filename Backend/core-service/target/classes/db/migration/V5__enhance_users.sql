-- V5: Enhanced users for GrantTrack Phase-2 requirements

ALTER TABLE users ADD COLUMN education VARCHAR(200);
ALTER TABLE users ADD COLUMN college_id_path VARCHAR(500);
ALTER TABLE users ADD COLUMN profile_photo_path VARCHAR(500);
ALTER TABLE users ADD COLUMN country_code VARCHAR(10);
