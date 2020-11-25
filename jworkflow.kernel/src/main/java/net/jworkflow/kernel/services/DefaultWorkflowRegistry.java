package net.jworkflow.kernel.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.jworkflow.kernel.builders.BaseWorkflowBuilder;
import net.jworkflow.kernel.interfaces.Workflow;
import net.jworkflow.kernel.interfaces.WorkflowBuilder;
import net.jworkflow.kernel.interfaces.WorkflowRegistry;
import net.jworkflow.kernel.models.WorkflowDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class DefaultWorkflowRegistry implements WorkflowRegistry {

  private final List<RegistryEntry> registry;
  private final Logger logger;
  @Inject
  public DefaultWorkflowRegistry(Logger logger) {
    this.registry = new ArrayList<>();
    this.logger = logger;
  }

  @Override
  public void registerWorkflow(Workflow workflow) {

    // if (registry.containsKey(key))
    //    throw new Exception("already registered");

    BaseWorkflowBuilder baseBuilder = new BaseWorkflowBuilder();
    WorkflowBuilder builder = baseBuilder.UseData(workflow.getDataType());

    workflow.build(builder);
    WorkflowDefinition def = builder.build(workflow.getId(), workflow.getVersion());

    RegistryEntry entry = new RegistryEntry(workflow.getId(), workflow.getVersion(), def);

    registry.add(entry);

    logger.log(
        Level.INFO,
        String.format("Registered workflow %s %s", workflow.getId(), workflow.getVersion()));
  }

  @Override
  public void registerWorkflow(WorkflowDefinition definition) {
    RegistryEntry entry =
        new RegistryEntry(definition.getId(), definition.getVersion(), definition);
    registry.add(entry);
    logger.log(
        Level.INFO,
        String.format("Registered workflow %s %s", definition.getId(), definition.getVersion()));
  }

  @Override
  public WorkflowDefinition getDefinition(String workflowId, int version) {
    for (RegistryEntry item : registry) {
      if (item.getId().equals(workflowId) && (item.version == version)) return item.getDefinition();
    }

    return null;
  }

  class RegistryEntry {
    private final String id;
    private final int version;
    private final WorkflowDefinition definition;

    public RegistryEntry(String id, int version, WorkflowDefinition definition) {
      this.id = id;
      this.version = version;
      this.definition = definition;
    }

    public String getId() {
      return id;
    }

    public int getVersion() {
      return version;
    }

    public WorkflowDefinition getDefinition() {
      return definition;
    }
  }
}
