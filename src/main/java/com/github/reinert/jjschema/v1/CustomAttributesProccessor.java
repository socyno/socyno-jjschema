package com.github.reinert.jjschema.v1;

import java.lang.reflect.Method;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.reinert.jjschema.Attributes;

public class CustomAttributesProccessor {
    
    public static Method[] getDynamicMethods(Class<?> clazz) {
        return null;
    }
    
    public static void processCommonAttributes(ObjectNode node, Attributes attributes, Class<?> clazz, String field, Method method) {
        
    }
}
