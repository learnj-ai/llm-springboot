## ADDED Requirements

### Requirement: Grafana Dashboard Configuration
The system SHALL provide Grafana dashboard JSON configurations for visualizing evaluation metrics in production.

#### Scenario: Dashboard JSON importable
- **WHEN** administrator imports dashboard JSON into Grafana
- **THEN** dashboard is created with all panels configured correctly

#### Scenario: Dashboard supports multiple datasources
- **WHEN** Prometheus is configured as datasource
- **THEN** dashboard queries metrics from Prometheus successfully

#### Scenario: Dashboard auto-refresh configured
- **WHEN** dashboard is opened in Grafana
- **THEN** metrics refresh every 30 seconds automatically

### Requirement: Pass Rate Overview Panel
The system SHALL display overall evaluation pass rate over time with trend indicators.

#### Scenario: Current pass rate displayed
- **WHEN** viewing dashboard
- **THEN** current pass rate is shown as large percentage value

#### Scenario: Pass rate trend graph
- **WHEN** viewing last 7 days of data
- **THEN** line graph shows pass rate trend with threshold line

#### Scenario: Threshold violation highlighted
- **WHEN** pass rate falls below 70%
- **THEN** panel background turns red with alert indicator

### Requirement: Per-Evaluator Score Tracking
The system SHALL display individual evaluator scores over time for detailed analysis.

#### Scenario: Evaluator score panels
- **WHEN** viewing dashboard
- **THEN** separate panels show scores for faithfulness, hallucination, contextual-relevance

#### Scenario: Score distribution histogram
- **WHEN** viewing evaluator details
- **THEN** histogram shows score distribution across all examples

#### Scenario: Min/max score range displayed
- **WHEN** viewing evaluator panel
- **THEN** score range (min, max, average) is shown

### Requirement: Evaluation Execution Metrics
The system SHALL track and display evaluation execution performance metrics.

#### Scenario: Execution duration tracked
- **WHEN** evaluations run in production
- **THEN** average execution time per evaluator is displayed

#### Scenario: LLM API call count
- **WHEN** LLM-as-judge evaluators execute
- **THEN** total API calls are counted and displayed

#### Scenario: Cost estimation displayed
- **WHEN** viewing execution metrics
- **THEN** estimated LLM API cost is shown based on token usage

### Requirement: Cache Performance Metrics
The system SHALL display cache hit/miss rates and cost savings from caching.

#### Scenario: Cache hit rate panel
- **WHEN** viewing dashboard
- **THEN** current cache hit rate is displayed as percentage

#### Scenario: Cost savings from cache
- **WHEN** evaluations use cached results
- **THEN** estimated cost savings (USD) is displayed

#### Scenario: Cache size monitoring
- **WHEN** viewing cache panel
- **THEN** current number of cached entries is shown

### Requirement: Regression Detection Panel
The system SHALL highlight evaluation regressions and score drops.

#### Scenario: Recent regression alert
- **WHEN** pass rate drops > 10% in last 24 hours
- **THEN** regression alert panel shows affected evaluators

#### Scenario: Score comparison view
- **WHEN** viewing regression details
- **THEN** side-by-side comparison shows previous vs current scores

#### Scenario: Regression timeline
- **WHEN** regression occurred
- **THEN** timeline shows exact commit/deployment that triggered regression

### Requirement: Dataset Coverage Metrics
The system SHALL track which datasets are evaluated and how frequently.

#### Scenario: Dataset evaluation frequency
- **WHEN** viewing coverage panel
- **THEN** chart shows evaluation runs per dataset (daily/weekly)

#### Scenario: Last evaluated timestamp
- **WHEN** viewing dataset list
- **THEN** each dataset shows timestamp of last evaluation

#### Scenario: Example coverage percentage
- **WHEN** partial dataset evaluation runs
- **THEN** percentage of dataset examples evaluated is shown

### Requirement: Example-Level Drill-Down
The system SHALL support drilling down to individual example results from dashboard.

#### Scenario: Failed examples list
- **WHEN** clicking on failed evaluations panel
- **THEN** list of failed examples with queries and scores is shown

#### Scenario: Example detail view
- **WHEN** clicking on specific example
- **THEN** detailed view shows query, response, context, and all evaluator scores

#### Scenario: Filter by evaluator
- **WHEN** filtering failed examples by evaluator
- **THEN** only examples that failed specified evaluator are shown

### Requirement: Alerting Configuration
The system SHALL support Grafana alerting based on evaluation metrics.

#### Scenario: Pass rate alert configured
- **WHEN** pass rate falls below 70%
- **THEN** Grafana sends alert notification to Slack/email

#### Scenario: Evaluator degradation alert
- **WHEN** any evaluator average score drops > 20%
- **THEN** alert is triggered with evaluator details

#### Scenario: Alert acknowledgment
- **WHEN** alert is triggered and acknowledged
- **THEN** alert state changes and no duplicate notifications sent

### Requirement: Multi-Environment Support
The system SHALL support separate dashboards for different environments (dev, staging, production).

#### Scenario: Environment selector
- **WHEN** viewing dashboard
- **THEN** dropdown allows switching between dev/staging/prod environments

#### Scenario: Environment-specific thresholds
- **WHEN** viewing production dashboard
- **THEN** stricter thresholds (80%) are applied vs staging (70%)

#### Scenario: Cross-environment comparison
- **WHEN** viewing comparison panel
- **THEN** pass rates across all environments are shown side-by-side

### Requirement: Export and Reporting
The system SHALL support exporting dashboard data for executive reporting.

#### Scenario: PDF report generation
- **WHEN** clicking "Generate Report" button
- **THEN** PDF report with all panels is generated

#### Scenario: CSV data export
- **WHEN** exporting metrics to CSV
- **THEN** time-series data for all evaluators is downloaded

#### Scenario: Weekly summary email
- **WHEN** scheduled report runs on Mondays
- **THEN** summary email with key metrics is sent to stakeholders

### Requirement: Documentation
The system SHALL provide comprehensive dashboard setup and usage guides.

#### Scenario: Dashboard setup guide available
- **WHEN** administrator reads docs/dashboards/SETUP.md
- **THEN** guide covers Grafana installation, datasource config, and dashboard import

#### Scenario: Metrics reference available
- **WHEN** developer reads docs/dashboards/METRICS.md
- **THEN** all Prometheus metrics are documented with examples

#### Scenario: Alerting guide available
- **WHEN** operator reads docs/dashboards/ALERTING.md
- **THEN** guide covers alert configuration, notification channels, and troubleshooting

#### Scenario: Lab exercise provided
- **WHEN** student completes Lab 6.7: Evaluation Dashboards
- **THEN** student can set up Grafana, import dashboards, and configure alerts
