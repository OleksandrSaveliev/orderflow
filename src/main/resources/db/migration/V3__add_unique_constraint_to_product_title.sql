-- Add unique constraint to product title
ALTER TABLE products ADD CONSTRAINT uk_product_title UNIQUE (title);
