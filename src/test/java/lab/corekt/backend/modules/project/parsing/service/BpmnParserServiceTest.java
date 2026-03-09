package lab.corekt.backend.modules.project.parsing.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class BpmnParserServiceTest {

  private final BpmnParserService parser = new BpmnParserService();

  private static final String SIMPLE_BPMN = """
      <?xml version="1.0" encoding="UTF-8"?>
      <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" id="Definitions_1hi09mf" targetNamespace="http://bpmn.io/schema/bpmn" exporter="bpmn-js (https://demo.bpmn.io)" exporterVersion="18.13.1">
        <bpmn:process id="Process_05bei3p" isExecutable="false">
          <bpmn:startEvent id="StartEvent_1b6qsve">
            <bpmn:outgoing>Flow_1c9tvrv</bpmn:outgoing>
          </bpmn:startEvent>
          <bpmn:task id="Activity_1y0806i" name="A06: Créer un projet">
            <bpmn:incoming>Flow_1c9tvrv</bpmn:incoming>
            <bpmn:outgoing>Flow_1szaemp</bpmn:outgoing>
          </bpmn:task>
          <bpmn:sequenceFlow id="Flow_1c9tvrv" sourceRef="StartEvent_1b6qsve" targetRef="Activity_1y0806i" />
          <bpmn:task id="Activity_1ezqbpg" name="A07: Terminer le projet">
            <bpmn:incoming>Flow_1szaemp</bpmn:incoming>
            <bpmn:outgoing>Flow_1xcm9is</bpmn:outgoing>
          </bpmn:task>
          <bpmn:sequenceFlow id="Flow_1szaemp" sourceRef="Activity_1y0806i" targetRef="Activity_1ezqbpg" />
          <bpmn:endEvent id="Event_0a7e2wt">
            <bpmn:incoming>Flow_1xcm9is</bpmn:incoming>
          </bpmn:endEvent>
          <bpmn:sequenceFlow id="Flow_1xcm9is" sourceRef="Activity_1ezqbpg" targetRef="Event_0a7e2wt" />
        </bpmn:process>
        <bpmndi:BPMNDiagram id="BPMNDiagram_1">
          <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_05bei3p">
            <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1b6qsve">
              <dc:Bounds x="156" y="82" width="36" height="36" />
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="Activity_1y0806i_di" bpmnElement="Activity_1y0806i">
              <dc:Bounds x="330" y="60" width="100" height="80" />
              <bpmndi:BPMNLabel />
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="Activity_1ezqbpg_di" bpmnElement="Activity_1ezqbpg">
              <dc:Bounds x="570" y="60" width="100" height="80" />
              <bpmndi:BPMNLabel />
            </bpmndi:BPMNShape>
            <bpmndi:BPMNShape id="Event_0a7e2wt_di" bpmnElement="Event_0a7e2wt">
              <dc:Bounds x="812" y="82" width="36" height="36" />
            </bpmndi:BPMNShape>
            <bpmndi:BPMNEdge id="Flow_1c9tvrv_di" bpmnElement="Flow_1c9tvrv">
              <di:waypoint x="192" y="100" />
              <di:waypoint x="330" y="100" />
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="Flow_1szaemp_di" bpmnElement="Flow_1szaemp">
              <di:waypoint x="430" y="100" />
              <di:waypoint x="570" y="100" />
            </bpmndi:BPMNEdge>
            <bpmndi:BPMNEdge id="Flow_1xcm9is_di" bpmnElement="Flow_1xcm9is">
              <di:waypoint x="670" y="100" />
              <di:waypoint x="812" y="100" />
            </bpmndi:BPMNEdge>
          </bpmndi:BPMNPlane>
        </bpmndi:BPMNDiagram>
      </bpmn:definitions>
                  """;

  @Test
  void shouldExtractTaskNames() {
    byte[] bpmn = SIMPLE_BPMN.getBytes(StandardCharsets.UTF_8);

    List<String> taskNames = parser.extractTaskNames(bpmn);

    assertEquals(2, taskNames.size());
    assertTrue(taskNames.contains("A06: Créer un projet"));
    assertTrue(taskNames.contains("A07: Terminer le projet"));
  }

  @Test
  void shouldReturnEmptyListForNoTasks() {
    String noTaskBpmn = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" id="Definitions_1hi09mf" targetNamespace="http://bpmn.io/schema/bpmn" exporter="bpmn-js (https://demo.bpmn.io)" exporterVersion="18.13.1">
          <bpmn:process id="Process_05bei3p" isExecutable="false" />
          <bpmndi:BPMNDiagram id="BPMNDiagram_1">
            <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_05bei3p" />
          </bpmndi:BPMNDiagram>
        </bpmn:definitions>
                """;

    List<String> taskNames = parser.extractTaskNames(noTaskBpmn.getBytes(StandardCharsets.UTF_8));

    assertTrue(taskNames.isEmpty());
  }
}
