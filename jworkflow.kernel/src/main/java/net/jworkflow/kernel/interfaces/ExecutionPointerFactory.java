package net.jworkflow.kernel.interfaces;

import net.jworkflow.kernel.models.ExecutionPointer;
import net.jworkflow.kernel.models.StepOutcome;
import net.jworkflow.kernel.models.WorkflowDefinition;

public interface ExecutionPointerFactory {
  ExecutionPointer buildGenesisPointer(WorkflowDefinition def);

  ExecutionPointer buildCompensationPointer(
      WorkflowDefinition def,
      ExecutionPointer pointer,
      ExecutionPointer exceptionPointer,
      int compensationStepId);

  ExecutionPointer buildNextPointer(
      WorkflowDefinition def, ExecutionPointer pointer, StepOutcome outcomeTarget);

  ExecutionPointer buildChildPointer(
      WorkflowDefinition def, ExecutionPointer pointer, int childDefinitionId, Object branch);
}
