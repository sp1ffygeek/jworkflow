package net.jworkflow.primitives;

import net.jworkflow.kernel.interfaces.StepBody;
import net.jworkflow.kernel.models.ExecutionResult;
import net.jworkflow.kernel.models.StepExecutionContext;

import java.util.Date;

public class WaitFor implements StepBody {

  public String eventKey;
  public String eventName;
  public Date effectiveDate;
  public Object eventData;

  @Override
  public ExecutionResult run(StepExecutionContext context) {

    if (!context.getExecutionPointer().eventPublished) {
      return ExecutionResult.waitForEvent(eventName, eventKey, effectiveDate);
    }

    eventData = context.getExecutionPointer().eventData;
    return ExecutionResult.next();
  }
}
