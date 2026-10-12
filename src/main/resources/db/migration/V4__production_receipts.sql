ALTER TABLE lots ADD COLUMN production_id BIGINT UNIQUE REFERENCES production_orders(id);
