package lab.corekt.backend.modules.project.parsing.service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.springframework.stereotype.Service;

@Service
public class BpmnParserService {

    public List<String> extractTaskNames(byte[] bpmnFile) {
        BpmnModelInstance model = Bpmn.readModelFromStream(new ByteArrayInputStream(bpmnFile));
        List<String> names = new ArrayList<>();

        for (Task task : model.getModelElementsByType(Task.class)) {
            String name = task.getName();
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }

        return names;
    }
}
