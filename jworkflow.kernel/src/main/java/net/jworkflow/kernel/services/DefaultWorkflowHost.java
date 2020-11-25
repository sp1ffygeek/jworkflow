package net.jworkflow.kernel.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.jworkflow.definitionstorage.services.DefinitionLoader;
import net.jworkflow.kernel.interfaces.*;
import net.jworkflow.kernel.models.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class DefaultWorkflowHost implements WorkflowHost {

  private final PersistenceService persistenceProvider;
  private final QueueService queueProvider;
  private final LockService lockProvider;
  private final WorkflowRegistry registry;
  private final WorkflowWorker workflowWorker;
  private final ExecutionPointerFactory pointerFactory;
  private final DefinitionLoader definitionLoader;
  private final Clock clock;
  private final Logger logger;
  private final Set<BackgroundService> backgroundServices;
  private boolean active;
  private boolean runOnCallerThread;

  @Inject
  public DefaultWorkflowHost(
      PersistenceService persistenceProvider,
      QueueService queueProvider,
      LockService lockProvider,
      WorkflowRegistry registry,
      WorkflowWorker workflowWorker,
      ExecutionPointerFactory pointerFactory,
      DefinitionLoader definitionLoader,
      Clock clock,
      Set<BackgroundService> backgroundServices,
      Logger logger, boolean runOnCallerThread) {
    this.persistenceProvider = persistenceProvider;
    this.queueProvider = queueProvider;
    this.lockProvider = lockProvider;
    this.registry = registry;
    this.workflowWorker = workflowWorker;
    this.pointerFactory = pointerFactory;
    this.clock = clock;
    this.logger = logger;
    this.definitionLoader = definitionLoader;
    this.backgroundServices = backgroundServices;
    active = false;
    this.runOnCallerThread = runOnCallerThread;
  }

  @Override
  public String startWorkflow(String workflowId, int version, Object data, boolean runOnCallerThread) throws Exception {

    if (!active) throw new Exception("Host is not running");

    WorkflowDefinition def = registry.getDefinition(workflowId, version);

    if (def == null)
      throw new Exception(
          String.format("Workflow %s version %s is not registered", workflowId, version));

    WorkflowInstance wf = new WorkflowInstance();
    wf.setWorkflowDefintionId(workflowId);
    wf.setVersion(version);
    wf.setData(data);
    wf.setDescription(def.getDescription());
    wf.setNextExecution((long) 0);
    wf.setCreateTimeUtc(Date.from(Instant.now(clock)));
    wf.setStatus(WorkflowStatus.RUNNABLE);

    if ((def.getDataType() != null) && (data == null)) {
      wf.setData(def.getDataType().getDeclaredConstructor().newInstance());
    }

    wf.getExecutionPointers().add(pointerFactory.buildGenesisPointer(def));

    String id = persistenceProvider.createNewWorkflow(wf);

    if(runOnCallerThread){
      workflowWorker.executeItem(wf);
      return wf.getId();
    }

    queueProvider.queueForProcessing(QueueType.WORKFLOW, id);
    return id;
  }

  @Override
  public void start() {
    active = true;
    persistenceProvider.provisionStore();
    lockProvider.start();
    backgroundServices.forEach(
            BackgroundService::start);
  }

  @Override
  public void start(String workflowProcessorName, Integer processThreadCount) {
    active = true;
    persistenceProvider.provisionStore();
    lockProvider.start();
    backgroundServices.forEach(
            BackgroundService::start);
  }

  @Override
  public void stop() {
    active = false;
    backgroundServices.forEach(
            BackgroundService::stop);
    lockProvider.stop();
  }

  @Override
  public void registerWorkflow(Class<? extends Workflow> workflow) throws Exception {
    Workflow wf = workflow.getDeclaredConstructor().newInstance();
    registry.registerWorkflow(wf);
  }

  @Override
  public void registerWorkflow(Workflow workflow) throws Exception {
    registry.registerWorkflow(workflow);
  }

  @Override
  public void publishEvent(
      String eventName, String eventKey, Object eventData, Date effectiveDateUtc) throws Exception {
    if (!active) throw new Exception("Host is not running");

    logger.log(Level.INFO, String.format("Creating event %s %s", eventName, eventKey));

    Event evt = new Event();

    // TODO: use utc
    if (effectiveDateUtc != null) evt.eventTimeUtc = effectiveDateUtc;
    else evt.eventTimeUtc = Date.from(Instant.now(clock));

    evt.eventData = eventData;
    evt.eventKey = eventKey;
    evt.eventName = eventName;
    evt.isProcessed = false;
    String eventId = persistenceProvider.createEvent(evt);

    queueProvider.queueForProcessing(QueueType.EVENT, eventId);
  }

  @Override
  public boolean suspendWorkflow(String workflowId) {
    if (lockProvider.acquireLock(workflowId)) {
      try {
        WorkflowInstance wf = persistenceProvider.getWorkflowInstance(workflowId);
        if (wf.getStatus() == WorkflowStatus.RUNNABLE) {
          wf.setStatus(WorkflowStatus.SUSPENDED);
          persistenceProvider.persistWorkflow(wf);
          return true;
        }
        return false;
      } finally {
        lockProvider.releaseLock(workflowId);
      }
    }
    return false;
  }

  @Override
  public boolean resumeWorkflow(String workflowId) {
    if (lockProvider.acquireLock(workflowId)) {
      boolean requeue = false;
      try {
        WorkflowInstance wf = persistenceProvider.getWorkflowInstance(workflowId);
        if (wf.getStatus() == WorkflowStatus.SUSPENDED) {
          wf.setStatus(WorkflowStatus.RUNNABLE);
          persistenceProvider.persistWorkflow(wf);
          requeue = true;
          return true;
        }
        return false;
      } finally {
        lockProvider.releaseLock(workflowId);
        if (requeue) queueProvider.queueForProcessing(QueueType.WORKFLOW, workflowId);
      }
    }
    return false;
  }

  @Override
  public void registerWorkflowFromJson(String json) throws Exception {
    definitionLoader.loadFromJson(json);
  }
}
