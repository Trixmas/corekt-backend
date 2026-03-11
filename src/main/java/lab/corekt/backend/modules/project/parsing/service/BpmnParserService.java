package lab.corekt.backend.modules.project.parsing.service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.Task;
import org.springframework.stereotype.Service;

import lab.corekt.backend.modules.project.parsing.enums.DataType;

@Service
public class BpmnParserService {

    private BpmnModelInstance readModel(byte[] bpmnFile) {
        return Bpmn.readModelFromStream(new ByteArrayInputStream(bpmnFile));
    }

    private <T extends BpmnModelElementInstance> List<String> extractElements(byte[] bpmnFile, Class<T> type,
            Function<T, String> func) {
        BpmnModelInstance model = this.readModel(bpmnFile);
        List<String> results = new ArrayList<>();

        for (T element : model.getModelElementsByType(type)) {
            String value = func.apply(element);
            if (value != null && !value.isBlank()) {
                results.add(value);
            }
        }

        return results;
    }

    public List<String> extract(byte[] bpmnFile, DataType dataType) {
        switch (dataType) {
            case ACTOR -> {
                return extractElements(bpmnFile, Participant.class, Participant::getName);
            }
            case ACTION -> {
                return extractElements(bpmnFile, Task.class, Task::getName);
            }
            case FLOW -> {
                return extractElements(bpmnFile, SequenceFlow.class, flow -> {
                    String sourceLabel = nameOrType(flow.getSource());
                    String targetLabel = nameOrType(flow.getTarget());
                    return sourceLabel + " -> " + targetLabel;
                });
            }
            default -> throw new IllegalArgumentException("Unsupported DataType for BPMN: " + dataType);
        }
    }

    private String nameOrType(BpmnModelElementInstance element) {
        String name = element.getAttributeValue("name");
        return (name != null && !name.isBlank()) ? name : element.getElementType().getTypeName();
    }
}
