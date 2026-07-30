-- Re-add the grant-call planning fields that were dropped in V6 but are now wired
-- through the backend again (GrantCall.expectedAwards / totalBudgetAllocated).
-- Columns restored to their original V1 definitions (both nullable).
ALTER TABLE grant_calls
    ADD COLUMN expected_awards        INT            NULL,
    ADD COLUMN total_budget_allocated DECIMAL(15, 2) NULL;
