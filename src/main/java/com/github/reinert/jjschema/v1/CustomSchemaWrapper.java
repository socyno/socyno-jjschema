/*
 * Copyright (c) 2014, Danilo Reinert (daniloreinert@growbit.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of both licenses is available under the src/resources/ directory of
 * this project (under the names LGPL-3.0.txt and ASL-2.0.txt respectively).
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.reinert.jjschema.v1;

import static com.github.reinert.jjschema.JJSchemaUtil.processCommonAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.JJSchemaUtil;
import com.github.reinert.jjschema.ManagedReference;

/**
 * @author Danilo Reinert
 */

public class CustomSchemaWrapper extends SchemaWrapper implements Iterable<PropertyWrapper> {

    public static final String TAG_REQUIRED = "required";
    public static final String TAG_PROPERTIES = "properties";

    private final List<PropertyWrapper> propertyWrappers;
    private boolean required;
    private final Set<ManagedReference> managedReferences;
    private String relativeId = "#";

    public CustomSchemaWrapper(Type type) {
        this(type, false);
    }

    public CustomSchemaWrapper(Type type, boolean ignoreProperties) {
        this(type, new HashSet<ManagedReference>(), null, ignoreProperties);
    }

    public CustomSchemaWrapper(Type type, Set<ManagedReference> managedReferences, boolean ignoreProperties) {
        this(type, managedReferences, null, ignoreProperties);
    }

    public CustomSchemaWrapper(Type type, Set<ManagedReference> managedReferences, String relativeId, boolean ignoreProperties) {
        super(type);
        setType("object");
        setClassPath();
        processNullable();
        processAttributes(getNode());
        this.managedReferences = managedReferences;

        if (relativeId != null) {
            addTokenToRelativeId(relativeId);
        }

        if (ignoreProperties) {
            this.propertyWrappers = null;
            return;
        }

        this.propertyWrappers = newArrayListWithExpectedSize(getJavaType().getDeclaredFields().length);
        processProperties();
    }

    public String getRelativeId() {
        return relativeId;
    }

    protected void addTokenToRelativeId(String token) {
        if (token.startsWith("#"))
            relativeId = token;
        else
            relativeId = relativeId + "/" + token;
    }
    
    public void addProperty(PropertyWrapper propertyWrapper) {
        this.propertyWrappers.add(propertyWrapper);

        if (!getNode().has(TAG_PROPERTIES))
            getNode().putObject(TAG_PROPERTIES);

        ((ObjectNode) getNode().get(TAG_PROPERTIES)).set(propertyWrapper.getName(), propertyWrapper.asJson());

        if (propertyWrapper.isRequired())
            addRequired(propertyWrapper.getName());
    }

    public boolean isRequired() {
        return required;
    }

    public void addRequired(String name) {
        if (!getNode().has(TAG_REQUIRED))
            getNode().putArray(TAG_REQUIRED);
        ArrayNode requiredNode = (ArrayNode) getNode().get(TAG_REQUIRED);
        requiredNode.add(name);
    }

    public boolean pullReference(ManagedReference managedReference) {
        if (managedReferences.contains(managedReference))
            return false;
        managedReferences.add(managedReference);
        return true;
    }

    public boolean pushReference(ManagedReference managedReference) {
        return managedReferences.remove(managedReference);
    }

    @Override
    public boolean isCustomWrapper() {
        return true;
    }

    /**
     * Returns an iterator over a set of elements of PropertyWrapper.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<PropertyWrapper> iterator() {
        return propertyWrappers != null ? propertyWrappers.iterator() : Collections.<PropertyWrapper>emptyIterator();
    }

    protected void processProperties() {
        HashMap<Method, Field> properties = findProperties();
        for (Entry<Method, Field> prop : properties.entrySet()) {
            PropertyWrapper propertyWrapper = new PropertyWrapper(this, managedReferences, 
                    prop.getKey(), prop.getValue());
            if (!propertyWrapper.isEmptyWrapper())
                addProperty(propertyWrapper);
        }
    }

    private HashMap<Method, Field> findProperties() {
        Field[] fields = new Field[0];
        Class<?> javaType = getJavaType();
        while(javaType.getSuperclass() != null) {
            fields = concatFieldArrays(fields, javaType.getDeclaredFields());
            javaType = javaType.getSuperclass();
        }

        Method[] methods = getJavaType().getMethods();
        Class<? extends CustomAttributesProccessor> parser;
        if ((parser = JJSchemaUtil.getCustomAttributesParser()) != null) {
            try {
                Method[] dynamics = (Method[]) parser.getMethod("getDynamicMthods", Class.class)
                                    .invoke(null, getJavaType());
                if (dynamics != null && dynamics.length > 0) {
                    if (methods.length <= 0) {
                        methods = dynamics;
                    } else {
                        Method[] newMethods = new Method[methods.length + dynamics.length];
                        for (int idx = 0; idx < methods.length; idx++) {
                            newMethods[idx] = methods[idx];
                        }
                        for (int idx = 0; idx < dynamics.length; idx++) {
                            newMethods[methods.length + idx] = dynamics[idx];
                        }
                        methods = newMethods;
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                throw new RuntimeException(e);
            }
        }
        // Ordering the properties
        Arrays.sort(methods, new Comparator<Method>() {
            @Override
            public int compare(Method m1, Method m2) {
                return m1.getName().compareTo(m2.getName());
            }
        });

        LinkedHashMap<Method, Field> props = new LinkedHashMap<Method, Field>();
        // get valid properties (get method and respective field (if exists))
        for (Method method : methods) {
            Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass.equals(Object.class)
                    || Collection.class.isAssignableFrom(declaringClass)) {
                continue;
            }
            String methodField;
            if ((methodField = getNameFromGetter(method)) != null) {
//                boolean hasField = false;
                for (Field field : fields) {
                    if (field.getName().equalsIgnoreCase(methodField)) {
                        props.put(method, field);
//                        hasField = true;
                        break;
                    }
                }
//                if (!hasField) {
//                    props.put(method, null);
//                }
            }
        }
        return props;
    }
    
    /**
     * check get method (starts with get/is, non static, no parameters)
     */
    private String getNameFromGetter(final Method method) {
        String name = method.getName();
        if (method.getParameterTypes().length > 0
                && (method.getModifiers() & Modifier.STATIC) != 0) {
            return null;
        }
        String[] getterPrefixes = {"get", "is"};
        for (String prefix : getterPrefixes) {
            if (name.startsWith(prefix) && name.length() > prefix.length()) {
                String field = name.substring(prefix.length());
                return field.substring(0, 1).toLowerCase() + field.substring(1);
            }
        }
        return null;
    }

    private Field[] concatFieldArrays(Field[] first, Field[] second) {
        Field[] result = new Field[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private <E> ArrayList<E> newArrayListWithExpectedSize(int estimatedSize) {
        return new ArrayList<>(5 + estimatedSize + (estimatedSize / 10));
    }

    protected void setRequired(boolean required) {
        this.required = required;
    }

    protected void processAttributes(ObjectNode node) {
        final Attributes attributes = getJavaType().getAnnotation(Attributes.class);
        processCommonAttributes(node, attributes, getJavaType(), null);
        if (attributes != null) {
            if (attributes.required()) {
                setRequired(true);
            }
            if (!attributes.additionalProperties()) {
                node.put("additionalProperties", false);
            }
        }
    }
}
