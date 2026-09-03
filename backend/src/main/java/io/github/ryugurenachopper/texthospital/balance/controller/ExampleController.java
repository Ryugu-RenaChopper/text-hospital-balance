package io.github.ryugurenachopper.texthospital.balance.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@RestController
@RequestMapping("/api/examples")
public class ExampleController {
    private static final Set<String> EXAMPLE_NAMES = Set.of("balanced", "imbalanced");
    private final ObjectMapper objectMapper;

    public ExampleController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{name}")
    public JsonNode example(@PathVariable String name) throws IOException {
        if (!EXAMPLE_NAMES.contains(name)) {
            throw new IllegalArgumentException("Unknown example: " + name);
        }
        ClassPathResource resource = new ClassPathResource("examples/synthetic-" + name + ".json");
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readTree(input);
        }
    }
}
