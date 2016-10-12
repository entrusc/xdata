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
package com.moebiusgames.xdata.marshaller;

import com.moebiusgames.xdata.AbstractDataMarshaller;
import com.moebiusgames.xdata.DataKey;
import com.moebiusgames.xdata.DataMarshaller;
import com.moebiusgames.xdata.DataNode;
import com.moebiusgames.xdata.ListDataKey;
import com.moebiusgames.xdata.type.GenericType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

/**
 *
 * @author Florian Frankenberger
 */
public class MapMarshaller<K, V> implements DataMarshaller<Map<K, V>>{

    private final List<AbstractDataMarshaller<?>> marshallers = new ArrayList<>();
    private final ListDataKey<Tupel<K, V>> DATA_KEY = ListDataKey.create("data", new GenericType<Tupel<K, V>>() {});

    private final Supplier<Map<K, V>> supplier;
    private final Class<? extends Map> mapClass;
    private final Class<K> keyClass;
    private final Class<V> valueClass;

    public MapMarshaller(Supplier<Map<K, V>> supplier, Class<? extends Map> mapClass,
            Class<K> keyClass, Class<V> valueClass, AbstractDataMarshaller<?>... marshallers) {
        this.supplier = supplier;
        this.mapClass = mapClass;
        this.keyClass = keyClass;
        this.valueClass = valueClass;

        this.marshallers.add(Tupel.MARSHALLER);
        this.marshallers.addAll(Arrays.asList(marshallers));
    }

    @Override
    public Class<Map<K, V>> getDataClass() {
        return (Class<Map<K, V>>) mapClass;
    }

    @Override
    public String getDataClassName() {
        return "hashmap";
    }

    @Override
    public List<? extends AbstractDataMarshaller<?>> getRequiredMarshallers() {
        return (List<? extends AbstractDataMarshaller<?>>) marshallers;
    }

    @Override
    public DataNode marshal(Map<K, V> object) {
        final DataNode dataNode = new DataNode();
        final List<Tupel<K, V>> tupels = new ArrayList<>();
        for (Entry<K, V> entry : object.entrySet()) {
            tupels.add(new Tupel<>(entry.getKey(), entry.getValue()));
        }
        dataNode.setObjectList(DATA_KEY, tupels);
        return dataNode;
    }

    @Override
    public Map<K, V> unMarshal(DataNode node) {
        Map<K, V> map = supplier.get();
        List<Tupel<K, V>> tupels = node.getMandatoryObjectList(DATA_KEY);

        K key;
        V value;
        for (Tupel<K, V> tupel : tupels) {
            key = tupel.getT1();
            value = tupel.getT2();
            checkValues(key, value);
            map.put(key, value);
        }
        return map;
    }

    private void checkValues(K key, V value) {
        if (!keyClass.isAssignableFrom(key.getClass())) {
            throw new IllegalStateException("a deserialized key of the map is of type " + key.getClass() + " which is not a subtype of " + keyClass);
        }
        if (!valueClass.isAssignableFrom(value.getClass())) {
            throw new IllegalStateException("a deserialized value of the map is of type " + value.getClass() + " which is not a subtype of " + valueClass);
        }
    }

    private static class Tupel<T1, T2> {

        public static final TupelMarshaller MARSHALLER = new TupelMarshaller();

        private final T1 t1;
        private final T2 t2;

        public Tupel(T1 t1, T2 t2) {
            this.t1 = t1;
            this.t2 = t2;
        }

        public T1 getT1() {
            return t1;
        }

        public T2 getT2() {
            return t2;
        }

    }

    private static class TupelMarshaller implements DataMarshaller<Tupel> {

        private final DataKey<Object> T1_KEY = DataKey.create("t1", Object.class);
        private final DataKey<Object> T2_KEY = DataKey.create("t2", Object.class);

        @Override
        public Class<Tupel> getDataClass() {
            return Tupel.class;
        }

        @Override
        public DataNode marshal(Tupel object) {
            DataNode node = new DataNode();
            node.setObject(T1_KEY, object.getT1());
            node.setObject(T2_KEY, object.getT2());
            return node;
        }

        @Override
        public Tupel unMarshal(DataNode node) {
            return new Tupel(node.getMandatoryObject(T1_KEY), node.getMandatoryObject(T2_KEY));
        }

        @Override
        public String getDataClassName() {
            return "tupel";
        }

        @Override
        public List<? extends AbstractDataMarshaller<?>> getRequiredMarshallers() {
            return Collections.EMPTY_LIST; //we use the marshaller from the sourrounding class
        }

    }


}
