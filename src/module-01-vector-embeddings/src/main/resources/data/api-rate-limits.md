# Public API Rate Limits

The customer integration API allows 1,000 requests per minute for standard tenants.
Burst traffic above the limit returns HTTP 429 responses with a `Retry-After` header.
Premium tenants can request higher limits through the platform operations team.
