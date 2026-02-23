-- src/main/resources/data.sql
INSERT INTO customer (id, name, total_rewards) VALUES (1, 'Jannik Sinner', 0.0);
INSERT INTO customer (id, name, total_rewards) VALUES (2, 'Carlos Alcaraz', 0.0);
INSERT INTO customer (id, name, total_rewards) VALUES (3, 'Novak Djokovic', 0.0);
INSERT INTO customer (id, name, total_rewards) VALUES (4, 'Rafael Nadal', 0.0);
INSERT INTO customer (id, name, total_rewards) VALUES (5, 'Roger Federer', 0.0);
INSERT INTO customer (id, name, total_rewards) VALUES (6, 'Ben Shelton', 0.0);
INSERT INTO customer (id, name, total_rewards) VALUES (7, 'Rybakina', 0.0);
INSERT INTO customer (id, name, total_rewards) VALUES (8, 'Swiatek', 0.0);
INSERT INTO customer (id, name, total_rewards) VALUES (9, 'Gauff', 0.0);
INSERT INTO customer (id, name, total_rewards) VALUES (10, 'Karolína', 0.0);

-- Reset sequence so future transactions don't conflict (H2 syntax)
ALTER TABLE customer ALTER COLUMN id RESTART WITH 11;

