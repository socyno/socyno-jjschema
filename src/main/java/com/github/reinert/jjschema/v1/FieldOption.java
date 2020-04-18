package com.github.reinert.jjschema.v1;

public interface FieldOption {
    
    public void setOptionValue(String value);
    
    public String getOptionValue();
    
    public String getOptionDisplay();
    
    public default String getOptionGroup() {
        return null;
    }
}
