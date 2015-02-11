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
package com.moebiusgames.xdata;

import com.moebiusgames.xdata.type.GenericType;

/**
 * A list data key to use with DataNode. You should declare your data keys
 * like this:
 * <p/>
 * <pre>
 *      public static final ListDataKey&lt;Boolean&gt; MY_KEY = ListDataKey.create("mykey", Boolean.class);
 *      public static final ListDataKey&lt;List&lt;String&gt;&gt; KEY_STRING_LIST = ListDataKey.create("strings", new GenericType&lt;List&lt;String&gt;&gt;() {});
 * </pre>
 * @author Florian Frankenberger
 * @param <T>
 */
public final class ListDataKey<T> implements Key<T> {

    private final String name;
    private final Class<T> dataClass;
    private final boolean allowNull;

    private ListDataKey(String name, Class<T> dataClass, boolean allowNull) {
        this.name = name;
        this.dataClass = dataClass;
        this.allowNull = allowNull;
    }

    @Override
    public String getName() {
        return name;
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
        return "ListDataKey{" + "name=" + name + ", dataClass=" + dataClass + ", allowNull=" + allowNull + '}';
    }

    /**
     * creates a list data key with the given name and type. Null types are allowed.
     *
     * @param <T> the type of the value
     * @param name the name of the key that is stored in the xdata file
     * @param dataClass the class of the type
     * @return
     */
    public static <T> ListDataKey<T> create(String name, Class<T> dataClass) {
        return create(name, dataClass, true);
    }

    /**
     * creates a list data key with the given name and type. If you allow for null values
     * then the list might as well be null. Otherwise only actual lists are allowed to be
     * stored and retrieved.
     *
     * @param <T> the type of the value
     * @param name the name of the key that is stored in the xdata file
     * @param dataClass the class of the type
     * @param allowNull if null is allowed or not
     * @return
     */
    public static <T> ListDataKey<T> create(String name, Class<T> dataClass, boolean allowNull) {
        if (dataClass.isPrimitive()) {
            throw new IllegalArgumentException("primitives are not supported - please use their corresponding wrappers");
        }
        if (dataClass.isArray()) {
            throw new IllegalArgumentException("arrays are not supported - please use a list with their corresponding wrapper");
        }
        return new ListDataKey(name, dataClass, allowNull);
    }

    /**
     * creates a list data key with the given name and type. Null types are allowed.
     *
     * @param <T> the type of the value
     * @param name the name of the key that is stored in the xdata file
     * @param genType the genericType of the class of the type
     * @return
     */
    public static <T> ListDataKey<T> create(String name, GenericType<T> genType) {
        return create(name, genType.getRawType(), true);
    }

    /**
     * creates a list data key with the given name and type. If you allow for null values
     * then the list might as well be null. Otherwise only actual lists are allowed to be
     * stored and retrieved.
     *
     * @param <T> the type of the value
     * @param name the name of the key that is stored in the xdata file
     * @param genType the genericType of the class of the type
     * @param allowNull if null is allowed or not
     * @return
     */
    public static <T> ListDataKey<T> create(String name, GenericType<T> genType, boolean allowNull) {
        return create(name, genType.getRawType(), allowNull);
    }

}
