package net.jworkflow.primitives;

import net.jworkflow.kernel.models.*;

import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

public class CancellableStep<TData> extends WorkflowStep {

  private final Function<TData, Boolean> condition;

  public CancellableStep(Class bodyType, Function<TData, Boolean> condition) {
    super(bodyType);
    this.condition = condition;
  }

  @Override
  public void afterWorkflowIteration(
      WorkflowExecutorResult executorResult,
      WorkflowDefinition defintion,
      WorkflowInstance workflow,
      ExecutionPointer executionPointer) {
    Boolean result = condition.apply((TData) workflow.getData());
    if (result) {
      executionPointer.active = false;
      executionPointer.status = PointerStatus.Complete;
      executionPointer.endTimeUtc = Date.from(Instant.now());
    }
  }
}
