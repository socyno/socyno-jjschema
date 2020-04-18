package com.github.reinert.jjschema.v1;

import lombok.Data;

@Data
public class FieldSimpleOption implements FieldOption {
    private String optionValue;
    private String optionGroup;
    private String optionDisplay;
    
    public FieldSimpleOption(String value) {
        this(value, value, null);
    }
    
    public FieldSimpleOption(String value, String display) {
        this(value, display, null);
    }
    
    public FieldSimpleOption(String value, String display, String group) {
        optionValue = value;
        optionGroup = group;
        optionDisplay = display;
    }
    
    public static FieldSimpleOption create(String value) {
        return new FieldSimpleOption(value);
    }
    
    public static FieldSimpleOption create(String value, String display) {
        return new FieldSimpleOption(value, display);
    }
    
    public static FieldSimpleOption create(String value, String display, String group) {
        return new FieldSimpleOption(value, display, group);
    }
}
