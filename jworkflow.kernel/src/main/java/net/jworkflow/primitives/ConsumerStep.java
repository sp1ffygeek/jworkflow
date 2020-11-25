package net.jworkflow.primitives;

import net.jworkflow.kernel.interfaces.StepBody;
import net.jworkflow.kernel.models.ExecutionResult;
import net.jworkflow.kernel.models.StepExecutionContext;

import java.util.function.Consumer;

public class ConsumerStep implements StepBody {

  public Consumer<StepExecutionContext> body;

  @Override
  public ExecutionResult run(StepExecutionContext context) throws Exception {
    body.accept(context);
    return ExecutionResult.next();
  }
}
