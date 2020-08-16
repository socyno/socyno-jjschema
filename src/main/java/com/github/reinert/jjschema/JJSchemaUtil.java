package com.github.reinert.jjschema;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.reinert.jjschema.v1.CustomAttributesProccessor;
import com.github.reinert.jjschema.v1.FieldOption;
import com.github.reinert.jjschema.v1.FieldType;
import com.github.reinert.jjschema.v1.FieldType.FieldOptionsType;

import lombok.Data;

public class JJSchemaUtil {
    
    private static final ThreadLocal<Class<? extends CustomAttributesProccessor>> 
                                        CUSTOM_ATTRIBUTES_PARSER = new ThreadLocal<>();
    
    private JJSchemaUtil() {
    }
    
    public static void setCustomAttributesParser(Class<? extends CustomAttributesProccessor> parser) {
        CUSTOM_ATTRIBUTES_PARSER.set(parser);
    }
    
    public static Class<? extends CustomAttributesProccessor> getCustomAttributesParser() {
        return CUSTOM_ATTRIBUTES_PARSER.get();
    }
    
    @Data
    public static class NamedTypeEntity {
        private String fieldType;
        private String fieldTypeKey;
        private FieldOptionsType optionsType = null;
        private List<? extends FieldOption> staticOptions;
    }
    
    public static NamedTypeEntity parseTypeAttributes(Attributes attributes) {
        if (attributes.type() != null) {
            try {
                Class<? extends FieldType> type = attributes.type();
                if (FieldType.class.equals(type) || (type.getModifiers() & Modifier.ABSTRACT) != 0) {
                    return null;
                }
                FieldType instance = null;
                try {
                    instance = (FieldType) type.getMethod("getInstance").invoke(null);
                } catch(Exception ex) {
                    instance = type.getDeclaredConstructor().newInstance();
                }
                if (instance == null || !type.isAssignableFrom(instance.getClass())) {
                    throw new RuntimeException(String.format(
                            "The returned value of getInstance is not invalid class type(%s or it's subclass required) .",
                            type.getName()));
                }
                NamedTypeEntity entity = new NamedTypeEntity();
                entity.setFieldTypeKey(type.getName());
                entity.setFieldType(instance.getTypeName());
                entity.setOptionsType(instance.getOptionsType());
                if (instance.getOptionsType() != null && FieldOptionsType.STATIC.equals(entity.getOptionsType())) {
                    entity.setStaticOptions(instance.getStaticOptions());
                }
                return entity;
            } catch (RuntimeException ex) {
                throw (RuntimeException) ex;
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return null;
    }
    
    public static void processCommonAttributes(ObjectNode node, Attributes attributes, Class<?> clazz, String field, Method method) {
        if (attributes != null) {
            if (!attributes.id().isEmpty()) {
                node.put("id", attributes.id());
            }
            if (!attributes.description().isEmpty()) {
                node.put("description", attributes.description());
            }
            if (!attributes.pattern().isEmpty()) {
                node.put("pattern", attributes.pattern());
            }
            if (!attributes.format().isEmpty()) {
                node.put("format", attributes.format());
            }
            if (!attributes.title().isEmpty()) {
                node.put("title", attributes.title());
            }
            if (attributes.maximum() != Long.MIN_VALUE) {
                node.put("maximum", attributes.maximum());
            }
            if (attributes.exclusiveMaximum()) {
                node.put("exclusiveMaximum", true);
            }
            if (attributes.minimum() != Integer.MAX_VALUE) {
                node.put("minimum", attributes.minimum());
            }
            if (attributes.exclusiveMinimum()) {
                node.put("exclusiveMinimum", true);
            }
            if (attributes.position() != 0) {
                node.put("position", attributes.position());
            }
            if (attributes.uniqueItems()) {
                node.put("uniqueItems", true);
            }
            if (attributes.minItems() > 0) {
                node.put("minItems", attributes.minItems());
            }
            if (attributes.maxItems() > -1) {
                node.put("maxItems", attributes.maxItems());
            }
            if (attributes.multipleOf() > 0) {
                node.put("multipleOf", attributes.multipleOf());
            }
            if (attributes.minLength() > 0) {
                node.put("minLength", attributes.minLength());
            }
            if (attributes.maxLength() > -1) {
                node.put("maxLength", attributes.maxLength());
            }
            if (attributes.readonly()) {
                node.put("readonly", true);
            }
            if (attributes.required() && method != null) {
                node.put("required", true);
            }
            if (attributes.enums().length > 0) {
                ArrayNode enumArray = node.putArray("enum");
                String[] enums = attributes.enums();
                for (String v : enums) {
                    if (v.equals("null")) {
                        enumArray.addNull();
                    } else {
                        enumArray.add(v);
                    }
                }
            }
            NamedTypeEntity namedTypeEntity;
            if ((namedTypeEntity = parseTypeAttributes(attributes)) != null) {
                String typeName = namedTypeEntity.getFieldType();
                if (typeName != null && !typeName.trim().isEmpty()) {
                    node.put("fieldType", typeName);
                    node.put("fieldTypeKey", namedTypeEntity.getFieldTypeKey());
                    node.put("fieldOptionsType", FieldOptionsType.NULL.name());
                    if (namedTypeEntity.getOptionsType() != null) {
                        node.put("fieldOptionsType", namedTypeEntity.getOptionsType().name());
                    }
                    
                    List<? extends FieldOption> options;
                    if (FieldOptionsType.STATIC.equals(namedTypeEntity.getOptionsType())
                            && (options = namedTypeEntity.getStaticOptions()) != null
                            && !options.isEmpty()) {
                        ArrayNode enumArray = node.putArray("staticOptions");
                        for (FieldOption option : options) {
                            ObjectNode jsonOption = node.objectNode()
                                    .put("group", option.getOptionGroup())
                                    .put("value", option.getOptionValue())
                                    .put("display", option.getOptionDisplay())
                                    .put("optionGroup", option.getOptionGroup())
                                    .put("optionValue", option.getOptionValue())
                                    .put("optionDisplay", option.getOptionDisplay());
                            enumArray.add(jsonOption);
                        }
                    }
                }
            }
        }
        Class<? extends CustomAttributesProccessor> parser;
        if ((parser = getCustomAttributesParser()) != null) {
            try {
                parser.getMethod("processCommonAttributes", ObjectNode.class, Attributes.class, 
                        Class.class, String.class, Method.class).invoke(null, node, attributes, clazz, field, method);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
