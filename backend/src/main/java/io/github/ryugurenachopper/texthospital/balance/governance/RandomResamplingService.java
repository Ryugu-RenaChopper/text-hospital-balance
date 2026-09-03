package io.github.ryugurenachopper.texthospital.balance.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.ryugurenachopper.texthospital.balance.dto.AnalysisResponse;
import io.github.ryugurenachopper.texthospital.balance.dto.GovernanceRequest;
import io.github.ryugurenachopper.texthospital.balance.dto.GovernanceResponse;
import io.github.ryugurenachopper.texthospital.balance.service.BalanceAnalysisService;
import io.github.ryugurenachopper.texthospital.balance.service.DatasetStatistics;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Service
public class RandomResamplingService {
    private static final long MAX_JAVASCRIPT_SAFE_INTEGER = 9_007_199_254_740_991L;
    private static final String WARNING = "Random resampling only duplicates existing samples. "
            + "It does not create new evidence and does not guarantee that every metric improves.";

    private final DatasetStatistics statistics;
    private final BalanceAnalysisService analysisService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public RandomResamplingService(
            DatasetStatistics statistics,
            BalanceAnalysisService analysisService,
            ObjectMapper objectMapper
    ) {
        this.statistics = statistics;
        this.analysisService = analysisService;
        this.objectMapper = objectMapper;
    }

    public GovernanceResponse govern(GovernanceRequest request) {
        validate(request);
        String kind = request.targetKind().trim().toUpperCase(Locale.ROOT);
        List<ObjectNode> originals = statistics.samples(request.dataset());
        Map<String, Integer> beforeCounts = statistics.countLabels(originals, kind);
        int currentCount = beforeCounts.getOrDefault(request.targetType(), 0);
        if (currentCount == 0) {
            throw new RandomResamplingException("target_type_not_found",
                    "Target type does not exist in the selected distribution");
        }

        long seed = request.randomSeed() == null
                ? secureRandom.nextLong(MAX_JAVASCRIPT_SAFE_INTEGER + 1)
                : request.randomSeed();
        String operationId = operationId(request.dataset(), kind, request.targetType(), request.targetCount(), seed);
        ArrayNode output = objectMapper.createArrayNode();
        originals.forEach(sample -> output.add(sample.deepCopy()));
        int actualCount = currentCount;
        int copiedSampleCount = 0;

        if (request.targetCount() > currentCount) {
            List<ObjectNode> candidates = originals.stream()
                    .filter(sample -> statistics.occurrences(sample, kind, request.targetType()) > 0)
                    .toList();
            Set<String> usedIds = collectIds(originals);
            Random random = new Random(seed);
            while (actualCount < request.targetCount()) {
                ObjectNode selected = candidates.get(random.nextInt(candidates.size()));
                ObjectNode copy = selected.deepCopy();
                copiedSampleCount++;
                copy.put("id", uniqueId(selected, operationId, copiedSampleCount, usedIds));
                output.add(copy);
                actualCount += statistics.occurrences(selected, kind, request.targetType());
            }
        }

        AnalysisResponse before = analysisService.analyze(request.dataset(), request.imbalanceRatioThreshold(),
                request.giniThreshold(), request.thresholdMode());
        AnalysisResponse after = analysisService.analyze(output, request.imbalanceRatioThreshold(),
                request.giniThreshold(), request.thresholdMode());
        boolean noChange = copiedSampleCount == 0;
        return new GovernanceResponse(
                noChange ? "NO_CHANGE_NEEDED" : "COMPLETED",
                kind,
                request.targetType(),
                request.targetCount(),
                currentCount,
                actualCount,
                Math.max(0, actualCount - request.targetCount()),
                copiedSampleCount,
                seed,
                before,
                after,
                output,
                WARNING
        );
    }

    private void validate(GovernanceRequest request) {
        if (request == null || request.dataset() == null || request.dataset().isNull()) {
            throw new RandomResamplingException("invalid_dataset", "Dataset must not be null");
        }
        String kind = request.targetKind() == null ? "" : request.targetKind().trim().toUpperCase(Locale.ROOT);
        if (!"ENTITY".equals(kind) && !"RELATION".equals(kind)) {
            throw new RandomResamplingException("invalid_target_kind", "targetKind must be ENTITY or RELATION");
        }
        if (request.targetType() == null || request.targetType().isBlank()) {
            throw new RandomResamplingException("invalid_target_type", "targetType must not be blank");
        }
        if (request.targetCount() == null || request.targetCount() <= 0) {
            throw new RandomResamplingException("invalid_target_count", "targetCount must be a positive integer");
        }
        if (request.randomSeed() != null
                && (request.randomSeed() < 0 || request.randomSeed() > MAX_JAVASCRIPT_SAFE_INTEGER)) {
            throw new RandomResamplingException("invalid_random_seed",
                    "randomSeed must be within JavaScript's non-negative safe-integer range");
        }
    }

    private Set<String> collectIds(List<ObjectNode> samples) {
        Set<String> usedIds = new HashSet<>();
        samples.stream()
                .filter(sample -> sample.hasNonNull("id") && !sample.get("id").asText().isBlank())
                .map(sample -> sample.get("id").asText())
                .forEach(usedIds::add);
        return usedIds;
    }

    private String uniqueId(ObjectNode selected, String operationId, int copyNumber, Set<String> usedIds) {
        String originalId = selected.hasNonNull("id") && !selected.get("id").asText().isBlank()
                ? selected.get("id").asText() : null;
        String prefix = originalId == null ? "rs_" : originalId + "__rs_";
        String candidate = prefix + operationId + "_" + copyNumber;
        int collision = 1;
        while (!usedIds.add(candidate)) {
            candidate = prefix + operationId + "_" + copyNumber + "_" + collision++;
        }
        return candidate;
    }

    private String operationId(JsonNode dataset, String kind, String targetType, int targetCount, long seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(dataset.toString().getBytes(StandardCharsets.UTF_8));
            digest.update((kind + "|" + targetType + "|" + targetCount + "|" + seed)
                    .getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest();
            StringBuilder value = new StringBuilder();
            for (int index = 0; index < 6; index++) {
                value.append(String.format("%02x", hash[index]));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
