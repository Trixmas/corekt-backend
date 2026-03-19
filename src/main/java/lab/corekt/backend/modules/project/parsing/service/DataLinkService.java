package lab.corekt.backend.modules.project.parsing.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lab.corekt.backend.modules.mistral.service.MistralService;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.enums.Severity;
import lab.corekt.backend.modules.project.parsing.repository.DataLinkRepository;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;

@Service
public class DataLinkService {

    private static final int MIN_SEMANTIC_CONFIDENCE = 80; // arbitraire pr le moment

    private final FileRepository fileRepository;
    private final DataLinkRepository dataLinkRepository;
    private final ParsedDataRepository parsedDataRepository;
    private final MistralService mistralService;

    public DataLinkService(FileRepository fileRepository, DataLinkRepository dataLinkRepository,
            ParsedDataRepository parsedDataRepository,
            MistralService mistralService) {
        this.fileRepository = fileRepository;
        this.dataLinkRepository = dataLinkRepository;
        this.parsedDataRepository = parsedDataRepository;
        this.mistralService = mistralService;
    }

    public Map<FileType, FileEntity> getRequiredFiles(VersionEntity version) {
        if (version == null) {
            throw new IllegalArgumentException("Version must not be null");
        }

        Map<FileType, FileEntity> filesByType = new HashMap<>();
        List<FileType> missingTypes = new ArrayList<>();

        // Get all files of the version and check if all required files are present.
        List<FileEntity> allFiles = fileRepository.findByVersion(version);
        for (FileType fileType : FileType.values()) {
            FileEntity file = allFiles.stream().filter(f -> f.getType() == fileType).findFirst().orElse(null);
            if (file != null) {
                filesByType.put(fileType, file);
            } else {
                missingTypes.add(fileType);
            }
        }

        if (!missingTypes.isEmpty()) {
            String missingFiles = String.join(", ",
                    missingTypes.stream().map(FileType::name).toList());

            throw new IllegalStateException("Missing required files for version: " + missingFiles);
        }

        return filesByType;
    }

    public Map<FileType, List<ParsedDataEntity>> getParsedDataByFileType(VersionEntity version) {
       getRequiredFiles(version); // ici c surtout pour la validation, autant ne pas refaire une émthode pr ça 

        List<ParsedDataEntity> parsedData = parsedDataRepository.findByVersion(version);
        Map<FileType, List<ParsedDataEntity>> parsedDataByFileType = new HashMap<>();

        for (ParsedDataEntity data : parsedData) { 
            FileType fileType = data.getFile().getType(); // 
            List<ParsedDataEntity> dataList = parsedDataByFileType.get(fileType); // on récupère le type de fichier dans la map

            if (dataList == null) { // si y'a aucune liste pour ce fileType dans la map
                dataList = new ArrayList<>(); 
                parsedDataByFileType.put(fileType, dataList); 
            }

            dataList.add(data);

        }

        // for (Map.Entry<FileType, FileEntity> entry : requiredFiles.entrySet()) {
        // List<ParsedDataEntity> parsedData =
        // parsedDataRepository.findByFile(entry.getValue());
        // parsedDataByFileType.put(entry.getKey(), parsedData);
        // }

        return parsedDataByFileType;
    }

    public Map<FileType, List<MistralService.EmbeddingTuple>> getEmbeddingsByFileType(VersionEntity version) {
        Map<FileType, List<ParsedDataEntity>> parsedDataByFileType = getParsedDataByFileType(version);
        Map<FileType, List<MistralService.EmbeddingTuple>> embeddingsByFileType = new HashMap<>();

        for (Map.Entry<FileType, List<ParsedDataEntity>> entry : parsedDataByFileType.entrySet()) {
            List<MistralService.EmbeddingTuple> embeddings = mistralService.embedParsedData(entry.getValue());
            embeddingsByFileType.put(entry.getKey(), embeddings);
        }

        return embeddingsByFileType;
    }

    public List<DataLinkEntity> buildDataLinks(VersionEntity version) {
        Map<FileType, List<MistralService.EmbeddingTuple>> embeddingsByFileType = getEmbeddingsByFileType(version);
        List<MistralService.EmbeddingTuple> allEmbeddings = new ArrayList<>();

        for (List<MistralService.EmbeddingTuple> embeddings : embeddingsByFileType.values()) {
            allEmbeddings.addAll(embeddings);
        }

        List<DataLinkEntity> dataLinks = new ArrayList<>();

        for (MistralService.EmbeddingTuple source : allEmbeddings) {
            MistralService.EmbeddingTuple bestTarget = null;
            int bestConfidence = -1;

            for (MistralService.EmbeddingTuple candidate : allEmbeddings) {
                if (!isComparable(source, candidate)) {
                    continue;
                }

                int confidence = getConfidenceScore(source, candidate);

                if (confidence > bestConfidence) {
                    bestConfidence = confidence;
                    bestTarget = candidate;
                }
            }

            if (bestTarget != null && bestConfidence >= MIN_SEMANTIC_CONFIDENCE) {
                MatchType matchType = bestConfidence == 100 ? MatchType.EXACT : MatchType.SEMANTIC;
                DataLinkEntity dataLink = new DataLinkEntity(matchType, Severity.LOW, source.parsedData(),
                        bestTarget.parsedData());
                dataLink.setConfidenceScore(bestConfidence);
                dataLink.setScore(bestConfidence);
                if (matchType == MatchType.EXACT) {
                    dataLink.setJustification("Meme valeur trouvee dans un autre fichier");
                } else {
                    dataLink.setJustification("Valeur proche trouvee dans un autre fichier");
                }
                dataLinks.add(dataLink);
            } else {
                DataLinkEntity dataLink = new DataLinkEntity(MatchType.MISSING, Severity.HIGH, source.parsedData(),
                        source.parsedData());
                dataLink.setConfidenceScore(0);
                dataLink.setScore(0);
                dataLink.setJustification("Aucune correspondance trouvee dans les autres fichiers");
                dataLinks.add(dataLink);
            }
        }

        return dataLinks;
    }

    public List<DataLinkEntity> saveDataLinks(VersionEntity version) {
        return dataLinkRepository.saveAll(buildDataLinks(version));
    }

    private boolean isComparable(MistralService.EmbeddingTuple source, MistralService.EmbeddingTuple candidate) {
        ParsedDataEntity sourceData = source.parsedData();
        ParsedDataEntity candidateData = candidate.parsedData();

        if (sourceData == candidateData) {
            return false;
        }

        if (sourceData.getDataType() != candidateData.getDataType()) {
            return false;
        }

        return sourceData.getFile() != candidateData.getFile();
    }

    private int getConfidenceScore(MistralService.EmbeddingTuple source, MistralService.EmbeddingTuple target) {
        String sourceContent = source.parsedData().getContent();
        String targetContent = target.parsedData().getContent();

        if (sourceContent != null && sourceContent.equalsIgnoreCase(targetContent)) {
            return 100;
        }

        return (int) Math.round(cosineSimilarity(source.embedding(), target.embedding()) * 100);
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0 || left.length != right.length) {
            throw new IllegalArgumentException("Embeddings must have the same non-empty size");
        }

        double dotProduct = 0;
        double leftNorm = 0;
        double rightNorm = 0;

        for (int i = 0; i < left.length; i++) {
            dotProduct += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }

        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }

        return dotProduct / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
