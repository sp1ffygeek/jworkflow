package net.jworkflow.kernel.errorhandlers;

import com.google.inject.Singleton;
import net.jworkflow.kernel.interfaces.StepErrorHandler;
import net.jworkflow.kernel.models.*;

import java.util.Queue;

@Singleton
public class SuspendHandler implements StepErrorHandler {

  @Override
  public ErrorBehavior getErrorBehavior() {
    return ErrorBehavior.SUSPEND;
  }

  @Override
  public void handle(
      WorkflowInstance workflow,
      WorkflowDefinition def,
      ExecutionPointer pointer,
      WorkflowStep step,
      Queue<ExecutionPointer> bubleupQueue) {
    workflow.setStatus(WorkflowStatus.SUSPENDED);
  }
}
