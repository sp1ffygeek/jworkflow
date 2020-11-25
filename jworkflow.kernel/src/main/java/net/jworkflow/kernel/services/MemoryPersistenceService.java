package net.jworkflow.kernel.services;

import com.google.inject.Singleton;
import net.jworkflow.kernel.interfaces.PersistenceService;
import net.jworkflow.kernel.models.Event;
import net.jworkflow.kernel.models.EventSubscription;
import net.jworkflow.kernel.models.WorkflowInstance;
import net.jworkflow.kernel.models.WorkflowStatus;

import java.util.*;

@Singleton
public class MemoryPersistenceService implements PersistenceService {

  private final List<WorkflowInstance> workflows;
  private final List<Event> events;
  private final List<EventSubscription> subscriptions;

  public MemoryPersistenceService() {
    workflows = new ArrayList<>();
    events = new ArrayList<>();
    subscriptions = new ArrayList<>();
  }

  @Override
  public synchronized String createNewWorkflow(WorkflowInstance workflow) {
    workflow.setId(UUID.randomUUID().toString());
    workflows.add(workflow);
    return workflow.getId();
  }

  @Override
  public synchronized void persistWorkflow(WorkflowInstance workflow) {
    workflows.removeIf(
        x -> (x.getId() == null ? workflow.getId() == null : x.getId().equals(workflow.getId())));
    workflows.add(workflow);
  }

  @Override
  public synchronized Iterable<String> getRunnableInstances() {
    ArrayList<String> result = new ArrayList<>();
    long now = new Date().getTime();
    workflows.stream()
        .filter(x -> x.getStatus() == WorkflowStatus.RUNNABLE && x.getNextExecution() != null)
        .filter(x -> x.getNextExecution() <= now)
        .forEach(
            item -> result.add(item.getId()));
    return result;
  }

  @Override
  public synchronized WorkflowInstance getWorkflowInstance(String id) {
    Optional<WorkflowInstance> result =
        workflows.stream()
            .filter(x -> (x.getId() == null ? id == null : x.getId().equals(id)))
            .findFirst();
    return result.orElse(null);
  }

  @Override
  public String createEventSubscription(EventSubscription subscription) {
    subscription.id = UUID.randomUUID().toString();
    subscriptions.add(subscription);
    return subscription.id;
  }

  @Override
  public Iterable<EventSubscription> getSubcriptions(String eventName, String eventKey, Date asOf) {
    ArrayList<EventSubscription> result = new ArrayList<>();
    subscriptions.stream()
        .filter(x -> x.eventName.equals(eventName) && x.eventKey.equals(eventKey))
        .filter(x -> x.subscribeAsOfUtc.before(asOf))
        .forEach(
                result::add);
    return result;
  }

  @Override
  public void terminateSubscription(String eventSubscriptionId) {
    subscriptions.removeIf(x -> x.id.equals(eventSubscriptionId));
  }

  @Override
  public synchronized String createEvent(Event newEvent) {
    newEvent.id = UUID.randomUUID().toString();
    events.add(newEvent);
    return newEvent.id;
  }

  @Override
  public synchronized Event getEvent(String id) {
    Optional<Event> result =
        events.stream().filter(x -> (Objects.equals(x.id, id))).findFirst();
    return result.orElse(null);
  }

  @Override
  public Iterable<String> getRunnableEvents() {
    ArrayList<String> result = new ArrayList<>();
    events.stream()
        .filter(x -> !x.isProcessed)
        .filter(x -> x.eventTimeUtc.before(new Date()))
        .forEach(
            item -> result.add(item.id));
    return result;
  }

  @Override
  public Iterable<String> getEvents(String eventName, String eventKey, Date asOf) {
    ArrayList<String> result = new ArrayList<>();
    events.stream()
        .filter(x -> x.eventName.equals(eventName) && x.eventKey.equals(eventKey))
        .filter(x -> x.eventTimeUtc.after(asOf))
        .forEach(
            item -> result.add(item.id));
    return result;
  }

  @Override
  public void markEventProcessed(String id) {
    Optional<Event> evt = events.stream().filter(x -> x.id.equals(id)).findFirst();
    evt.ifPresent(event -> event.isProcessed = true);
  }

  @Override
  public void markEventUnprocessed(String id) {
    Optional<Event> evt = events.stream().filter(x -> x.id.equals(id)).findFirst();
    evt.ifPresent(event -> event.isProcessed = false);
  }

  @Override
  public void provisionStore() {}
}
