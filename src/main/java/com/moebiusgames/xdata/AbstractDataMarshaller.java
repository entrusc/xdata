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

import java.util.List;

/**
 * This is the basic class for all marshallers. DON'T IMPLEMENT
 * THIS MARSHALLER DIRECTLY. USE ONE OF THE SUBINTERFACES:
 * <pre>
 *      * DataMarshaller<T>
 *      * GenericDataMarshaller<T>
 * </pre>
 * @author Florian Frankenberger
 */
public interface AbstractDataMarshaller<T> {

    /**
     * returns the (most possible unique name) of the class that this marshaller
     * is capable of marshalling. This string is written to the xdata file and used
     * for restoring und unmarshalling.
     * <p>
     * For standard java classes like java.util.Date this
     * should be the fully qualified classname. For other projects that should stay
     * compatible you should better find a better unique name like "xcylin.worldsetting"
     * that is not directly dependent to the package structure that won't survive
     * the next refactoring.
     * </p>
     * @return
     */
    String getDataClassName();

    /**
     * returns a list of other required marshallers. Say e.g. you
     * want to marshal class Car and need for it to use marshalling
     * for class Wheel you'd return a list with a WheelMarshaller
     * here.
     *
     * @return
     */
    List<? extends AbstractDataMarshaller<?>> getRequiredMarshallers();

    /**
     * marshalls the object to a DataNode
     *
     * @param object
     * @return
     */
    DataNode marshal(T object);

    /**
     * unmarshalls the object from a DataNode
     *
     * @param node
     * @return
     */
    T unMarshal(DataNode node);

}
