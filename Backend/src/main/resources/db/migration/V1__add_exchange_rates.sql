-- Exchange Rates Table
-- Stores currency exchange rates to VND for price conversion
-- Auto-updated daily via scheduled task

CREATE TABLE IF NOT EXISTS exchange_rates (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    currency VARCHAR(3) NOT NULL UNIQUE COMMENT 'Currency code: USD, CNY',
    rate_to_vnd DECIMAL(12, 2) NOT NULL COMMENT 'Exchange rate to VND',
    source VARCHAR(50) DEFAULT 'API' COMMENT 'API or MANUAL',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_currency (currency),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert initial fallback values
INSERT INTO exchange_rates (currency, rate_to_vnd, source) VALUES
('USD', 25000.00, 'MANUAL'),
('CNY', 3500.00, 'MANUAL')
ON DUPLICATE KEY UPDATE currency=currency;
