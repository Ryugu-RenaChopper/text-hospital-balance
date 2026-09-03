package io.github.ryugurenachopper.texthospital.balance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DatasetStatistics {
    public List<ObjectNode> samples(JsonNode dataset) {
        List<ObjectNode> samples = new ArrayList<>();
        if (dataset == null || dataset.isNull()) {
            throw new IllegalArgumentException("Dataset must not be null");
        }
        if (dataset.isArray()) {
            dataset.forEach(node -> {
                if (!node.isObject()) {
                    throw new IllegalArgumentException("Every dataset array element must be an object");
                }
                samples.add((ObjectNode) node);
            });
        } else if (dataset.isObject()) {
            samples.add((ObjectNode) dataset);
        } else {
            throw new IllegalArgumentException("Dataset root must be an object or an array");
        }
        if (samples.isEmpty()) {
            throw new IllegalArgumentException("Dataset must contain at least one sample");
        }
        return samples;
    }

    public Map<String, Integer> countLabels(List<ObjectNode> samples, String kind) {
        String field = "ENTITY".equals(kind) ? "ner" : "relations";
        int labelIndex = "ENTITY".equals(kind) ? 3 : 4;
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ObjectNode sample : samples) {
            JsonNode tuples = sample.get(field);
            if (tuples == null || tuples.isNull()) {
                continue;
            }
            if (!tuples.isArray()) {
                throw new IllegalArgumentException(field + " must be an array when present");
            }
            for (JsonNode tuple : tuples) {
                if (!tuple.isArray() || tuple.size() <= labelIndex || !tuple.get(labelIndex).isTextual()
                        || tuple.get(labelIndex).asText().isBlank()) {
                    throw new IllegalArgumentException("Malformed " + field + " tuple: label must be a non-blank string");
                }
                counts.merge(tuple.get(labelIndex).asText(), 1, Integer::sum);
            }
        }
        return counts;
    }

    public int occurrences(ObjectNode sample, String kind, String targetType) {
        String field = "ENTITY".equals(kind) ? "ner" : "relations";
        int labelIndex = "ENTITY".equals(kind) ? 3 : 4;
        JsonNode tuples = sample.get(field);
        if (tuples == null || !tuples.isArray()) {
            return 0;
        }
        int count = 0;
        for (JsonNode tuple : tuples) {
            if (tuple.isArray() && tuple.size() > labelIndex && tuple.get(labelIndex).isTextual()
                    && targetType.equals(tuple.get(labelIndex).asText())) {
                count++;
            }
        }
        return count;
    }
}
