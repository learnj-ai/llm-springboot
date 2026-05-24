-- Module 03: Sample Customer and Support Ticket Data

-- Insert sample customers
INSERT INTO customers (customer_id, name, email, subscription_plan) VALUES
    ('12345', 'Alice Johnson', 'alice.johnson@example.com', 'premium'),
    ('12346', 'Bob Smith', 'bob.smith@example.com', 'standard'),
    ('12347', 'Carol Martinez', 'carol.martinez@example.com', 'premium'),
    ('12348', 'David Chen', 'david.chen@example.com', 'basic'),
    ('12349', 'Emma Wilson', 'emma.wilson@example.com', 'standard')
ON CONFLICT (customer_id) DO NOTHING;

-- Insert sample support tickets
INSERT INTO support_tickets (customer_id, subject, status) VALUES
    ('12345', 'Cannot access dashboard after login', 'open'),
    ('12345', 'Billing discrepancy for March invoice', 'pending'),
    ('12346', 'Feature request: dark mode', 'closed'),
    ('12346', 'API rate limit exceeded error', 'open'),
    ('12347', 'Password reset not working', 'open'),
    ('12347', 'Export functionality broken', 'pending'),
    ('12348', 'Slow performance on reports page', 'closed'),
    ('12349', 'Integration with third-party service', 'pending'),
    ('12349', 'Mobile app crashes on startup', 'open'),
    ('12345', 'Need help with API documentation', 'closed');
