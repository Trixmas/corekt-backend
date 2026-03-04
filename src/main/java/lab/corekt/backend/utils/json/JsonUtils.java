package lab.corekt.backend.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class JsonUtils {

    private JsonUtils() {
    }

    public static String readJson(String json, String element) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonObject = mapper.readTree(json);
        return jsonObject.get(element).asText();
    }

    public static ArrayNode getJsonNodes(String body) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonObject = mapper.readTree(body);
        return (ArrayNode) jsonObject;

    }

}
