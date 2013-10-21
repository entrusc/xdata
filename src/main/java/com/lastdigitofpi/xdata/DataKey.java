/*
 * Copyright (C) 2013 Florian Frankenberger.
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
package com.lastdigitofpi.xdata;

/**
 * A data key to use with DataNode. You should declare your data keys
 * like this:
 * <p/>
 * <pre>
 *      public static final DataKey&lt;Boolean&gt; MY_KEY = DataKey.create("mykey", Boolean.class);
 * </pre>
 * @author Florian Frankenberger
 * @param <T>
 */
public final class DataKey<T> implements Key<T> {

    private final String name;
    private final Class<T> dataClass;
    private final boolean allowNull;
    private final T defaultValue;
    
    private DataKey(String name, Class<T> dataClass, boolean allowNull, T defaultValue) {
        this.name = name;
        this.dataClass = dataClass;
        this.allowNull = allowNull;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getName() {
        return name;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Class<T> getDataClass() {
        return dataClass;
    }

    public boolean allowNull() {
        return allowNull;
    }

    @Override
    public String toString() {
        return "DataKey{" + "name=" + name + ", dataClass=" + dataClass + ", defaultValue=" + defaultValue + '}';
    }
    
    /**
     * creates a data key with the given name and type. This data
     * key allows for null values and sets no default value.
     * 
     * @param <T> the type
     * @param name the name of the key that is stored in the xdata file
     * @param dataClass the type's class
     * @return 
     */
    public static <T> DataKey<T> create(String name, Class<T> dataClass) {
        return create(name, dataClass, true, null);
    }
    
    /**
     * creates a data key with the given name, type and default value. This
     * data key allows for null values.
     * 
     * @param <T> the type
     * @param name the name of the key that is stored in the xdata file
     * @param dataClass the type's class
     * @param defaultValue the default value to use when there is no value set for this key.
     *                     If set to null this will be the same as setting no default value.
     * @return 
     */
    public static <T> DataKey<T> create(String name, Class<T> dataClass, T defaultValue) {
        return create(name, dataClass, true, defaultValue);
    }
    
    /**
     * creates a data key with the given name, data type and the given permission
     * to allow null values. No default value will be specified.
     * 
     * @param <T> the type
     * @param name the name of the key that is stored in the xdata file
     * @param dataClass the type's class
     * @param allowNull allow null values?
     * @return 
     */
    public static <T> DataKey<T> create(String name, Class<T> dataClass, boolean allowNull) {
        return create(name, dataClass, allowNull, null);
    }
    
    /**
     * creates a data key with the given name, data type, the given permission
     * to allow null values and a default value. You can set the default value to
     * null to signal that there should be no default value.
     * 
     * @param <T> the type
     * @param name the name of the key that is stored in the xdata file
     * @param dataClass the type's class
     * @param allowNull allow null values?
     * @param defaultValue the default value to use when there is no value set for this key.
     *                     If set to null this will be the same as setting no default value.
     * @return 
     */
    public static <T> DataKey<T> create(String name, Class<T> dataClass, boolean allowNull, T defaultValue) {
        if (dataClass.isPrimitive()) {
            throw new IllegalArgumentException("primitives are not supported - please use their corresponding wrappers");
        }
        if (dataClass.isArray()) {
            throw new IllegalArgumentException("arrays are not supported - please use a list with their corresponding wrapper");
        }
        return new DataKey(name, dataClass, allowNull, defaultValue);
    }
    
}
