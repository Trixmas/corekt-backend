package lab.corekt.backend.modules.project.parsing.service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
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

    public List<String> extractParticipantNames(byte[] bpmnFile) {
        BpmnModelInstance model = Bpmn.readModelFromStream(new ByteArrayInputStream(bpmnFile));
        List<String> names = new ArrayList<>();

        for (Participant participant : model.getModelElementsByType(Participant.class)) {
            String name = participant.getName();
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }

        return names;
    }

    public List<String> extractSequenceFlows(byte[] bpmnFile) {
        BpmnModelInstance model = Bpmn.readModelFromStream(new ByteArrayInputStream(bpmnFile));
        List<String> flows = new ArrayList<>();

        for (SequenceFlow flow : model.getModelElementsByType(SequenceFlow.class)) {
            String sourceName = flow.getSource().getName();
            String targetName = flow.getTarget().getName();

            // if there is a custom name, and not empty, we use it
            // else, we use the type of the element
            String sourceLabel = (sourceName != null && !sourceName.isBlank())
                    ? sourceName
                    : flow.getSource().getElementType().getTypeName();

            // same here
            String targetLabel = (targetName != null && !targetName.isBlank())
                    ? targetName
                    : flow.getTarget().getElementType().getTypeName();

            String content = sourceLabel + " -> " + targetLabel;

            flows.add(content);
        }

        return flows;
    }

    public List<String> extractGateways(byte[] bpmnFile) {
        BpmnModelInstance model = Bpmn.readModelFromStream(new ByteArrayInputStream(bpmnFile));
        List<String> gateways = new ArrayList<>();

        for (Gateway gateway : model.getModelElementsByType(Gateway.class)) {
            String name = gateway.getName();
            String type = gateway.getElementType().getTypeName();

            String label = (name != null && !name.isBlank())
                    ? type + ": " + name
                    : type;

            gateways.add(label);
        }

        return gateways;
    }
}
