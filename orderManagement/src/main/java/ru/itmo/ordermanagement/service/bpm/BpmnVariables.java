package ru.itmo.ordermanagement.service.bpm;

import org.camunda.bpm.engine.delegate.DelegateExecution;

final class BpmnVariables {

    private BpmnVariables() {
    }

    static Long requiredLong(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value == null) {
            throw new IllegalArgumentException("Required BPMN variable is missing: " + name);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    static Boolean requiredBoolean(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value == null) {
            throw new IllegalArgumentException("Required BPMN variable is missing: " + name);
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.valueOf(value.toString());
    }

    static String optionalString(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }
}
