package net.jworkflow.primitives;

import net.jworkflow.kernel.interfaces.StepBody;
import net.jworkflow.kernel.models.ControlStepData;
import net.jworkflow.kernel.models.ExecutionResult;
import net.jworkflow.kernel.models.StepExecutionContext;

public class Sequence implements StepBody {

  @Override
  public ExecutionResult run(StepExecutionContext context) {

    if (context.getPersistenceData() == null) {
      return ExecutionResult.branch(new Object[1], new ControlStepData(true));
    }

    if (context.getPersistenceData() instanceof ControlStepData) {

      ControlStepData persistenceData = (ControlStepData) context.getPersistenceData();

      if (persistenceData.childrenActive) {
        if (context.getWorkflow().isBranchComplete(context.getExecutionPointer().id))
          return ExecutionResult.next();
      }
    }

    return ExecutionResult.persist(context.getPersistenceData());
  }
}
