## ADDED Requirements

### Requirement: Dokimos framework integration
The system SHALL integrate the Dokimos evaluation framework for comprehensive LLM application assessment, replacing custom evaluation implementation with production-grade evaluators.

#### Scenario: Load Dokimos dependencies
- **WHEN** Module 06 application starts
- **THEN** Dokimos core, Spring AI integration, and JUnit modules are available on classpath

#### Scenario: Configure judge LLM
- **WHEN** evaluation configuration initializes
- **THEN** Spring AI ChatModel is converted to Dokimos JudgeLM using SpringAiSupport.asJudge()

### Requirement: Dataset management
The system SHALL load evaluation datasets in Dokimos format with support for examples, expected outputs, and metadata tagging.

#### Scenario: Load dataset from JSON
- **WHEN** evaluation service loads dataset file
- **THEN** JSON is parsed into Dokimos Dataset containing Example objects with input, expected, and metadata fields

#### Scenario: Access example metadata
- **WHEN** processing evaluation examples
- **THEN** each example's category and difficulty tags are accessible for filtering and reporting

#### Scenario: Handle missing dataset file
- **WHEN** dataset file does not exist at specified path
- **THEN** system throws descriptive error with file path

### Requirement: Built-in evaluators
The system SHALL use Dokimos built-in evaluators for faithfulness, hallucination detection, and contextual relevance assessment.

#### Scenario: Evaluate faithfulness
- **WHEN** FaithfulnessEvaluator runs on RAG response with source context
- **THEN** evaluator returns score indicating how well response aligns with provided context

#### Scenario: Detect hallucinations
- **WHEN** HallucinationEvaluator analyzes response against source documents
- **THEN** evaluator quantifies fabricated content with lower scores indicating better performance

#### Scenario: Assess contextual relevance
- **WHEN** ContextualRelevanceEvaluator examines retrieved context quality
- **THEN** evaluator scores relevance of retrieved documents to the query

#### Scenario: Use exact match evaluator
- **WHEN** ExactMatchEvaluator compares response to expected answer
- **THEN** evaluator returns binary pass/fail for exact string matching

### Requirement: Custom evaluator extensibility
The system SHALL support custom evaluators extending Dokimos BaseEvaluator for domain-specific assessment criteria.

#### Scenario: Create custom evaluator
- **WHEN** developer extends BaseEvaluator with custom logic
- **THEN** custom evaluator integrates seamlessly with Experiment orchestration

#### Scenario: Example custom evaluator for response length
- **WHEN** ResponseLengthEvaluator checks if response meets length requirements
- **THEN** evaluator scores responses based on configurable min/max character thresholds

### Requirement: Experiment orchestration
The system SHALL orchestrate comprehensive evaluation runs using Dokimos Experiment API, executing multiple evaluators across all dataset examples.

#### Scenario: Build experiment with dataset and evaluators
- **WHEN** creating experiment with dataset, task, and evaluator list
- **THEN** experiment configuration is validated and ready to run

#### Scenario: Execute evaluation experiment
- **WHEN** experiment.run() is invoked
- **THEN** all evaluators execute against all dataset examples, producing aggregated results

#### Scenario: Handle evaluator failures gracefully
- **WHEN** individual evaluator encounters error during execution
- **THEN** error is logged, other evaluators continue, and partial results are returned

### Requirement: Result aggregation and reporting
The system SHALL aggregate per-example evaluation scores into summary statistics including averages, min/max values, and pass rates.

#### Scenario: Calculate metric averages
- **WHEN** experiment completes with multiple examples
- **THEN** ExperimentResult provides average scores for each evaluator across all examples

#### Scenario: Report individual example results
- **WHEN** accessing experiment results
- **THEN** per-example scores are available for detailed analysis

#### Scenario: Export results to JSON
- **WHEN** calling ExperimentResult.toJson()
- **THEN** results export to structured JSON format suitable for API responses

#### Scenario: Display results in console
- **WHEN** running evaluation in development mode
- **THEN** results print to console with formatted tables showing scores per evaluator

### Requirement: Task implementation for RAG service
The system SHALL implement Dokimos Task interface bridging the RAG service to evaluation framework, providing responses for evaluation.

#### Scenario: Execute RAG task for evaluation
- **WHEN** task receives example input and context
- **THEN** task invokes RAG service and returns generated response

#### Scenario: Include source documents in task output
- **WHEN** RAG task executes query
- **THEN** task output includes both response text and retrieved source documents for evaluator context

#### Scenario: Handle RAG service errors
- **WHEN** RAG service throws exception during task execution
- **THEN** task wraps error in evaluator-friendly format without failing entire experiment

### Requirement: JUnit integration for CI/CD
The system SHALL provide JUnit test integration using dokimos-junit module for automated evaluation in build pipelines.

#### Scenario: Run evaluations as JUnit tests
- **WHEN** JUnit test suite executes
- **THEN** Dokimos evaluations run as test cases with pass/fail assertions

#### Scenario: Assert minimum score thresholds
- **WHEN** JUnit test validates experiment results
- **THEN** test fails if any evaluator score falls below configured threshold

#### Scenario: Generate test reports
- **WHEN** JUnit tests complete
- **THEN** evaluation results appear in standard JUnit XML reports for CI/CD integration

### Requirement: REST API for evaluation
The system SHALL expose REST endpoint accepting evaluation requests and returning Dokimos experiment results in JSON format.

#### Scenario: POST evaluation request
- **WHEN** POST /api/v1/eval/run with dataset name and optional evaluator filter
- **THEN** returns HTTP 200 with ExperimentResult JSON containing scores and aggregations

#### Scenario: Filter evaluators by name
- **WHEN** request specifies subset of evaluators to run
- **THEN** only requested evaluators execute, others are skipped

#### Scenario: Handle invalid dataset name
- **WHEN** request specifies non-existent dataset
- **THEN** returns HTTP 404 with error message indicating dataset not found

#### Scenario: Stream evaluation progress (optional)
- **WHEN** long-running evaluation executes
- **THEN** server can optionally stream progress updates via Server-Sent Events

### Requirement: Configuration and customization
The system SHALL provide configuration options for evaluator thresholds, LLM model selection, and evaluation parameters.

#### Scenario: Configure evaluator thresholds
- **WHEN** application properties specify custom thresholds (e.g., faithfulness minimum score)
- **THEN** evaluators use configured thresholds instead of defaults

#### Scenario: Select judge LLM model
- **WHEN** configuration specifies judge model name (e.g., gpt-4, gpt-3.5-turbo)
- **THEN** JudgeLM uses specified model for LLM-as-judge evaluations

#### Scenario: Set evaluation timeout
- **WHEN** configuration defines maximum execution time per example
- **THEN** evaluations respect timeout and mark slow examples as failed

### Requirement: Error handling and logging
The system SHALL provide detailed logging for evaluation execution, including evaluator selections, scores, and failure reasons.

#### Scenario: Log evaluator execution
- **WHEN** evaluator processes example
- **THEN** log entry includes evaluator name, example ID, and execution duration

#### Scenario: Log evaluation scores
- **WHEN** evaluator completes scoring
- **THEN** log entry shows score value and pass/fail status

#### Scenario: Log failure details
- **WHEN** evaluation fails due to LLM error or timeout
- **THEN** log includes full exception stack trace and example that caused failure
