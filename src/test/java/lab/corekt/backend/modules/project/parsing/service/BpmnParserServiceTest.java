package lab.corekt.backend.modules.project.parsing.service;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import lab.corekt.backend.modules.project.parsing.enums.DataType;

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
    byte[] bpmn = SIMPLE_BPMN.getBytes();

    List<String> taskNames = parser.extract(bpmn, DataType.ACTION);

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

    List<String> taskNames = parser.extract(noTaskBpmn.getBytes(), DataType.ACTION);

    assertTrue(taskNames.isEmpty());
  }

  @Test
  void shouldExtractSixTaskNames() {
    String bpmn = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI" id="Definitions_12nhh4u" targetNamespace="http://bpmn.io/schema/bpmn" exporter="bpmn-js (https://demo.bpmn.io)" exporterVersion="18.13.1">
          <bpmn:process id="Process_1xgm600" isExecutable="false">
            <bpmn:startEvent id="StartEvent_1xcfacd">
              <bpmn:outgoing>Flow_1y7hot8</bpmn:outgoing>
            </bpmn:startEvent>
            <bpmn:task id="Activity_0y7kxrz" name="A01: Bonjour">
              <bpmn:incoming>Flow_1y7hot8</bpmn:incoming>
              <bpmn:outgoing>Flow_1jz7rpi</bpmn:outgoing>
            </bpmn:task>
            <bpmn:sequenceFlow id="Flow_1y7hot8" sourceRef="StartEvent_1xcfacd" targetRef="Activity_0y7kxrz" />
            <bpmn:task id="Activity_0onw0eq" name="A02 : Oui">
              <bpmn:incoming>Flow_1jz7rpi</bpmn:incoming>
              <bpmn:outgoing>Flow_0nvaefl</bpmn:outgoing>
            </bpmn:task>
            <bpmn:sequenceFlow id="Flow_1jz7rpi" sourceRef="Activity_0y7kxrz" targetRef="Activity_0onw0eq" />
            <bpmn:task id="Activity_1tvp0mo" name="A03: Texte à faire voir par le dessus de le bord">
              <bpmn:incoming>Flow_0nvaefl</bpmn:incoming>
              <bpmn:outgoing>Flow_1u019b4</bpmn:outgoing>
            </bpmn:task>
            <bpmn:sequenceFlow id="Flow_0nvaefl" sourceRef="Activity_0onw0eq" targetRef="Activity_1tvp0mo" />
            <bpmn:sequenceFlow id="Flow_1u019b4" sourceRef="Activity_1tvp0mo" targetRef="Activity_1xejkfq" />
            <bpmn:task id="Activity_1xejkfq" name="A04 : c&#39;est à dire ?">
              <bpmn:incoming>Flow_1u019b4</bpmn:incoming>
              <bpmn:outgoing>Flow_1pmhvgt</bpmn:outgoing>
            </bpmn:task>
            <bpmn:task id="Activity_13x8kci" name="A05 : Encore une activité">
              <bpmn:incoming>Flow_1pmhvgt</bpmn:incoming>
              <bpmn:outgoing>Flow_0089vbp</bpmn:outgoing>
            </bpmn:task>
            <bpmn:sequenceFlow id="Flow_1pmhvgt" sourceRef="Activity_1xejkfq" targetRef="Activity_13x8kci" />
            <bpmn:task id="Activity_1i4qled" name="A06 : Et alors ?">
              <bpmn:incoming>Flow_0089vbp</bpmn:incoming>
              <bpmn:outgoing>Flow_1d6s6kp</bpmn:outgoing>
            </bpmn:task>
            <bpmn:sequenceFlow id="Flow_0089vbp" sourceRef="Activity_13x8kci" targetRef="Activity_1i4qled" />
            <bpmn:endEvent id="Event_1m7y5zy">
              <bpmn:incoming>Flow_1d6s6kp</bpmn:incoming>
            </bpmn:endEvent>
            <bpmn:sequenceFlow id="Flow_1d6s6kp" sourceRef="Activity_1i4qled" targetRef="Event_1m7y5zy" />
          </bpmn:process>
          <bpmndi:BPMNDiagram id="BPMNDiagram_1">
            <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1xgm600" />
          </bpmndi:BPMNDiagram>
        </bpmn:definitions>
        """;

    List<String> taskNames = parser.extract(bpmn.getBytes(StandardCharsets.UTF_8), DataType.ACTION);

    assertEquals(6, taskNames.size());
    assertTrue(taskNames.contains("A01: Bonjour"));
    assertTrue(taskNames.contains("A02 : Oui"));
    assertTrue(taskNames.contains("A03: Texte à faire voir par le dessus de le bord"));
    assertTrue(taskNames.contains("A04 : c'est à dire ?"));
    assertTrue(taskNames.contains("A05 : Encore une activité"));
    assertTrue(taskNames.contains("A06 : Et alors ?"));
  }

  // https://gibson-pages.telecomnancy.univ-lorraine.fr/francois.charoy/coursbpm/td/corrections/TD2_Correction/
  @Test
  void shouldExtractParticipantNames() {
    List<String> participants = parser.extract(COLLABORATION_BPMN.getBytes(), DataType.ACTOR);

    assertEquals(3, participants.size());
    assertTrue(participants.contains("Employé"));
    assertTrue(participants.contains("Manager"));
    assertTrue(participants.contains("Service RH"));
  }

  @Test
  void shouldReturnEmptyListForNoParticipants() {
    List<String> participants = parser.extract(SIMPLE_BPMN.getBytes(), DataType.ACTOR);

    assertTrue(participants.isEmpty());
  }

  // https://gibson-pages.telecomnancy.univ-lorraine.fr/francois.charoy/coursbpm/td/corrections/TD2_Correction/
  private static final String COLLABORATION_BPMN = """
      <?xml version="1.0" encoding="UTF-8"?>
      <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                        id="Definitions_4"
                        targetNamespace="http://bpmn.io/schema/bpmn">

        <bpmn:collaboration id="Collaboration_DemandeConges">

          <bpmn:participant id="Participant_Employe" name="Employé" processRef="Process_Employe" />

          <bpmn:participant id="Participant_Manager" name="Manager" processRef="Process_Manager" />

          <bpmn:participant id="Participant_RH" name="Service RH" processRef="Process_RH" />

          <bpmn:messageFlow id="MessageFlow_Demande" sourceRef="Task_SoumettreDemande" targetRef="StartEvent_DemandeRecue" />

          <bpmn:messageFlow id="MessageFlow_Validation" sourceRef="Task_Valider" targetRef="Event_ValidationRecue" />

          <bpmn:messageFlow id="MessageFlow_Enregistrement" sourceRef="Task_Enregistrer" targetRef="Event_Confirmation" />

        </bpmn:collaboration>

        <bpmn:process id="Process_Employe" isExecutable="true">

          <bpmn:startEvent id="StartEvent_BesoinConge" name="Besoin de&#10;congés" />

          <bpmn:task id="Task_SoumettreDemande" name="Soumettre demande via formulaire" />

          <bpmn:intermediateCatchEvent id="Event_Confirmation" name="Réception&#10;confirmation" />

          <bpmn:endEvent id="EndEvent_CongesPlanifies" name="Congés&#10;planifiés" />

          <bpmn:sequenceFlow sourceRef="StartEvent_BesoinConge" targetRef="Task_SoumettreDemande" />
          <bpmn:sequenceFlow sourceRef="Task_SoumettreDemande" targetRef="Event_Confirmation" />
          <bpmn:sequenceFlow sourceRef="Event_Confirmation" targetRef="EndEvent_CongesPlanifies" />

        </bpmn:process>

        <bpmn:process id="Process_Manager" isExecutable="true">

          <bpmn:startEvent id="StartEvent_DemandeRecue" name="Demande&#10;reçue" />

          <bpmn:task id="Task_Analyser" name="Analyser disponibilité équipe" />

          <bpmn:exclusiveGateway id="Gateway_Decision" name="Accepter ?" />

          <bpmn:task id="Task_Valider" name="Valider la demande" />

          <bpmn:task id="Task_Rejeter" name="Rejeter avec motif" />

          <bpmn:endEvent id="EndEvent_Approuve" name="Demande&#10;approuvée" />

          <bpmn:endEvent id="EndEvent_Rejete" name="Demande&#10;rejetée" />

          <bpmn:sequenceFlow sourceRef="StartEvent_DemandeRecue" targetRef="Task_Analyser" />
          <bpmn:sequenceFlow sourceRef="Task_Analyser" targetRef="Gateway_Decision" />

          <bpmn:sequenceFlow name="Oui" sourceRef="Gateway_Decision" targetRef="Task_Valider">
            <bpmn:conditionExpression>#{approuve == true}</bpmn:conditionExpression>
          </bpmn:sequenceFlow>

          <bpmn:sequenceFlow name="Non" sourceRef="Gateway_Decision" targetRef="Task_Rejeter">
            <bpmn:conditionExpression>#{approuve == false}</bpmn:conditionExpression>
          </bpmn:sequenceFlow>

          <bpmn:sequenceFlow sourceRef="Task_Valider" targetRef="EndEvent_Approuve" />
          <bpmn:sequenceFlow sourceRef="Task_Rejeter" targetRef="EndEvent_Rejete" />

        </bpmn:process>

        <bpmn:process id="Process_RH" isExecutable="true">

          <bpmn:startEvent id="StartEvent_ValidationRecue" name="Validation&#10;reçue" />

          <bpmn:task id="Task_Verifier" name="Vérifier solde congés" />

          <bpmn:task id="Task_Enregistrer" name="Enregistrer dans SIRH" />

          <bpmn:task id="Task_Notifier" name="Notifier l'employé" />

          <bpmn:endEvent id="EndEvent_Archive" name="Dossier&#10;archivé" />

          <bpmn:sequenceFlow sourceRef="StartEvent_ValidationRecue" targetRef="Task_Verifier" />
          <bpmn:sequenceFlow sourceRef="Task_Verifier" targetRef="Task_Enregistrer" />
          <bpmn:sequenceFlow sourceRef="Task_Enregistrer" targetRef="Task_Notifier" />
          <bpmn:sequenceFlow sourceRef="Task_Notifier" targetRef="EndEvent_Archive" />

        </bpmn:process>

      </bpmn:definitions>
              """;

  @Test
  void shouldExtractSequenceFlows() {
    List<String> flows = parser.extract(SIMPLE_BPMN.getBytes(), DataType.FLOW);

    assertEquals(3, flows.size());
    assertTrue(flows.contains("startEvent -> A06: Créer un projet"));
    assertTrue(flows.contains("A06: Créer un projet -> A07: Terminer le projet"));
    assertTrue(flows.contains("A07: Terminer le projet -> endEvent"));
  }

  @Test
  void shouldReturnEmptyListForNoFlows() {
    String noFlowBpmn = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL" xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI" id="Definitions_1hi09mf" targetNamespace="http://bpmn.io/schema/bpmn" exporter="bpmn-js (https://demo.bpmn.io)" exporterVersion="18.13.1">
          <bpmn:process id="Process_05bei3p" isExecutable="false" />
          <bpmndi:BPMNDiagram id="BPMNDiagram_1">
            <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_05bei3p" />
          </bpmndi:BPMNDiagram>
        </bpmn:definitions>
                """;

    List<String> flows = parser.extract(noFlowBpmn.getBytes(), DataType.FLOW);

    assertTrue(flows.isEmpty());
  }

  @Test
  void shouldExtractActionsFromCollaborationBpmn() {
    List<String> actions = parser.extract(COLLABORATION_BPMN.getBytes(), DataType.ACTION);

    assertEquals(7, actions.size());
    assertTrue(actions.contains("Soumettre demande via formulaire"));
    assertTrue(actions.contains("Analyser disponibilité équipe"));
    assertTrue(actions.contains("Valider la demande"));
    assertTrue(actions.contains("Rejeter avec motif"));
    assertTrue(actions.contains("Vérifier solde congés"));
    assertTrue(actions.contains("Enregistrer dans SIRH"));
    assertTrue(actions.contains("Notifier l'employé"));
  }

  @Test
  void shouldExtractFlowsFromCollaborationBpmn() {
    List<String> flows = parser.extract(COLLABORATION_BPMN.getBytes(), DataType.FLOW);

    assertEquals(13, flows.size());
    assertTrue(flows.contains("Soumettre demande via formulaire -> Réception\nconfirmation"));
    assertTrue(flows.contains("Valider la demande -> Demande\napprouvée"));
    assertTrue(flows.contains("Analyser disponibilité équipe -> Accepter ?"));
    assertTrue(flows.contains("Accepter ? -> Valider la demande"));
    assertTrue(flows.contains("Accepter ? -> Rejeter avec motif"));
  }

  @Test
  void shouldThrowForUnsupportedDataType() {
    assertThrows(IllegalArgumentException.class, () -> {
      parser.extract(SIMPLE_BPMN.getBytes(), DataType.US);
    });
  }
}
