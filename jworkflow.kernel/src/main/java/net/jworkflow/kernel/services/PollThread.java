package net.jworkflow.kernel.services;

import com.google.inject.Inject;
import net.jworkflow.kernel.interfaces.BackgroundService;
import net.jworkflow.kernel.interfaces.LockService;
import net.jworkflow.kernel.interfaces.PersistenceService;
import net.jworkflow.kernel.interfaces.QueueService;
import net.jworkflow.kernel.models.QueueType;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PollThread implements BackgroundService {

  private final PersistenceService persistence;
  private final QueueService queueProvider;
  private final LockService lockProvider;
  private final Logger logger;
  private final ScheduledExecutorService scheduler;

  @Inject
  public PollThread(
      PersistenceService persistence,
      QueueService queueProvider,
      LockService lockProvider,
      Logger logger) {
    this.persistence = persistence;
    this.queueProvider = queueProvider;
    this.lockProvider = lockProvider;
    this.logger = logger;
    this.scheduler = Executors.newSingleThreadScheduledExecutor();
  }

  public void run() {
    logger.log(Level.INFO, "Polling for runnables");
    try {
      if (lockProvider.acquireLock("poll-workflows")) {
        try {
          Iterable<String> runnables = persistence.getRunnableInstances();
          runnables.forEach(
              item -> {
                queueProvider.queueForProcessing(QueueType.WORKFLOW, item);
              });
        } finally {
          lockProvider.releaseLock("poll-workflows");
        }
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }

    try {
      if (lockProvider.acquireLock("poll-events")) {
        try {
          Iterable<String> runnables = persistence.getRunnableEvents();
          runnables.forEach(
              item -> {
                queueProvider.queueForProcessing(QueueType.EVENT, item);
              });
        } finally {
          lockProvider.releaseLock("poll-events");
        }
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, ex.getMessage());
    }
  }

  @Override
  public void start() {
    scheduler.scheduleAtFixedRate(() -> run(), 10, 10, TimeUnit.SECONDS);
  }

  @Override
  public void stop() {
    scheduler.shutdownNow();
  }
}
