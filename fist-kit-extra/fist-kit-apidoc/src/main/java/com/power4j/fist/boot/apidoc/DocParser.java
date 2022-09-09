package com.power4j.fist.boot.apidoc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.power4j.coca.kit.common.text.StringPool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/8/26
 * @since 1.0
 */
@Slf4j
public class DocParser {

	private final static String SWAGGER_EXTENSION_KEY = "extensions";

	private final static String KEY_PATHS = "paths";

	private final static String KEY_OPERATION_ID = "operationId";

	private final static String KEY_TAGS = "tags";

	private final static String KEY_SUMMARY = "summary";

	private final static String KEY_LEVEL = "level";

	private final static String KEY_RESOURCE_ID = "resourceId";

	private final static String KEY_CODE = "code";

	private final static String KEY_EXPOSE = "expose";

	private final static String KEY_SIGN = "sign";

	private final ObjectMapper objectMapper;

	public DocParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public DocParser() {
		this(new ObjectMapper());
	}

	private List<ApiModel> parseFromJson(@Nullable String json) throws JsonProcessingException {
		if (Objects.isNull(json) || json.isEmpty()) {
			return Collections.emptyList();
		}
		final JsonNode root = objectMapper.readTree(json);

		Iterator<Map.Entry<String, JsonNode>> pathIter = Optional.ofNullable(root.get(KEY_PATHS)).map(JsonNode::fields)
				.orElse(null);
		if (Objects.isNull(pathIter)) {
			return Collections.emptyList();
		}
		final List<ApiModel> permissionInfos = new ArrayList<>(16);
		pathIter.forEachRemaining(path -> {
			path.getValue().fields().forEachRemaining(kv -> {
				final ApiModel apiModel = new ApiModel();
				apiModel.setPath(path.getKey());
				apiModel.setMethod(kv.getKey().toLowerCase());
				Optional<JsonNode> methodInfo = Optional.ofNullable(kv.getValue());
				methodInfo.ifPresent(o -> {
					apiModel.setAction(Optional.ofNullable(o.get(KEY_OPERATION_ID)).map(JsonNode::asText)
							.orElse(StringPool.EMPTY));
					apiModel.setDocTags(Optional.ofNullable(o.get(KEY_TAGS)).map(this::readStrArray).orElse(null));
					apiModel.setDescription(
							Optional.ofNullable(o.get(KEY_SUMMARY)).map(JsonNode::asText).orElse(StringPool.EMPTY));
				});
				Optional<JsonNode> secureDetails = methodInfo.map(o -> o.get(DocConstant.SECURE_API_DETAILS_EXTENSION));
				if (secureDetails.isEmpty()) {
					secureDetails = methodInfo.map(o -> o.get(SWAGGER_EXTENSION_KEY))
							.map(o -> o.get(DocConstant.SECURE_API_DETAILS_EXTENSION));
				}
				if (secureDetails.isEmpty()) {
					log.info("忽略注册 {} {}", kv.getKey(), path.getKey());
				}
				else {
					apiModel.setLevel(secureDetails.map(o -> o.get(KEY_LEVEL)).map(JsonNode::asText).orElse(null));
					apiModel.setResourceId(
							secureDetails.map(o -> o.get(KEY_RESOURCE_ID)).map(JsonNode::asText).orElse(null));
					apiModel.setCode(
							secureDetails.map(o -> o.get(KEY_CODE)).map(JsonNode::asText).orElse(StringPool.EMPTY));
					apiModel.setExpose(secureDetails.map(o -> o.get(KEY_EXPOSE)).map(JsonNode::asText).orElse(null));
					apiModel.setSign(secureDetails.map(o -> o.get(KEY_SIGN)).map(JsonNode::asBoolean).orElse(false));
					permissionInfos.add(apiModel);
				}
			});
		});
		return permissionInfos;
	}

	protected List<JsonNode> readArray(JsonNode node) {
		if (!node.isArray()) {
			return Collections.emptyList();
		}
		List<JsonNode> list = new ArrayList<>(8);
		for (JsonNode c : node) {
			list.add(c);
		}
		return list;
	}

	protected List<String> readStrArray(JsonNode node) {
		return readArray(node).stream().map(JsonNode::asText).collect(Collectors.toList());
	}

}
