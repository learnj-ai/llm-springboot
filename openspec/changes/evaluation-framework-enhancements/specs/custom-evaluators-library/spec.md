## ADDED Requirements

### Requirement: Citation Quality Evaluator
The system SHALL provide a CitationQualityEvaluator that assesses the quality and accuracy of citations in RAG responses.

#### Scenario: Proper citation format detected
- **WHEN** response contains citations in [1], [2] format
- **THEN** evaluator assigns score of 1.0

#### Scenario: Missing citations detected
- **WHEN** response contains no citations but uses source material
- **THEN** evaluator assigns score of 0.0 with reason "Missing citations"

#### Scenario: Broken citation format
- **WHEN** response contains malformed citations like "source 1" instead of [1]
- **THEN** evaluator assigns score of 0.5 with reason "Improper citation format"

### Requirement: Source Diversity Evaluator
The system SHALL provide a SourceDiversityEvaluator that measures the variety of sources used in RAG responses.

#### Scenario: Multiple distinct sources used
- **WHEN** response cites 3 or more different source documents
- **THEN** evaluator assigns score >= 0.8

#### Scenario: Single source overreliance
- **WHEN** response cites only 1 source multiple times
- **THEN** evaluator assigns score <= 0.3 with reason "Low source diversity"

#### Scenario: No sources retrieved
- **WHEN** RAG task returns empty source documents list
- **THEN** evaluator assigns score of 0.0 with reason "No sources available"

### Requirement: Answer Completeness Evaluator
The system SHALL provide an AnswerCompletenessEvaluator that verifies responses address all aspects of the query.

#### Scenario: Complete answer to multi-part question
- **WHEN** query asks "What are the features and pricing?" AND response covers both
- **THEN** evaluator assigns score >= 0.9

#### Scenario: Partial answer to multi-part question
- **WHEN** query asks "What are the features and pricing?" AND response covers only features
- **THEN** evaluator assigns score between 0.4 and 0.6

#### Scenario: Incomplete answer
- **WHEN** response omits key aspects of the query
- **THEN** evaluator provides detailed reason listing missing aspects

### Requirement: Semantic Coherence Evaluator
The system SHALL provide a SemanticCoherenceEvaluator that assesses logical flow and consistency in responses.

#### Scenario: Logically coherent response
- **WHEN** response has clear structure with consistent argumentation
- **THEN** evaluator assigns score >= 0.8

#### Scenario: Contradictory statements detected
- **WHEN** response contains contradictory claims (e.g., "X is true" then "X is false")
- **THEN** evaluator assigns score <= 0.4 with reason "Contains contradictions"

#### Scenario: Fragmented response
- **WHEN** response jumps between unrelated topics without transitions
- **THEN** evaluator assigns score between 0.3 and 0.6 with reason "Lacks coherence"

### Requirement: Bias Detection Evaluator
The system SHALL provide a BiasDetectionEvaluator that identifies potential bias in RAG responses.

#### Scenario: Neutral response detected
- **WHEN** response presents balanced viewpoints without favoring particular perspectives
- **THEN** evaluator assigns score >= 0.9

#### Scenario: One-sided perspective detected
- **WHEN** response only presents one viewpoint on a debatable topic
- **THEN** evaluator assigns score <= 0.5 with reason "One-sided perspective"

#### Scenario: Loaded language detected
- **WHEN** response uses emotionally charged or biased language
- **THEN** evaluator assigns score <= 0.6 with reason "Biased language detected"

### Requirement: Evaluator Registration
The system SHALL automatically register all custom evaluators as Spring beans for dependency injection.

#### Scenario: Custom evaluators discovered
- **WHEN** application context initializes
- **THEN** all custom evaluators are registered and available for experiments

#### Scenario: Evaluator filtering by name
- **WHEN** experiment is run with evaluator filter ["citation-quality", "source-diversity"]
- **THEN** only specified custom evaluators are included in experiment

### Requirement: Configuration Properties
The system SHALL support configurable thresholds and parameters for all custom evaluators via application.yml.

#### Scenario: Citation quality threshold configured
- **WHEN** application.yml sets dokimos.evaluators.citation-quality.min-citations=3
- **THEN** CitationQualityEvaluator requires minimum 3 citations

#### Scenario: Source diversity threshold configured
- **WHEN** application.yml sets dokimos.evaluators.source-diversity.min-sources=2
- **THEN** SourceDiversityEvaluator requires minimum 2 distinct sources

#### Scenario: Default values used when not configured
- **WHEN** no configuration provided for custom evaluator
- **THEN** evaluator uses sensible defaults (e.g., min-citations=2)

### Requirement: Documentation
The system SHALL provide comprehensive documentation for each custom evaluator including usage examples and configuration options.

#### Scenario: Evaluator documentation available
- **WHEN** developer reads docs/custom-evaluators/
- **THEN** documentation exists for all 5 custom evaluators with examples

#### Scenario: Configuration reference available
- **WHEN** developer checks application.yml configuration guide
- **THEN** all custom evaluator properties are documented with defaults

#### Scenario: Lab exercise provided
- **WHEN** student completes Lab 6.4: Custom Evaluator Library
- **THEN** student can create, configure, and test custom evaluators
