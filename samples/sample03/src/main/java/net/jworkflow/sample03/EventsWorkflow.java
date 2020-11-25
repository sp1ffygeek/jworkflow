package net.jworkflow.sample03;

import net.jworkflow.sample03.steps.Hello;
import net.jworkflow.sample03.steps.DisplayAnswer;
import net.jworkflow.kernel.interfaces.Workflow;
import net.jworkflow.kernel.interfaces.WorkflowBuilder;

public class EventsWorkflow implements Workflow<MyData> {

    @Override
    public String getId() {
        return "events-workflow";
    }

    @Override
    public Class getDataType() {
        return MyData.class;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void build(WorkflowBuilder<MyData> builder) {        
        builder
            .startsWith(Hello.class)
            .waitFor("myEvent", x -> "1")
                .output((step, data) -> data.value1 = step.eventData)
            .then(DisplayAnswer.class)
                .input((step, data) -> step.answer = data.value1);
    }    
}
