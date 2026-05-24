# Database Access Policy

Production database access requires approval from both the team lead and the database operations team.
Read-only access is granted through the SQL Gateway proxy — never connect directly to production instances.
All queries executed through the SQL Gateway are logged and audited monthly.

For write access, submit a change request ticket with the exact SQL statements to be executed.
Emergency write access during SEV1 incidents can be granted by the on-call DBA with verbal approval from an engineering manager. These emergency grants expire after 4 hours and must be documented in the incident post-mortem.
