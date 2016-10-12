package com.moebiusgames.xdata;

import com.moebiusgames.xdata.annotation.MarshallableField;
import com.moebiusgames.xdata.annotation.MarshallableFinalParameter;
import com.moebiusgames.xdata.annotation.MarshallableType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/*
 * Copyright (C) 2016 Florian Frankenberger.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

/**
 * A generic marshaller that can be used instead of a special marshaller
 * for classes that are annotated with MarshallableType and MarshallableField
 * annotations
 *
 * @author Florian Frankenberger
 */
public class AnnotationBasedMarshaller<T> implements DataMarshaller<T> {

    private final Class<T> annotatedClass;
    private final MarshallableType annotation;

    private final Map<Field, Key<Object>> dataKeys = new HashMap<>();
    private final Set<Field> getterSet = new HashSet<>();
    private final Set<Field> setterSet = new HashSet<>();

    private final Map<String, Integer> fieldNameToConstructorParameterIndex = new HashMap<>();
    private Constructor<T> constructorToUse;

    public AnnotationBasedMarshaller(Class<T> annotatedClass) {
        if (annotatedClass == null) {
            throw new IllegalArgumentException("null can't be an annotated class");
        }
        this.annotatedClass = annotatedClass;
        checkAnnotations();
        this.annotation = this.annotatedClass.getAnnotation(MarshallableType.class);

        prepareFields();
    }

    private void checkAnnotations() {
        if (annotatedClass.getAnnotation(MarshallableType.class) == null) {
            throw new IllegalStateException("Class " + annotatedClass.getCanonicalName() + " has not MarshallableField annotation");
        }
    }

    private void prepareFields() {
        final Map<String, Field> finalFields = new HashMap<>();

        Class<? super T> clazz = this.annotatedClass;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                if (!Modifier.isStatic(field.getModifiers())) {
                    final MarshallableField fieldAnnotation = field.getAnnotation(MarshallableField.class);
                    if (fieldAnnotation != null) {
                        final Class<?> boxedType = box(field.getType());
                        if (List.class.isAssignableFrom(field.getType())) {
                            dataKeys.put(field, (Key<Object>) ListDataKey.create(fieldAnnotation.name(), boxedType));
                        } else {
                            dataKeys.put(field, (Key<Object>) DataKey.create(fieldAnnotation.name(), boxedType));
                        }

                        String uppercaseFieldName = getUppercaseStartingFielName(field);
                        try {
                            final Method method = annotatedClass.getMethod("get" + uppercaseFieldName);
                            if (method.getReturnType() == field.getType()) {
                                getterSet.add(field);
                            }
                        } catch (NoSuchMethodException ex) {
                            //ignore
                        } catch (SecurityException ex) {
                            //ignore
                        }

                        if (Modifier.isFinal(field.getModifiers())) {
                            finalFields.put(field.getName(), field);
                        } else {
                            try {
                                annotatedClass.getMethod("set" + uppercaseFieldName, field.getType());
                                setterSet.add(field);
                            } catch (NoSuchMethodException ex) {
                                //ignore
                            } catch (SecurityException ex) {
                                //ignore
                            }
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }

        //now determine the correct constructor
        constructorToUse = null;
        for (Constructor<?> constructor : this.annotatedClass.getDeclaredConstructors()) {
            Set<String> finalFieldsNames = new HashSet<>(finalFields.keySet());
            if (constructor.getParameterCount() == finalFields.size()) {
                for (int idx = 0; idx < constructor.getParameterCount(); ++idx) {
                    final Parameter parameter = constructor.getParameters()[idx];
                    final MarshallableFinalParameter finalParameter = parameter.getAnnotation(MarshallableFinalParameter.class);
                    if (finalParameter != null) {
                        final Field field = finalFields.get(finalParameter.name());
                        if (field != null && field.getType().isAssignableFrom(parameter.getType())) {
                            finalFieldsNames.remove(finalParameter.name());
                            fieldNameToConstructorParameterIndex.put(field.getName(), idx);
                        }
                    }
                }
                if (finalFieldsNames.isEmpty()) {
                    constructorToUse = (Constructor<T>) constructor;
                }
            }
        }

        if (constructorToUse == null) {
            throw new IllegalStateException("No construcor found that matches the final fields");
        }
    }

    private static String getUppercaseStartingFielName(Field field) {
        final String fieldName = field.getName();
        final String uppercaseFieldName = fieldName.length() >= 2
                ? (Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1))
                : String.valueOf(Character.toUpperCase(fieldName.charAt(0)));
        return uppercaseFieldName;
    }

    @Override
    public String getDataClassName() {
        return annotation.uniqueName();
    }

    @Override
    public Class<T> getDataClass() {
        return annotatedClass;
    }

    @Override
    public List<? extends AbstractDataMarshaller<?>> getRequiredMarshallers() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public DataNode marshal(T object) {
        DataNode node = new DataNode();
        for (Entry<Field, Key<Object>> entry : dataKeys.entrySet()) {
            final Field field = entry.getKey();
            final Key<Object> key = entry.getValue();

            try {
                final Object value;
                if (getterSet.contains(field)) {
                    try {
                        value = annotatedClass.getMethod("get" + getUppercaseStartingFielName(field)).invoke(object);
                    } catch (Exception e) {
                        throw new IllegalStateException("Can't call " + annotatedClass.getCanonicalName() + ".get" + getUppercaseStartingFielName(field) + "()", e);
                    }
                } else {
                    field.setAccessible(true);
                    value = field.get(object);
                }
                if (key instanceof ListDataKey) {
                    final ListDataKey<Object> listDataKey = (ListDataKey<Object>) key;
                    node.setObjectList(listDataKey, (List<Object>) value);
                } else {
                    final DataKey<Object> dataKey = (DataKey<Object>) key;
                    node.setObject(dataKey, value);
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Can't read from " + annotatedClass.getCanonicalName() + "." + field.getName(), ex);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Can't read from " + annotatedClass.getCanonicalName() + "." + field.getName(), ex);
            }

        }
        return node;
    }

    @Override
    public T unMarshal(DataNode node) {
        try {
            final Map<Field, Object> readValues = new HashMap<>();
            final Object[] constructorParameters = new Object[constructorToUse.getParameterCount()];

            for (Entry<Field, Key<Object>> entry : dataKeys.entrySet()) {
                final Field field = entry.getKey();
                final Key<Object> key = entry.getValue();

                final Object value;
                if (key instanceof ListDataKey) {
                    value = node.getObjectList((ListDataKey<Object>) key);
                } else {
                    value = node.getObject((DataKey<Object>) key);
                }

                final Integer index = fieldNameToConstructorParameterIndex.get(field.getName());
                if (index != null) {
                    constructorParameters[index] = value;
                } else {
                    readValues.put(field, value);
                }
            }

            //construct object
            final T result = constructorToUse.newInstance(constructorParameters);

            for (Entry<Field, Object> entry : readValues.entrySet()) {
                final Field field = entry.getKey();
                final Object value = entry.getValue();

                if (setterSet.contains(field)) {
                    try {
                        Method setter = this.annotatedClass.getMethod("set" + getUppercaseStartingFielName(field), field.getType());
                        setter.invoke(result, value);
                    } catch (NoSuchMethodException ex) {
                        throw new IllegalStateException("Problem calling " + annotatedClass.getCanonicalName() + ".set" + getUppercaseStartingFielName(field) + "()", ex);
                    } catch (SecurityException ex) {
                        throw new IllegalStateException("Problem calling " + annotatedClass.getCanonicalName() + ".set" + getUppercaseStartingFielName(field) + "()", ex);
                    } catch (IllegalAccessException ex) {
                        throw new IllegalStateException("Problem calling " + annotatedClass.getCanonicalName() + ".set" + getUppercaseStartingFielName(field) + "()", ex);
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalStateException("Problem calling " + annotatedClass.getCanonicalName() + ".set" + getUppercaseStartingFielName(field) + "()", ex);
                    } catch (InvocationTargetException ex) {
                        throw new IllegalStateException("Problem calling " + annotatedClass.getCanonicalName() + ".set" + getUppercaseStartingFielName(field) + "()", ex);
                    }
                } else {
                    try {
                        field.set(result, value);
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalStateException("Problem setting field " + annotatedClass.getCanonicalName() + "." + field.getName(), ex);
                    } catch (IllegalAccessException ex) {
                        throw new IllegalStateException("Problem setting field " + annotatedClass.getCanonicalName() + "." + field.getName(), ex);
                    }
                }

            }

            return result;

        } catch (InstantiationException ex) {
            throw new IllegalStateException("Can't construct " + annotatedClass.getCanonicalName(), ex);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Can't construct " + annotatedClass.getCanonicalName(), ex);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Can't construct " + annotatedClass.getCanonicalName(), ex);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("Can't construct " + annotatedClass.getCanonicalName(), ex);
        }
    }

    /**
     * returns the boxed type of the primitive class or the
     * class itself if there is no wrapper for the given type.
     *
     * @param clazz
     * @return
     */
    private static Class<?> box(Class<?> clazz) {
        if (clazz == int.class) {
            return Integer.class;
        } else
            if (clazz == float.class) {
                return Float.class;
            } else
                if (clazz == long.class) {
                    return Long.class;
                } else
                    if (clazz == double.class) {
                        return double.class;
                    } else
                        if (clazz == boolean.class) {
                            return Boolean.class;
                        } else
                            if (clazz == char.class) {
                                return Character.class;
                            } else
                                if (clazz == byte.class) {
                                    return Byte.class;
                                }
        return clazz;
    }

}
