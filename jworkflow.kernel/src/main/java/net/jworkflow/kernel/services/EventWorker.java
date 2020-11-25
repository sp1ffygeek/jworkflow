package net.jworkflow.kernel.services;

import com.google.inject.Inject;
import net.jworkflow.kernel.interfaces.LockService;
import net.jworkflow.kernel.interfaces.PersistenceService;
import net.jworkflow.kernel.interfaces.QueueService;
import net.jworkflow.kernel.models.*;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventWorker extends QueueWorker {

  private final PersistenceService persistenceProvider;
  private final LockService lockProvider;
  private final Integer processingThreadCount;

  @Inject
  public EventWorker(
      PersistenceService persistenceProvider,
      QueueService queueProvider,
      LockService lockProvider,
      Logger logger,
      Integer processingThreadCount) {
    super(queueProvider, logger);
    this.persistenceProvider = persistenceProvider;
    this.lockProvider = lockProvider;
    this.processingThreadCount = processingThreadCount;
  }

  @Override
  protected QueueType getQueueType() {
    return QueueType.EVENT;
  }

  @Override
  protected void executeItem(String item) {
    if (!lockProvider.acquireLock("evt:" + item)) {
      logger.log(Level.INFO, String.format("Event %s locked", item));
      return;
    }

    try {
      Event evt = persistenceProvider.getEvent(item);
      if (evt.eventTimeUtc.before(new Date())) {
        Iterable<EventSubscription> subs =
            persistenceProvider.getSubcriptions(evt.eventName, evt.eventKey, evt.eventTimeUtc);
        boolean success = true;

        for (EventSubscription sub : subs) success = success && seedSubscription(evt, sub);

        if (success) persistenceProvider.markEventProcessed(item);
      }
    } finally {
      lockProvider.releaseLock("evt:" + item);
    }
  }

  @Override
  protected void executeItem(WorkflowInstance workflowInstance) {
    executeItem(workflowInstance.getId());
  }

  private boolean seedSubscription(Event evt, EventSubscription sub) {
    if (!lockProvider.acquireLock(sub.workflowId)) {
      logger.log(Level.FINE, "Workflow locked {0}", sub.workflowId);
      return false;
    }

    try {
      WorkflowInstance workflow = persistenceProvider.getWorkflowInstance(sub.workflowId);
      ExecutionPointer[] pointers =
          workflow.getExecutionPointers().stream()
              .filter(p -> p.eventName != null && p.eventKey != null && !p.eventPublished)
              .filter(p -> p.eventName.equals(sub.eventName) && p.eventKey.equals(sub.eventKey))
              .toArray(ExecutionPointer[]::new);

      for (ExecutionPointer p : pointers) {
        p.eventData = evt.eventData;
        p.eventPublished = true;
        p.active = true;
      }

      workflow.setNextExecution((long) 0);
      persistenceProvider.persistWorkflow(workflow);
      persistenceProvider.terminateSubscription(sub.id);
      return true;
    } catch (Exception ex) {
      logger.log(Level.SEVERE, ex.toString());
      return false;
    } finally {
      lockProvider.releaseLock(sub.workflowId);
      queueProvider.queueForProcessing(QueueType.WORKFLOW, sub.workflowId);
    }
  }

  @Override
  protected int getThreadCount() {
    return processingThreadCount;
  }
}
