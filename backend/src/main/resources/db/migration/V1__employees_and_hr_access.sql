CREATE TABLE countries (
    code VARCHAR(2) PRIMARY KEY,
    name VARCHAR(80) NOT NULL
);

CREATE TABLE departments (
    code VARCHAR(30) PRIMARY KEY,
    name VARCHAR(80) NOT NULL
);

-- Immutable assessment assumptions: USD per one native unit, not live market quotes.
CREATE TABLE fx_rates (
    currency_code VARCHAR(3) PRIMARY KEY,
    usd_per_unit NUMERIC(18,8) NOT NULL CHECK (usd_per_unit > 0),
    version VARCHAR(30) NOT NULL,
    effective_date DATE NOT NULL,
    CHECK (currency_code <> 'USD' OR usd_per_unit = 1)
);

INSERT INTO countries VALUES
('US', 'United States'), ('IN', 'India'), ('DE', 'Germany'),
('GB', 'United Kingdom'), ('CA', 'Canada'), ('SG', 'Singapore');

INSERT INTO departments VALUES
('ENGINEERING', 'Engineering'), ('PRODUCT', 'Product'), ('DESIGN', 'Design'),
('PEOPLE', 'People'), ('FINANCE', 'Finance'), ('OPERATIONS', 'Operations');

INSERT INTO fx_rates VALUES
('USD', 1.00000000, 'illustrative-v1', '2026-01-01'),
('INR', 0.01200000, 'illustrative-v1', '2026-01-01'),
('EUR', 1.10000000, 'illustrative-v1', '2026-01-01'),
('GBP', 1.27000000, 'illustrative-v1', '2026-01-01'),
('CAD', 0.74000000, 'illustrative-v1', '2026-01-01'),
('SGD', 0.75000000, 'illustrative-v1', '2026-01-01');

CREATE TABLE employees (
    id UUID PRIMARY KEY,
    employee_code VARCHAR(30) NOT NULL UNIQUE,
    first_name VARCHAR(80) NOT NULL CHECK (length(trim(first_name)) > 0),
    last_name VARCHAR(80) NOT NULL CHECK (length(trim(last_name)) > 0),
    email VARCHAR(254) NOT NULL,
    country_code VARCHAR(2) NOT NULL REFERENCES countries(code),
    department_code VARCHAR(30) NOT NULL REFERENCES departments(code),
    annual_salary NUMERIC(12,2) NOT NULL CHECK (annual_salary > 0),
    currency_code VARCHAR(3) NOT NULL REFERENCES fx_rates(currency_code),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    demo_seed_key INTEGER UNIQUE
);

CREATE UNIQUE INDEX employees_email_unique ON employees (lower(email));
CREATE INDEX employees_population ON employees (active, country_code, department_code);
CREATE INDEX employees_salary ON employees (currency_code, annual_salary, id);
CREATE INDEX employees_name ON employees (lower(last_name), lower(first_name), id);

CREATE TABLE hr_users (
    username VARCHAR(80) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
