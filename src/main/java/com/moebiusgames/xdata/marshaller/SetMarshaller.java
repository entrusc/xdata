/*
 * Copyright (C) 2018 Florian Frankenberger.
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
import com.moebiusgames.xdata.DataMarshaller;
import com.moebiusgames.xdata.DataNode;
import com.moebiusgames.xdata.ListDataKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A generic set marshaller that stores items as list
 * and reconstructs the set with the given supplier.
 * <p>
 * Note that enforcing of parameter types is not possible, so you need to make
 * sure that you check the deserialized maps yourself if they contain the correct
 * types (see also: type erasure).
 * </p>
 * @author Florian Frankenberger
 * @param <S>
 */
public class SetMarshaller<S extends Set> implements DataMarshaller<S> {

    private final List<AbstractDataMarshaller<?>> marshallers = new ArrayList<>();
    private final ListDataKey<Object> DATA_KEY = ListDataKey.create("data", Object.class);

    private final Supplier<S> supplier;
    private final Class<S> setClass;

    public SetMarshaller(Supplier<S> supplier, Class<S> setClass,
            AbstractDataMarshaller<?>... marshallers) {
        this.supplier = supplier;
        this.setClass = setClass;

        this.marshallers.addAll(Arrays.asList(marshallers));
    }

    @Override
    public Class<S> getDataClass() {
        return setClass;
    }

    @Override
    public String getDataClassName() {
        return "set";
    }

    @Override
    public List<? extends AbstractDataMarshaller<?>> getRequiredMarshallers() {
        return (List<? extends AbstractDataMarshaller<?>>) marshallers;
    }

    @Override
    public DataNode marshal(S object) {
        final DataNode dataNode = new DataNode();
        dataNode.setObjectList(DATA_KEY, new ArrayList<>(object));
        return dataNode;
    }

    @Override
    public S unMarshal(DataNode node) {
        Set<Object> set = (Set<Object>) supplier.get();
        set.addAll(node.getMandatoryObjectList(DATA_KEY));
        return (S) set;
    }


}
