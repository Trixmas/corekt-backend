package lab.corekt.backend.modules.project.parsing.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.upload.entity.FileEntity;

@ExtendWith(MockitoExtension.class)
class BpmnParserServiceTest {

  @Mock
  private ParsedDataRepository parsedDataRepository;

  @InjectMocks
  private BpmnParserService parser;

  private static final String SIMPLE_BPMN;
  private static final String COLLABORATION_BPMN;
  private static final String NO_TASKS_BPMN;
  private static final String COMPLEX_BPMN;
  private static final String EMPTY_BPMN;

  static {
    try {
      SIMPLE_BPMN = loadResourceFile("test_bpmn.bpmn");
      COLLABORATION_BPMN = loadResourceFile("collaboration_bpmn.bpmn");
      NO_TASKS_BPMN = loadResourceFile("no_tasks_bpmn.bpmn");
      COMPLEX_BPMN = loadResourceFile("complex_bpmn.bpmn");
      EMPTY_BPMN = loadResourceFile("empty_bpmn.bpmn");
    } catch (IOException e) {
      throw new RuntimeException("Failed to load test resources", e);
    }
  }

  private static String loadResourceFile(String filename) throws IOException {
    String resourcePath = "src/test/resources/" + filename;
    return Files.readString(Paths.get(resourcePath), StandardCharsets.UTF_8);
  }


  @Test
  @DisplayName("Should extract task names from BPMN file")
  void shouldExtractTaskNames() {
    byte[] bpmn = SIMPLE_BPMN.getBytes();

    List<String> taskNames = parser.extract(bpmn, DataType.ACTION);

    assertEquals(2, taskNames.size());
    assertTrue(taskNames.contains("A06: Créer un projet"));
    assertTrue(taskNames.contains("A07: Terminer le projet"));
  }

  @Test
  @DisplayName("Should return empty list when no tasks exist")
  void shouldReturnEmptyListForNoTasks() {
    List<String> taskNames = parser.extract(NO_TASKS_BPMN.getBytes(), DataType.ACTION);

    assertTrue(taskNames.isEmpty());
  }

  @Test
  @DisplayName("Should extract all six task names from complex BPMN")
  void shouldExtractSixTaskNames() {
    List<String> taskNames = parser.extract(COMPLEX_BPMN.getBytes(StandardCharsets.UTF_8), DataType.ACTION);

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
  @DisplayName("Should extract participant names from collaboration BPMN")
  void shouldExtractParticipantNames() {
    List<String> participants = parser.extract(COLLABORATION_BPMN.getBytes(), DataType.ACTOR);

    assertEquals(3, participants.size());
    assertTrue(participants.contains("Employé"));
    assertTrue(participants.contains("Manager"));
    assertTrue(participants.contains("Service RH"));
  }

  @Test
  @DisplayName("Should return empty list when no participants exist")
  void shouldReturnEmptyListForNoParticipants() {
    List<String> participants = parser.extract(SIMPLE_BPMN.getBytes(), DataType.ACTOR);

    assertTrue(participants.isEmpty());
  }

  @Test
  @DisplayName("Should extract sequence flows from BPMN")
  void shouldExtractSequenceFlows() {
    List<String> flows = parser.extract(SIMPLE_BPMN.getBytes(), DataType.FLOW);

    assertEquals(3, flows.size());
    assertTrue(flows.contains("startEvent -> A06: Créer un projet"));
    assertTrue(flows.contains("A06: Créer un projet -> A07: Terminer le projet"));
    assertTrue(flows.contains("A07: Terminer le projet -> endEvent"));
  }

  @Test
  @DisplayName("Should return empty list when no sequence flows exist")
  void shouldReturnEmptyListForNoFlows() {
    List<String> flows = parser.extract(EMPTY_BPMN.getBytes(), DataType.FLOW);

    assertTrue(flows.isEmpty());
  }

  @Test
  @DisplayName("Should extract actions from collaboration BPMN")
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
  @DisplayName("Should extract flows from collaboration BPMN")
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
  @DisplayName("Should throw IllegalArgumentException for unsupported DataType")
  void shouldThrowForUnsupportedDataType() {
    assertThrows(IllegalArgumentException.class, () -> parser.extract(SIMPLE_BPMN.getBytes(), DataType.US));
  }

  @Test
  @DisplayName("Should process BPMN file and save parsed data to repository")
  void shouldProcessFileAndSaveParsedData() throws Exception {
    FileEntity fileEntity = new FileEntity();
    fileEntity.setId(UUID.randomUUID());
    fileEntity.setFile(SIMPLE_BPMN.getBytes());
    fileEntity.setName("test.bpmn");

    parser.processFile(fileEntity);

    verify(parsedDataRepository).saveAll(anyList());
  }

  @Test
  @DisplayName("Should process complex BPMN file and save all extracted data")
  void shouldProcessComplexBpmnFileAndSaveAllData() throws Exception {
    FileEntity fileEntity = new FileEntity();
    fileEntity.setId(UUID.randomUUID());
    fileEntity.setFile(COLLABORATION_BPMN.getBytes());
    fileEntity.setName("collaboration.bpmn");

    parser.processFile(fileEntity);

    verify(parsedDataRepository).saveAll(anyList());
  }

  @Test
  @DisplayName("Should handle empty BPMN file without errors")
  void shouldHandleEmptyBpmnFile() throws Exception {
    FileEntity fileEntity = new FileEntity();
    fileEntity.setId(UUID.randomUUID());
    fileEntity.setFile(EMPTY_BPMN.getBytes());
    fileEntity.setName("empty.bpmn");

    parser.processFile(fileEntity);

    verify(parsedDataRepository).saveAll(anyList());
  }
}
