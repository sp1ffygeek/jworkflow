package net.jworkflow.primitives;

import net.jworkflow.kernel.exceptions.CorruptPersistenceDataException;
import net.jworkflow.kernel.interfaces.StepBody;
import net.jworkflow.kernel.models.ExecutionResult;
import net.jworkflow.kernel.models.ScheduleStepData;
import net.jworkflow.kernel.models.StepExecutionContext;

import java.time.Duration;

public class Schedule implements StepBody {

  public Duration duration;

  @Override
  public ExecutionResult run(StepExecutionContext context) throws CorruptPersistenceDataException {

    if (context.getPersistenceData() == null) {
      return ExecutionResult.sleep(duration, new ScheduleStepData(false));
    }

    if (context.getPersistenceData() instanceof ScheduleStepData) {
      ScheduleStepData persistenceData = (ScheduleStepData) context.getPersistenceData();

      if (!persistenceData.elapsed) {
        return ExecutionResult.branch(new Object[1], new ScheduleStepData(true));
      }

      if (context.getWorkflow().isBranchComplete(context.getExecutionPointer().id))
        return ExecutionResult.next();

      return ExecutionResult.persist(persistenceData);
    }

    throw new CorruptPersistenceDataException();
  }
}
