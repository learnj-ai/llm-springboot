# Incident Response Procedure

When a production incident is detected:
1. The on-call engineer acknowledges the alert in PagerDuty within 5 minutes.
2. Open an incident channel in Slack using the `/incident` command.
3. Classify severity: SEV1 (customer impact), SEV2 (degraded service), SEV3 (internal only).
4. For SEV1 incidents, page the engineering manager and notify the VP of Engineering.
5. Post status updates every 15 minutes in the incident channel.
6. After resolution, schedule a blameless post-mortem within 48 hours.

All incidents must be documented in the incident tracker with root cause analysis and corrective actions.
