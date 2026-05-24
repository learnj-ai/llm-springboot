-- Module 04 chatbot/agent demo seed: customers + support tickets.

CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    subscription_plan VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS support_tickets (
    ticket_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) REFERENCES customers(customer_id),
    subject VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO customers (customer_id, name, email, subscription_plan) VALUES
    ('CUST001', 'Alice Johnson', 'alice@example.com', 'premium'),
    ('CUST002', 'Bob Smith',     'bob@example.com',   'standard'),
    ('CUST003', 'Carol White',   'carol@example.com', 'premium'),
    ('CUST004', 'David Brown',   'david@example.com', 'basic')
ON CONFLICT (customer_id) DO NOTHING;

INSERT INTO support_tickets (ticket_id, customer_id, subject, status) VALUES
    ('TKT001', 'CUST001', 'Cannot access premium features', 'open'),
    ('TKT002', 'CUST002', 'Billing question',               'pending'),
    ('TKT003', 'CUST003', 'Feature request: Dark mode',     'open'),
    ('TKT004', 'CUST001', 'Integration help needed',        'closed')
ON CONFLICT (ticket_id) DO NOTHING;
