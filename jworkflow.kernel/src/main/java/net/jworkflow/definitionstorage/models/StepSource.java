package net.jworkflow.definitionstorage.models;

import net.jworkflow.kernel.models.ErrorBehavior;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class StepSource {

  public String stepType;

  public String id;

  public String name;

  public String cancelCondition;

  public ErrorBehavior errorBehavior;

  public Duration retryInterval;

  public List<List<StepSource>> thenDo = new ArrayList<>();

  public List<StepSource> compensateWith = new ArrayList<>();

  public boolean saga = false;

  public String nextStepId;

  public Properties inputs = new Properties();

  public Properties outputs = new Properties();
}
