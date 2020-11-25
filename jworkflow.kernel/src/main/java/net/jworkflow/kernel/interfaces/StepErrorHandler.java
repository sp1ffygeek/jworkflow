package net.jworkflow.kernel.interfaces;

import net.jworkflow.kernel.models.*;

import java.util.Queue;

public interface StepErrorHandler {
  ErrorBehavior getErrorBehavior();

  void handle(
      WorkflowInstance workflow,
      WorkflowDefinition def,
      ExecutionPointer pointer,
      WorkflowStep step,
      Queue<ExecutionPointer> bubleupQueue);
}
