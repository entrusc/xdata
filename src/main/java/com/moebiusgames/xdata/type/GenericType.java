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
package com.moebiusgames.xdata.type;

import java.lang.reflect.ParameterizedType;

/**
 * Helper class to prevent ugly casting scenarios when
 * defining keys in XData.
 * <p />
 * Example:
 * <pre>
 *     public static final ListDataKey&lt;List&lt;String&gt;&gt; KEY_STRING_LIST = ListDataKey.create("strings", new GenericType&lt;List&lt;String&gt;&gt;() {});
 * </pre>
 *
 * @author Florian Frankenberger
 * @param <T>
 */
public class GenericType<T> extends Capture<T> {

    private final ParameterizedType genericType;

    public GenericType() {
        this.genericType = capture();
    }

    public Class<T> getRawType() {
        return (Class<T>) this.genericType.getRawType();
    }

    @Override
    public String toString() {
        return "GenericType{" + "genericType=" + genericType.getClass() + '}';
    }
}
