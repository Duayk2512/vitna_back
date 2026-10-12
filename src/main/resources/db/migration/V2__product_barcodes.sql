ALTER TABLE products ADD COLUMN barcode VARCHAR(48);
UPDATE products SET barcode = 'VTLEGACY' || LPAD(id::text, 12, '0');
ALTER TABLE products ALTER COLUMN barcode SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT uk_products_barcode UNIQUE (barcode);
