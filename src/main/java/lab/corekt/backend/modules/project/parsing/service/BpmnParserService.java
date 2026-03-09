package lab.corekt.backend.modules.project.parsing.service;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.springframework.stereotype.Service;

@Service
public class BpmnParserService {

    public List<String> extractTaskNames(byte[] bpmnFile) {
        BpmnModelInstance model = Bpmn.readModelFromStream(new ByteArrayInputStream(bpmnFile));
        Collection<Task> tasks = model.getModelElementsByType(Task.class);

        return tasks.stream()
                .map(Task::getName)
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }
}
