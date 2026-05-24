-- Test data

INSERT INTO customers (customer_id, name, email, subscription_plan) VALUES
    ('12345', 'Alice Johnson', 'alice.johnson@example.com', 'premium'),
    ('12346', 'Bob Smith', 'bob.smith@example.com', 'standard');

INSERT INTO support_tickets (customer_id, subject, status) VALUES
    ('12345', 'Test open ticket', 'open'),
    ('12345', 'Test pending ticket', 'pending'),
    ('12346', 'Test closed ticket', 'closed');
