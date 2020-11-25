package net.jworkflow;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Providers;
import net.jworkflow.definitionstorage.services.DefaultDefinitionLoader;
import net.jworkflow.definitionstorage.services.DefinitionLoader;
import net.jworkflow.kernel.errorhandlers.CompensateHandler;
import net.jworkflow.kernel.errorhandlers.RetryHandler;
import net.jworkflow.kernel.errorhandlers.SuspendHandler;
import net.jworkflow.kernel.errorhandlers.TerminateHandler;
import net.jworkflow.kernel.interfaces.*;
import net.jworkflow.kernel.services.*;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.time.Clock;

public class WorkflowModule extends AbstractModule {

  public Provider<? extends PersistenceService> persistenceProvider;
  public Provider<? extends QueueService> queueProvider;
  public Provider<? extends LockService> lockProvider;
  private Injector injector;

  public WorkflowModule() {
    persistenceProvider = Providers.of(new MemoryPersistenceService());
    queueProvider = Providers.of(new SingleNodeQueueService());
    lockProvider = Providers.of(new SingleNodeLockService());
  }

  @Override
  protected void configure() {
    bind(WorkflowHost.class).to(DefaultWorkflowHost.class);
    bind(WorkflowExecutor.class).to(DefaultWorkflowExecutor.class);
    bind(WorkflowRegistry.class).to(DefaultWorkflowRegistry.class);
    bind(ExecutionPointerFactory.class).to(DefaultExecutionPointerFactory.class);
    bind(ExecutionResultProcessor.class).to(DefaultExecutionResultProcessor.class);
    bind(DefinitionLoader.class).to(DefaultDefinitionLoader.class);
    bind(Clock.class).toInstance(Clock.systemUTC());
    bind(ScriptEngine.class).toInstance(new ScriptEngineManager().getEngineByName("nashorn"));

    Multibinder<BackgroundService> backgroundServiceBinder =
        Multibinder.newSetBinder(binder(), BackgroundService.class);
    backgroundServiceBinder.addBinding().to(WorkflowWorker.class);
    backgroundServiceBinder.addBinding().to(EventWorker.class);
    backgroundServiceBinder.addBinding().to(PollThread.class);

    Multibinder<StepErrorHandler> errorHandlerBinder =
        Multibinder.newSetBinder(binder(), StepErrorHandler.class);
    errorHandlerBinder.addBinding().to(RetryHandler.class);
    errorHandlerBinder.addBinding().to(CompensateHandler.class);
    errorHandlerBinder.addBinding().to(SuspendHandler.class);
    errorHandlerBinder.addBinding().to(TerminateHandler.class);

    bind(PersistenceService.class).toProvider(persistenceProvider);
    bind(LockService.class).toProvider(lockProvider).asEagerSingleton();
    bind(QueueService.class).toProvider(queueProvider).asEagerSingleton();
  }

  public void build() {
    injector = Guice.createInjector(this);
  }

  public void usePersistence(Provider<? extends PersistenceService> persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  public void useQueue(Provider<? extends QueueService> queueProvider) {
    this.queueProvider = queueProvider;
  }

  public void useDistibutedLock(Provider<? extends LockService> lockProvider) {
    this.lockProvider = lockProvider;
  }

  public WorkflowHost getHost() {
    if (injector != null) return injector.getInstance(WorkflowHost.class);
    return null;
  }

  public PersistenceService getPersistenceProvider() {
    if (injector != null) return injector.getInstance(PersistenceService.class);
    return null;
  }

  public DefinitionLoader getLoader() {
    if (injector != null) return injector.getInstance(DefinitionLoader.class);
    return null;
  }
}
