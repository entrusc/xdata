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
package com.moebiusgames.xdata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A annotated field will be read/set by a AnnoataionBasedMarshaller (even
 * when this field is private). The AnnoationBasedMarshaller will use setter
 * and getter with the name schema setXyz() and getXyz() (assuming the field name
 * is xyz) when they are declared. When they are not declared, then the field
 * will be read/set directly. The default value of the field is the constructor
 * set value. All other options are similar to DataKey<T> and ListDataKey<T>.
 *
 * If the field is set final then it is expected that there is a constructor
 * with all final variables as parameters. If there are multiple constructors
 * then the algorithm will choose the one that fits all annotated final field
 * names.
 *
 * @author Florian Frankenberger
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MarshallableField {

    String name();

}
