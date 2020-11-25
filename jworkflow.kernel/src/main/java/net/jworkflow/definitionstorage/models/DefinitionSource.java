package net.jworkflow.definitionstorage.models;

import net.jworkflow.kernel.models.ErrorBehavior;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class DefinitionSource {

  public int schemaVersion;

  public String id;

  public int version;

  public String description;

  public String dataType;

  public ErrorBehavior defaultErrorBehavior;

  public Duration defaultErrorRetryInterval;

  public List<StepSource> steps = new ArrayList<>();
}
