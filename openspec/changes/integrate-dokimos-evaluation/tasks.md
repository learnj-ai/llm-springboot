## 1. Dependencies and Configuration

- [x] 1.1 Add dokimos-core dependency to Module 06 pom.xml
- [x] 1.2 Add dokimos-spring-ai dependency to Module 06 pom.xml
- [x] 1.3 Add spring-ai-openai-spring-boot-starter dependency to Module 06 pom.xml
- [x] 1.4 Add dokimos-junit dependency with test scope to Module 06 pom.xml
- [x] 1.5 Create EvaluationConfig with JudgeLM bean using SpringAiSupport.asJudge()
- [x] 1.6 Add evaluation configuration properties (thresholds, model selection, timeout) to application.yml
- [x] 1.7 Verify no dependency conflicts between Langchain4J and Spring AI

## 2. Dataset Migration

- [x] 2.1 Convert data/eval-golden-set.json to Dokimos Dataset format with Example structure
- [x] 2.2 Add metadata fields (category, difficulty) to each example in dataset
- [x] 2.3 Create DatasetLoader service to load JSON files into Dokimos Dataset objects
- [x] 2.4 Add error handling for missing or malformed dataset files
- [x] 2.5 Create sample dataset with 10+ evaluation examples for RAG testing

## 3. Built-in Evaluators Integration

- [x] 3.1 Configure FaithfulnessEvaluator bean with JudgeLM
- [x] 3.2 Configure HallucinationEvaluator bean with JudgeLM
- [x] 3.3 Configure ContextualRelevanceEvaluator bean with JudgeLM
- [x] 3.4 Configure ExactMatchEvaluator bean for accuracy testing
- [x] 3.5 Add configurable thresholds for each evaluator from application properties

## 4. Custom Evaluator Example

- [x] 4.1 Create ResponseLengthEvaluator extending BaseEvaluator
- [x] 4.2 Implement evaluation logic checking min/max character thresholds
- [x] 4.3 Add configuration for ResponseLengthEvaluator thresholds
- [x] 4.4 Register ResponseLengthEvaluator as Spring bean

## 5. Task Implementation for RAG Service

- [x] 5.1 Create RAGEvaluationTask implementing Dokimos Task interface
- [x] 5.2 Inject RAG service dependencies into RAGEvaluationTask
- [x] 5.3 Implement task execution logic invoking RAG service with example input
- [x] 5.4 Include retrieved source documents in task output for evaluator context
- [x] 5.5 Add error handling wrapping RAG service exceptions

## 6. Experiment Orchestration

- [x] 6.1 Create EvaluationService with runExperiment method
- [x] 6.2 Build Experiment using builder pattern with dataset, task, and evaluators
- [x] 6.3 Execute experiment.run() and capture ExperimentResult
- [x] 6.4 Implement result aggregation extracting averages, min/max, and pass rates
- [x] 6.5 Add logging for experiment execution progress and scores

## 7. Result Export and Reporting

- [x] 7.1 Implement JSON export using ExperimentResult.toJson()
- [x] 7.2 Add console output formatting for development mode
- [x] 7.3 Create ExperimentResultDTO for REST API responses
- [x] 7.4 Add per-example result details to export format

## 8. REST API

- [x] 8.1 Create EvaluationController with POST /api/v1/eval/run endpoint
- [x] 8.2 Add EvalRequest DTO with dataset name and optional evaluator filter
- [x] 8.3 Add EvalResponse DTO mapping ExperimentResult to JSON
- [x] 8.4 Implement evaluator filtering logic when subset specified
- [x] 8.5 Add error handling for invalid dataset names (HTTP 404)
- [x] 8.6 Add validation for request parameters

## 9. JUnit Integration

- [x] 9.1 Create evaluation test class using dokimos-junit annotations
- [x] 9.2 Implement test method running experiment as JUnit test
- [x] 9.3 Add assertions for minimum score thresholds
- [x] 9.4 Configure JUnit to generate XML reports for CI/CD
- [x] 9.5 Add example tests for each evaluator type

## 10. Remove Custom Evaluation Code

- [x] 10.1 Remove custom EvaluationService.calculateAccuracy() method
- [x] 10.2 Remove custom EvaluationService.calculateRelevance() method
- [x] 10.3 Remove custom EvaluationService.calculateFaithfulness() method
- [x] 10.4 Remove custom EvaluationService.loadEvalSet() method
- [x] 10.5 Remove custom record definitions (EvalCase, EvalResult, MetricAverages, EvaluationReport)
- [x] 10.6 Update tasks.md in workshop-modules-03-06 change to mark 18.1-18.7 as replaced

## 11. Testing

- [x] 11.1 Write unit tests for DatasetLoader
- [x] 11.2 Write unit tests for RAGEvaluationTask
- [x] 11.3 Write unit tests for ResponseLengthEvaluator
- [x] 11.4 Write integration test for full experiment execution
- [x] 11.5 Write REST API test for /api/v1/eval/run endpoint
- [x] 11.6 Test dependency compatibility between Langchain4J and Spring AI

## 12. Documentation

- [x] 12.1 Update Module 06 workshop instructions replacing custom evaluation with Dokimos
- [x] 12.2 Add Lab 6.1: Load and run evaluation dataset
- [x] 12.3 Add Lab 6.2: Create custom evaluator
- [x] 12.4 Add Lab 6.3: Integrate evaluation into JUnit tests
- [x] 12.5 Document Dokimos vs custom implementation comparison
- [x] 12.6 Add troubleshooting section for dependency conflicts
- [x] 12.7 Update README with Dokimos integration details
