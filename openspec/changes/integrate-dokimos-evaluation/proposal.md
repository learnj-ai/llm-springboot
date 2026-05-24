## Why

The workshop currently implements custom LLM evaluation logic (accuracy, relevance, faithfulness metrics) in Module 06, which duplicates functionality already provided by established frameworks. Dokimos is a production-grade Java evaluation framework that offers battle-tested evaluators, experiment orchestration, and result tracking—capabilities that would take significant effort to implement correctly from scratch. Integrating Dokimos reduces code complexity, provides students with industry-standard evaluation patterns, and demonstrates best practices for production AI systems.

## What Changes

- Replace custom `EvaluationService` with Dokimos integration
- Update Module 06 dependencies to include `dokimos-core` and `dokimos-spring-ai`
- Migrate custom evaluation metrics (accuracy, relevance, faithfulness) to Dokimos evaluators
- Update evaluation dataset format to match Dokimos `Dataset` structure
- Integrate with Dokimos experiment orchestration for comprehensive evaluation runs
- Add JUnit integration for CI/CD pipeline evaluation
- Update workshop documentation to teach Dokimos evaluation patterns

## Capabilities

### New Capabilities

- `dokimos-evaluation`: Integration of Dokimos evaluation framework for RAG and agent assessment, including built-in evaluators (faithfulness, hallucination, contextual relevance), experiment orchestration, dataset management, and result tracking

### Modified Capabilities

<!-- No existing capabilities are being modified - this replaces implementation details only -->

## Impact

- **Code**: Module 06's evaluation service implementation will be replaced with Dokimos API calls
- **Dependencies**: Adds `dokimos-core`, `dokimos-spring-ai`, and `dokimos-junit` Maven dependencies
- **Data**: Evaluation golden dataset (`data/eval-golden-set.json`) format will change to match Dokimos `Example` structure
- **Tests**: Evaluation tests will use Dokimos `Experiment` API instead of custom assertions
- **Documentation**: Workshop Module 06 instructions will teach Dokimos patterns instead of custom implementation
- **APIs**: Internal evaluation methods change, but REST endpoint `/api/v1/eval/run` signature remains compatible
- **Benefits**: Reduced code complexity, production-ready evaluation patterns, built-in experiment tracking, easier extensibility with custom evaluators
