/*
 * Copyright (C) 2015 Florian Frankenberger.
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
package com.moebiusgames.xdata.marshaller;

import com.moebiusgames.xdata.AbstractDataMarshaller;
import com.moebiusgames.xdata.DataKey;
import com.moebiusgames.xdata.DataMarshaller;
import com.moebiusgames.xdata.DataNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An enum marshaller. Note that you need to add one for every enum type
 * you want to marshal/unmarshal.
 *
 * @author Florian Frankenberger
 * @param <T>
 */
public class EnumMarshaller<T extends Enum<T>> implements DataMarshaller<T> {

    private final DataKey<String> KEY_VALUE = DataKey.create("value", String.class);

    private final String typeName;

    private final Map<String, T> mappings = new HashMap<String, T>();
    private final Class<T> clazz;
    private final T[] universe;
    private final T fallback;

    /**
     * constructor that uses the enum's simple class name as
     * part of the type name (otherwise xdata could not tell the
     * different enum types apart)
     *
     * @param clazz the enum class
     * @param fallback fallback type to use if an enum value could not be mapped
     */
    public EnumMarshaller(Class<T> clazz, T fallback) {
        this(clazz, clazz.getSimpleName(), fallback);
    }

    /**
     * full constructor
     *
     * @param clazz the enum class
     * @param typeName type name as part of the enum type name "xdata.enum.&lt;typeName&gt;"
     * @param fallback fallback type to use if an enum value could not be mapped
     */
    public EnumMarshaller(Class<T> clazz, String typeName, T fallback) {
        this.clazz = clazz;
        this.typeName = typeName;
        this.fallback = fallback;
        this.universe = clazz.getEnumConstants();
    }

    public void addMapping(String oldEnumName, T newEnumValue) {
        this.mappings.put(oldEnumName, newEnumValue);
    }

    @Override
    public Class<T> getDataClass() {
        return clazz;
    }

    @Override
    public String getDataClassName() {
        return "xdata.enum." + typeName;
    }

    @Override
    public List<? extends AbstractDataMarshaller<?>> getRequiredMarshallers() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public DataNode marshal(T aEnum) {
        DataNode node = new DataNode();
        node.setObject(KEY_VALUE, aEnum.name());
        return node;
    }

    @Override
    public T unMarshal(DataNode node) {
        final String storedValue = node.getMandatoryObject(KEY_VALUE);
        T mappedValue = this.mappings.get(storedValue);
        if (mappedValue == null) {
            for (T value : universe) {
                if (value.name().equals(storedValue)) {
                    mappedValue = value;
                    break;
                }
            }
        }
        if (mappedValue == null) {
            mappedValue = fallback;
        }
        return mappedValue;
    }


}
