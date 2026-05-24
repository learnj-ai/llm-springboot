# Deployment Process

TechCorp uses a continuous deployment pipeline with the following stages:
1. Code merged to main triggers an automated build in Jenkins.
2. Unit and integration tests must pass with at least 80% code coverage.
3. The artifact is deployed to the staging environment automatically.
4. QA team runs smoke tests and approves the staging deployment.
5. Production deployment requires two approvals: the PR author and a senior engineer.
6. Canary deployment rolls out to 5% of traffic first, then 25%, then 100%.

Rollbacks are automated — if error rate exceeds 1% during canary, the deployment is reverted automatically. Manual rollbacks can be triggered via the deployment dashboard or the `/rollback` Slack command.
