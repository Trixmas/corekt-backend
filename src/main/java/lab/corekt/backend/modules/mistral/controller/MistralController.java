package lab.corekt.backend.modules.mistral.controller;

import lab.corekt.backend.modules.mistral.MistralService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mistral")
class MistralController {

    private final MistralService mistralService;

    public MistralController(MistralService mistralService) {
        this.mistralService = mistralService;
    }


    @PostMapping("/test")
    public Map embed(@RequestBody List<String> parsedData) {
        return mistralService.embedParsedData(parsedData);
    }

}
