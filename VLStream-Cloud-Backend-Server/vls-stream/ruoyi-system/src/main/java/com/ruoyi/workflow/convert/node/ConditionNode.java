package com.ruoyi.workflow.convert.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.workflow.convert.condition.FilterRules;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;

import java.util.*;

/**
 * @description：条件(分支)
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ConditionNode extends Node {
    private Boolean def;
    private FilterRules property;
    private String expression;
    @JsonIgnore
    private Map<String, String> operatorMap = new HashMap<>();

    {
        // 等于
        operatorMap.put("eq", "var:eq(%s, %s)");
        // 不等于
        operatorMap.put("ne", "var:notEquals(%s, %s)");
        // 包含
        operatorMap.put("in", "var:containsAny(%s, %s)");
        // 不包含
        operatorMap.put("ni", "var:notContainsAny(%s, %s)");
        // 为空
        operatorMap.put("ul", "var:isNull(%s)");
        // 不为空
        operatorMap.put("nu", "var:isNotNull(%s)");
        // 字符包含
        operatorMap.put("lk", "var:contains(%s, %s)");
        // 大于
        operatorMap.put("gt", "var:gt(%s, %s)");
        // 小于
        operatorMap.put("lt", "var:lt(%s, %s)");
        // 小于或等于
        operatorMap.put("lte", "var:lte(%s, %s)");
        // 大于或等于
        operatorMap.put("gte", "var:gte(%s, %s)");
    }

//    protected String stringVal(Object val) {
//        if (val instanceof String) {
//            return String.format("'%s'", val);
//        } else {
//            return String.valueOf(val);
//        }
//    }
//
//    public String toConditionExpression(FilterRules filterRules) {
//        String expression = filterRules.getConditions().stream().map(e -> {
//            String operator = operatorMap.get(e.getOperator());
//            if (StringUtils.isNotBlank(operator)) {
//                if (e.getValue() instanceof Collection) {
//                    e.setValue(
//                        ((Collection<?>) e.getValue())
//                            .stream()
//                            .map(this::stringVal)
//                            .collect(Collectors.joining(","))
//                    );
//                } else if (e.getValue() instanceof Object[]) {
//                    e.setValue(
//                        Arrays.stream((Object[]) e.getValue())
//                            .map(this::stringVal)
//                            .collect(Collectors.joining(","))
//                    );
//                } else if (e.getValue() instanceof String) {
//                    e.setValue(String.format("'%s'", e.getValue()));
//                }
//                return String.format(operator,
//                    e.getField(),
//                    e.getValue()
//                );
//            } else {
//                return "";
//            }
//        }).collect(Collectors.joining("and".equals(filterRules.getOperator()) ? " && " : " ||"));
//        if (CollectionUtils.isEmpty(filterRules.getGroups())) {
//            return expression;
//        } else {
//            String collect = filterRules
//                .getGroups()
//                .stream()
//                .map(this::toConditionExpression)
//                .collect(Collectors.joining("and".equals(filterRules.getOperator()) ? " && " : " || "));
//            if (StringUtils.isNotBlank(expression)) {
//                return String.format("(%s) %s (%s)", expression, "and".equals(filterRules.getOperator()) ? " && " : " || ", collect);
//            } else {
//                return collect;
//            }
//        }
//    }

    // 将JSON转换为表达式的方法
    public static String jsonToExpression(JsonNode jsonNode) {
        if (jsonNode == null || jsonNode.isNull()) {
            return "";
        }

        StringBuilder expression = new StringBuilder();
        JsonNode ruleNode = jsonNode.get("rule");
        if (ruleNode != null) {
            Iterator<String> ruleKeys = ruleNode.fieldNames();
            while (ruleKeys.hasNext()) {
                String key = ruleKeys.next();
                JsonNode subRule = ruleNode.get(key);

                if (key.startsWith("r") && subRule.isObject()) {
                    if (expression.length() > 0) {
                        expression.append(" ").append(jsonNode.get("rule_flag").asText()).append(" ");
                    }
                    expression.append(buildSubExpression(subRule));
                }
            }
        }

        return expression.toString().trim();
    }

    private static String buildSubExpression(JsonNode subRule) {
        StringBuilder subExpression = new StringBuilder("(");
        Iterator<String> fieldNames = subRule.fieldNames();
        String ruleFlag = "";

        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (fieldName.equals("rule_flag")) {
                ruleFlag = subRule.get(fieldName).asText();
            } else {
                subExpression.append(subRule.get(fieldName).asText()).append(" ").append(ruleFlag).append(" ");
            }
        }

        // Remove the trailing operator and close the parenthesis
        if (subExpression.length() > 1 && ruleFlag.length() > 0) {
            subExpression.setLength(subExpression.length() - ruleFlag.length() - 2);
        }
        subExpression.append(")");

        return subExpression.toString();
    }

    // 将表达式转换为JSON的方法
    public static JsonNode expressionToJson(String expression) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        ObjectNode ruleNode = mapper.createObjectNode();

        String[] subExpressions = splitExpression(expression);
        for (int i = 0; i < subExpressions.length; i++) {
            String subExpression = subExpressions[i];
            ObjectNode subRuleNode = buildRuleFromExpression(subExpression, mapper);
            ruleNode.set("r" + (i + 1), subRuleNode);

            if (i < subExpressions.length - 1) {
                ruleNode.put("rule_r" + (i + 1) + (i + 2) + "_flag", cleanConnector(extractConnector(expression, subExpressions[i], subExpressions[i + 1])));
            }
        }

        rootNode.set("rule", ruleNode);
        rootNode.put("rule_flag", "&&"); // 顶层规则标志，根据需要调整

        return rootNode;
    }

    private static String[] splitExpression(String expression) {
        return expression.split("\\)\\s*(\\|\\||&&)\\s*\\(");
    }

    private static ObjectNode buildRuleFromExpression(String subExpression, ObjectMapper mapper) {
        ObjectNode subRuleNode = mapper.createObjectNode();
        String[] conditions = subExpression.replace("(", "").replace(")", "").split("\\s*(&&|\\|\\|)\\s*");
        String[] connectors = subExpression.split("[^&|]+");

        for (int i = 0; i < conditions.length; i++) {
            subRuleNode.put(Character.toString((char) ('a' + i)), conditions[i].trim());
            if (i < connectors.length - 1) {
                subRuleNode.put("rule_" + Character.toString((char) ('a' + i)) + Character.toString((char) ('a' + i + 1)) + "_flag", cleanConnector(connectors[i + 1].trim()));
            }
        }

        return subRuleNode;
    }

    private static String extractConnector(String expression, String current, String next) {
        int start = expression.indexOf(current) + current.length();
        int end = expression.indexOf(next);
        return expression.substring(start, end).trim();
    }

    private static String cleanConnector(String connector) {
        return connector.replace(")", "").replace("(", "").trim();
    }

    @Override
    public List<FlowElement> convert() {
        ArrayList<FlowElement> elements = new ArrayList<>();
        // 条件节点
        SequenceFlow sequenceFlow = this.buildSequence(this);
        sequenceFlow.setId(this.getId());
        sequenceFlow.setName(this.getNodeName());
        sequenceFlow.setTargetRef(
            Optional.ofNullable(this.getChildNode()).map(Node::getId).orElse(this.getBranchId())
        );
//        String expression = this.toConditionExpression(this.getProperty());
        if (StringUtils.isNotBlank(expression) && def != true) {
            ExtensionElement extensionElement = new ExtensionElement();
            extensionElement.setName("expression");
            // 设置命名空间 URI 和前缀
            extensionElement.setNamespace("http://oortcloud.com/flowable/extensions");
            extensionElement.setNamespacePrefix("ext");
            // 设置元素的文本内容
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode JsonNode = objectMapper.readTree(expression);
                String expressionValue = jsonToExpression(JsonNode);
                extensionElement.setElementText(String.format("%s", expressionValue));
                sequenceFlow.setConditionExpression(String.format("${%s}", sequenceFlow.getId()+"expression"));
                sequenceFlow.addExtensionElement(extensionElement);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        elements.add(sequenceFlow);
        // 下一个节点
        Node child = this.getChildNode();
        if (Objects.nonNull(child)) {
            child.setBranchId(this.getBranchId());
            List<FlowElement> flowElements = child.convert();
            elements.addAll(flowElements);
        }
        return elements;
    }
}
