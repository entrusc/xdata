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

/**
 * Marshals an object of type T into a DataNode and
 * back. This way it is always defined how to map fields
 * to fields of a type - therefore guaranteeing a maximum
 * of data portability.
 *
 * @author Florian Frankenberger
 * @param <T>
 */
public interface DataMarshaller<T> extends AbstractDataMarshaller<T> {

    /**
     * returns the actual class that this marshaller is capable of marshalling.
     * @return
     */
    Class<T> getDataClass();

}

