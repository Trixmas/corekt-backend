package lab.corekt.backend.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {

    private JsonUtils() {
    }

    public static String readJson(String json, String element) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonObject = mapper.readTree(json);
        return jsonObject.get(element).asText();
    }

}
