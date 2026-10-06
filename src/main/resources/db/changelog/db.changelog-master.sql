--liquibase formatted sql

--changeset cheapskountant:001-create-receipt-tables
CREATE TABLE receipt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_type VARCHAR(32) NOT NULL,
    source_file_name VARCHAR(2048) NULL,
    source_raw_text TEXT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_receipt PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE seller (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    trade_name VARCHAR(512) NOT NULL,
    legal_name VARCHAR(512) NULL,
    tax_id VARCHAR(10) NOT NULL,
    bdo_number VARCHAR(9) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_seller PRIMARY KEY (id),
    CONSTRAINT uq_seller_receipt UNIQUE (receipt_id),
    CONSTRAINT fk_seller_receipt FOREIGN KEY (receipt_id) REFERENCES receipt (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE address (
    id BIGINT NOT NULL AUTO_INCREMENT,
    seller_id BIGINT NOT NULL,
    role VARCHAR(16) NOT NULL,
    street VARCHAR(512) NOT NULL,
    postal_code VARCHAR(16) NOT NULL,
    city VARCHAR(256) NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_address PRIMARY KEY (id),
    CONSTRAINT uq_address_seller_role UNIQUE (seller_id, role),
    CONSTRAINT fk_address_seller FOREIGN KEY (seller_id) REFERENCES seller (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE receipt_header (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    receipt_number VARCHAR(128) NOT NULL,
    issued_at DATETIME(3) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    order_number VARCHAR(128) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_receipt_header PRIMARY KEY (id),
    CONSTRAINT uq_receipt_header_receipt UNIQUE (receipt_id),
    CONSTRAINT fk_receipt_header_receipt FOREIGN KEY (receipt_id) REFERENCES receipt (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE receipt_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    description VARCHAR(1024) NOT NULL,
    item_type VARCHAR(32) NULL,
    quantity DECIMAL(12, 3) NOT NULL,
    unit VARCHAR(32) NULL,
    unit_price DECIMAL(14, 2) NOT NULL,
    line_total DECIMAL(14, 2) NOT NULL,
    tax_category VARCHAR(32) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_receipt_item PRIMARY KEY (id),
    CONSTRAINT uq_receipt_item_line UNIQUE (receipt_id, line_no),
    CONSTRAINT fk_receipt_item_receipt FOREIGN KEY (receipt_id) REFERENCES receipt (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tax_summary (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    tax_category VARCHAR(32) NOT NULL,
    tax_rate DECIMAL(5, 2) NOT NULL,
    taxable_sales DECIMAL(14, 2) NOT NULL,
    tax_amount DECIMAL(14, 2) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_tax_summary PRIMARY KEY (id),
    CONSTRAINT uq_tax_summary_line UNIQUE (receipt_id, line_no),
    CONSTRAINT fk_tax_summary_receipt FOREIGN KEY (receipt_id) REFERENCES receipt (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE receipt_totals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    tax_amount DECIMAL(14, 2) NOT NULL,
    gross_amount DECIMAL(14, 2) NOT NULL,
    amount_due DECIMAL(14, 2) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_receipt_totals PRIMARY KEY (id),
    CONSTRAINT uq_receipt_totals_receipt UNIQUE (receipt_id),
    CONSTRAINT fk_receipt_totals_receipt FOREIGN KEY (receipt_id) REFERENCES receipt (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    method VARCHAR(16) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    transaction_id VARCHAR(128) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_payment PRIMARY KEY (id),
    CONSTRAINT uq_payment_line UNIQUE (receipt_id, line_no),
    CONSTRAINT fk_payment_receipt FOREIGN KEY (receipt_id) REFERENCES receipt (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fiscal_data (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    cash_register_code VARCHAR(64) NULL,
    cashier_code VARCHAR(64) NULL,
    fiscal_device_number VARCHAR(128) NULL,
    verification_hash VARCHAR(256) NULL,
    raw_identification_line VARCHAR(512) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_fiscal_data PRIMARY KEY (id),
    CONSTRAINT uq_fiscal_data_receipt UNIQUE (receipt_id),
    CONSTRAINT fk_fiscal_data_receipt FOREIGN KEY (receipt_id) REFERENCES receipt (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE unparsed_line (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    line_text TEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    CONSTRAINT pk_unparsed_line PRIMARY KEY (id),
    CONSTRAINT uq_unparsed_line_line UNIQUE (receipt_id, line_no),
    CONSTRAINT fk_unparsed_line_receipt FOREIGN KEY (receipt_id) REFERENCES receipt (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
