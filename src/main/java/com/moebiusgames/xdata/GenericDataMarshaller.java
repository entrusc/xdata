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
package com.moebiusgames.xdata;

import com.moebiusgames.xdata.type.GenericType;

/**
 * Same as a normal DataMarshaller only that it requires you
 * to return a GenericType&lt;T&gt; instead of a Class&lt;T&gt;, which should
 * save a lot of "casting" trouble when dealing with Generics.
 * <p/>
 * Example:
 * <pre>
 * public MyMarshaller&lt;List&lt;Car&gt;&gt; implements GenericDataMarshaller {
 *
 *      private static final GenericType&lt;List&lt;Car&gt;&gt; TYPE_CAR = new GenericType&lt;List&lt;Car&gt;&gt;() {};
 *
 *      //...
 *
 *      public GenericType&lt;List&lt;Car&gt;&gt; getDataGenericType() {
 *          return TYPE_CAR;
 *      }
 * }
 * </pre>
 *
 * @author Florian Frankenberger
 */
public interface GenericDataMarshaller<T> extends AbstractDataMarshaller<T> {

    GenericType<T> getDataGenericType();

}
