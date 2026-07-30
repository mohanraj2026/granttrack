-- Seed the six canonical security roles.
INSERT INTO roles (name, description, created_at, updated_at, deleted)
VALUES
    ('ROLE_RESEARCHER',         'Principal Investigator / Researcher',        NOW(6), NOW(6), FALSE),
    ('ROLE_REVIEWER',           'Peer Reviewer',                              NOW(6), NOW(6), FALSE),
    ('ROLE_GRANT_ADMIN',        'Grant Administrator',                        NOW(6), NOW(6), FALSE),
    ('ROLE_FINANCE_OFFICER',    'Research Finance Officer',                   NOW(6), NOW(6), FALSE),
    ('ROLE_COMPLIANCE_OFFICER', 'Compliance Officer',                         NOW(6), NOW(6), FALSE),
    ('ROLE_ADMIN',              'Research Admin (system administrator)',      NOW(6), NOW(6), FALSE);
