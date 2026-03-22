package lab.corekt.backend.modules.project.parsing.service;

import lab.corekt.backend.modules.mistral.service.MistralService;
import lab.corekt.backend.modules.mistral.service.MistralService.EmbeddingTuple;
import lab.corekt.backend.modules.project.parsing.entity.DataLinkEntity;
import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import lab.corekt.backend.modules.project.parsing.enums.DataType;
import lab.corekt.backend.modules.project.parsing.enums.FileType;
import lab.corekt.backend.modules.project.parsing.enums.MatchType;
import lab.corekt.backend.modules.project.parsing.enums.Severity;
import lab.corekt.backend.modules.project.parsing.repository.DataLinkRepository;
import lab.corekt.backend.modules.project.parsing.repository.ParsedDataRepository;
import lab.corekt.backend.modules.project.versions.entity.VersionEntity;
import lab.corekt.backend.modules.upload.entity.FileEntity;
import lab.corekt.backend.modules.upload.repository.FileRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;
@Service
public class DataLinkService {

    private static final int MIN_SEMANTIC_CONFIDENCE = 70; // arbitraire pour le moment
    private static final Map<DataProfile, Set<DataProfile>> COMPATIBILITY_MATRIX = buildCompatibilityMatrix();

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

    private static Map<DataProfile, Set<DataProfile>> buildCompatibilityMatrix() {
        Map<DataProfile, Set<DataProfile>> matrix = new HashMap<>();

        registerCompatible(matrix,
                new DataProfile(FileType.BPMN, DataType.ACTOR),
                new DataProfile(FileType.INTERVIEW, DataType.ACTOR));

        registerCompatible(matrix,
                new DataProfile(FileType.BPMN, DataType.ACTION),
                new DataProfile(FileType.INTERVIEW, DataType.ACTION));

        registerCompatible(matrix,
                new DataProfile(FileType.BPMN, DataType.FLOW),
                new DataProfile(FileType.INTERVIEW, DataType.FLOW));

        registerCompatible(matrix,
                new DataProfile(FileType.MCD, DataType.ENTITY),
                new DataProfile(FileType.INTERVIEW, DataType.ENTITY));

        registerCompatible(matrix,
                new DataProfile(FileType.US, DataType.US),
                new DataProfile(FileType.INTERVIEW, DataType.ACTION));

        return matrix;
    }

    private static void registerCompatible(Map<DataProfile, Set<DataProfile>> matrix,
                                           DataProfile left,
                                           DataProfile right) {
        matrix.computeIfAbsent(left, ignored -> new HashSet<>()).add(right);
        matrix.computeIfAbsent(right, ignored -> new HashSet<>()).add(left);
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

    public Map<FileType, List<EmbeddingTuple>> getEmbeddingsByFileType(VersionEntity version) {
        Map<FileType, List<ParsedDataEntity>> parsedDataByFileType = getParsedDataByFileType(version);
        Map<FileType, List<EmbeddingTuple>> embeddingsByFileType = new HashMap<>();

        for (Map.Entry<FileType, List<ParsedDataEntity>> entry : parsedDataByFileType.entrySet()) {
            List<EmbeddingTuple> embeddings = mistralService.embedParsedData(entry.getValue());
            embeddingsByFileType.put(entry.getKey(), embeddings);
        }

        return embeddingsByFileType;
    }

    public List<DataLinkEntity> buildDataLinks(VersionEntity version) {
        Map<FileType, List<EmbeddingTuple>> embeddingsByFileType = getEmbeddingsByFileType(version);
        List<EmbeddingTuple> allEmbeddings = new ArrayList<>();

        for (List<EmbeddingTuple> embeddings : embeddingsByFileType.values()) {
            allEmbeddings.addAll(embeddings);
        }

        Map<DataProfile, List<EmbeddingTuple>> embeddingsByProfile = indexByProfile(allEmbeddings);
        List<DataLinkEntity> dataLinks = new ArrayList<>();

        for (EmbeddingTuple source : allEmbeddings) {
            EmbeddingTuple bestTarget = null;
            int bestConfidence = -1;

            for (EmbeddingTuple candidate : getComparableCandidates(source, embeddingsByProfile)) {
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
                DataLinkEntity dataLink = createDataLink(source, bestConfidence, bestTarget);
                dataLinks.add(dataLink);
            } else {
                DataLinkEntity dataLink = new DataLinkEntity(MatchType.MISSING, Severity.HIGH, source.parsedData(),
                        source.parsedData());
                dataLink.setConfidenceScore(0);
                dataLink.setScore(0);
                dataLink.setJustification(String.format("Aucune correspondance trouvée pour le type %s de valeur %s entre le fichier %s et %s.", source.parsedData().getDataType(), source.parsedData().getContent(), source.parsedData().getFile().getName(), bestTarget == null ? "aucun fichier" : bestTarget.parsedData().getFile().getName()));
                dataLinks.add(dataLink);
            }
        }

        return dataLinks;
    }

    private static @NonNull DataLinkEntity createDataLink(EmbeddingTuple source, int bestConfidence, EmbeddingTuple bestTarget) {
        MatchType matchType = bestConfidence == 100 ? MatchType.EXACT : MatchType.SEMANTIC;
        DataLinkEntity dataLink = new DataLinkEntity(matchType, Severity.LOW, source.parsedData(),
                bestTarget.parsedData());
        dataLink.setConfidenceScore(bestConfidence);
        dataLink.setScore(bestConfidence);
        if (matchType == MatchType.EXACT) {
            dataLink.setJustification(String.format("Correspondance exacte entre la valeur %s du fichier %s et la valeur %s du fichier %s.",
                    source.parsedData().getContent(), source.parsedData().getFile().getName(), bestTarget.parsedData().getContent(), bestTarget.parsedData().getFile().getName()));
        } else {
            dataLink.setJustification(String.format("Similarité sémantique de %d%% entre la valeur %s du fichier %s et la valeur %s du fichier %s.",
                    bestConfidence, source.parsedData().getContent(), source.parsedData().getFile().getName(), bestTarget.parsedData().getContent(), bestTarget.parsedData().getFile().getName()));
        }
        return dataLink;
    }

    public List<DataLinkEntity> saveDataLinks(VersionEntity version) {
        return dataLinkRepository.saveAll(buildDataLinks(version));
    }

    private boolean isComparable(EmbeddingTuple source, EmbeddingTuple candidate) {
        ParsedDataEntity sourceData = source.parsedData();
        ParsedDataEntity candidateData = candidate.parsedData();

        if (sourceData == candidateData || sourceData.getFile() == candidateData.getFile()) {
            return false;
        }

        DataProfile sourceProfile = DataProfile.from(sourceData);
        DataProfile candidateProfile = DataProfile.from(candidateData);

        if (sourceProfile == null || candidateProfile == null) {
            return false;
        }

        return COMPATIBILITY_MATRIX
                .getOrDefault(sourceProfile, Set.of())
                .contains(candidateProfile);
    }

    private Map<DataProfile, List<EmbeddingTuple>> indexByProfile(List<EmbeddingTuple> embeddings) {
        Map<DataProfile, List<EmbeddingTuple>> embeddingsByProfile = new HashMap<>();

        for (EmbeddingTuple embedding : embeddings) {
            DataProfile profile = DataProfile.from(embedding.parsedData());
            if (profile == null) {
                continue;
            }

            embeddingsByProfile.computeIfAbsent(profile, ignored -> new ArrayList<>()).add(embedding);
        }

        return embeddingsByProfile;
    }

    private List<EmbeddingTuple> getComparableCandidates(EmbeddingTuple source,
                                                         Map<DataProfile, List<EmbeddingTuple>> embeddingsByProfile) {
        DataProfile sourceProfile = DataProfile.from(source.parsedData());
        if (sourceProfile == null) {
            return List.of();
        }

        Set<DataProfile> compatibleProfiles = COMPATIBILITY_MATRIX.getOrDefault(sourceProfile, Set.of());
        if (compatibleProfiles.isEmpty()) {
            return List.of();
        }

        List<EmbeddingTuple> candidates = new ArrayList<>();
        for (DataProfile compatibleProfile : compatibleProfiles) {
            candidates.addAll(embeddingsByProfile.getOrDefault(compatibleProfile, List.of()));
        }

        return candidates;
    }

    private int getConfidenceScore(EmbeddingTuple source, EmbeddingTuple target) {
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

    private record DataProfile(FileType fileType, DataType dataType) {
        private static DataProfile from(ParsedDataEntity parsedData) {
            if (parsedData == null || parsedData.getFile() == null || parsedData.getFile().getType() == null
                    || parsedData.getDataType() == null) {
                return null;
            }

            return new DataProfile(parsedData.getFile().getType(), parsedData.getDataType());
        }
    }
}
