## ADDED Requirements

### Requirement: GitHub Actions Workflow Template
The system SHALL provide a GitHub Actions workflow template for automated evaluation on pull requests and commits.

#### Scenario: Evaluation on pull request
- **WHEN** pull request is opened or updated
- **THEN** workflow runs evaluation tests and posts results as comment

#### Scenario: Evaluation quality gate
- **WHEN** evaluation pass rate falls below configured threshold (e.g., 70%)
- **THEN** workflow fails and blocks merge

#### Scenario: Evaluation artifacts published
- **WHEN** evaluation completes successfully
- **THEN** results are uploaded as workflow artifacts (JSON, HTML reports)

### Requirement: GitLab CI Pipeline Template
The system SHALL provide a GitLab CI pipeline template for automated evaluation in merge requests.

#### Scenario: Evaluation in merge request pipeline
- **WHEN** merge request is created
- **THEN** evaluation stage runs and displays results in pipeline view

#### Scenario: Quality gate enforcement
- **WHEN** any evaluator fails threshold check
- **THEN** pipeline stage fails and prevents merge

#### Scenario: JUnit XML report integration
- **WHEN** evaluation tests complete
- **THEN** JUnit XML reports are published to GitLab test summary

### Requirement: Jenkins Pipeline Template
The system SHALL provide a Jenkinsfile template for automated evaluation in Jenkins builds.

#### Scenario: Evaluation in Jenkins pipeline
- **WHEN** Jenkins build is triggered
- **THEN** evaluation stage runs with proper OpenAI API key from credentials

#### Scenario: Build artifacts archived
- **WHEN** evaluation completes
- **THEN** evaluation results are archived as build artifacts

#### Scenario: Email notification on failure
- **WHEN** evaluation pass rate drops below threshold
- **THEN** Jenkins sends email notification to development team

### Requirement: Threshold Configuration
The system SHALL support configurable pass rate thresholds for different environments (PR vs main branch).

#### Scenario: Strict threshold on main branch
- **WHEN** evaluation runs on main branch
- **THEN** minimum pass rate is 80% (strict)

#### Scenario: Lenient threshold on PR
- **WHEN** evaluation runs on pull request
- **THEN** minimum pass rate is 70% (allows iteration)

#### Scenario: Per-evaluator thresholds
- **WHEN** pipeline configuration specifies individual evaluator thresholds
- **THEN** each evaluator must meet its specific threshold

### Requirement: Parallel Evaluation Execution
The system SHALL support parallel execution of independent evaluators to reduce pipeline duration.

#### Scenario: LLM-as-judge evaluators run in parallel
- **WHEN** faithfulness and hallucination evaluators are independent
- **THEN** pipeline executes them concurrently to save time

#### Scenario: Rule-based evaluators run quickly
- **WHEN** exact-match and response-length evaluators run
- **THEN** execution completes in < 10 seconds

#### Scenario: Overall pipeline duration optimized
- **WHEN** full evaluation suite runs in CI/CD
- **THEN** total duration is < 5 minutes with parallelization

### Requirement: API Key Management
The system SHALL demonstrate secure OpenAI API key handling in CI/CD pipelines using secrets management.

#### Scenario: GitHub Actions secrets
- **WHEN** workflow accesses OPENAI_API_KEY
- **THEN** key is retrieved from GitHub repository secrets

#### Scenario: GitLab CI variables
- **WHEN** pipeline accesses OPENAI_API_KEY
- **THEN** key is retrieved from masked CI/CD variables

#### Scenario: Jenkins credentials
- **WHEN** Jenkinsfile accesses OpenAI credentials
- **THEN** key is retrieved from Jenkins credential store

### Requirement: Result Visualization
The system SHALL publish evaluation results in human-readable format accessible from CI/CD interface.

#### Scenario: HTML report published
- **WHEN** evaluation generates HTML report
- **THEN** report is available as downloadable artifact

#### Scenario: Inline test results in PR
- **WHEN** evaluation completes in GitHub Actions
- **THEN** results appear as PR check with pass/fail details

#### Scenario: Pipeline badge for main branch
- **WHEN** evaluation runs on main branch
- **THEN** repository README can display evaluation status badge

### Requirement: Selective Evaluation
The system SHALL support running different evaluation subsets in CI/CD based on changed files or pipeline stage.

#### Scenario: Smoke test on PR
- **WHEN** pull request is opened
- **THEN** only fast rule-based evaluators run for quick feedback

#### Scenario: Full evaluation on merge to main
- **WHEN** code is merged to main branch
- **THEN** all evaluators including expensive LLM-as-judge run

#### Scenario: Evaluation skipped on docs-only changes
- **WHEN** commit only modifies .md files
- **THEN** evaluation pipeline stage is skipped

### Requirement: Trend Analysis
The system SHALL track evaluation metrics over time and detect regressions.

#### Scenario: Historical pass rate tracking
- **WHEN** evaluation runs on main branch
- **THEN** pass rate is stored with commit SHA for trend analysis

#### Scenario: Regression detection
- **WHEN** pass rate drops > 10% from previous commit
- **THEN** pipeline sends alert notification

#### Scenario: Evaluator score trends exported
- **WHEN** evaluation completes
- **THEN** per-evaluator scores are exported to monitoring system

### Requirement: Documentation
The system SHALL provide comprehensive CI/CD integration guides with example configurations.

#### Scenario: GitHub Actions guide available
- **WHEN** developer reads docs/cicd/GITHUB-ACTIONS.md
- **THEN** guide covers workflow setup, secrets configuration, and troubleshooting

#### Scenario: GitLab CI guide available
- **WHEN** developer reads docs/cicd/GITLAB-CI.md
- **THEN** guide covers pipeline configuration, artifact publishing, and quality gates

#### Scenario: Jenkins guide available
- **WHEN** developer reads docs/cicd/JENKINS.md
- **THEN** guide covers Jenkinsfile setup, credentials management, and notifications

#### Scenario: Lab exercise provided
- **WHEN** student completes Lab 6.6: CI/CD Integration
- **THEN** student can set up evaluation pipeline with quality gates and artifact publishing
