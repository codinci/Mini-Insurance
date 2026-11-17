-- Runs once when the postgres volume is first created.
-- Each microservice owns its own database on the shared server.
CREATE DATABASE auth_db;
CREATE DATABASE policy_db;
CREATE DATABASE claims_db;
CREATE DATABASE payment_db;
