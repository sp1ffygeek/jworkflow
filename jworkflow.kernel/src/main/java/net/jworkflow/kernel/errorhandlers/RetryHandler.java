package net.jworkflow.kernel.errorhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.jworkflow.kernel.interfaces.StepErrorHandler;
import net.jworkflow.kernel.models.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Queue;

@Singleton
public class RetryHandler implements StepErrorHandler {

  private final Clock clock;

  @Inject
  public RetryHandler(Clock clock) {
    this.clock = clock;
  }

  @Override
  public ErrorBehavior getErrorBehavior() {
    return ErrorBehavior.RETRY;
  }

  @Override
  public void handle(
      WorkflowInstance workflow,
      WorkflowDefinition def,
      ExecutionPointer pointer,
      WorkflowStep step,
      Queue<ExecutionPointer> bubleupQueue) {
    pointer.retryCounter++;
    pointer.sleepUntil = Date.from(Instant.now(clock).plus(step.getRetryInterval()));
    step.primeForRetry(pointer);
  }
}
