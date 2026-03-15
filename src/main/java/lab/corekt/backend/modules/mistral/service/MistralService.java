package lab.corekt.backend.modules.mistral.service;

import lab.corekt.backend.modules.project.parsing.entity.ParsedDataEntity;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MistralService {

    private final EmbeddingModel embeddingModel;

    public MistralService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<EmbeddingTuple> embedParsedData(List<ParsedDataEntity> parsedData) {
        List<EmbeddingTuple> embeddings = new ArrayList<>();
        List<float[]> embeddingResponse = embeddingModel.embed(parsedData.stream().map(ParsedDataEntity::getContent).toList());
        embeddingResponse.forEach(embedding -> {
            int index = embeddingResponse.indexOf(embedding);
            embeddings.add(new EmbeddingTuple(parsedData.get(index), embedding));
        });
        return embeddings;
    }

    public record EmbeddingTuple(ParsedDataEntity parsedData, float[] embedding) {
    }
}
