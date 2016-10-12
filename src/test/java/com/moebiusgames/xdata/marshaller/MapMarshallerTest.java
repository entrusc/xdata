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

import com.moebiusgames.xdata.DataKey;
import com.moebiusgames.xdata.DataNode;
import com.moebiusgames.xdata.XData;
import com.moebiusgames.xdata.type.GenericType;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Florian Frankenberger
 */
public class MapMarshallerTest {

    private static final DataKey<HashMap<Integer, String>> KEY_MAP = DataKey.create("map", new GenericType<HashMap<Integer, String>>() {});
    private static final DataKey<HashMap<String, String>> KEY_MAP2 = DataKey.create("map", new GenericType<HashMap<String, String>>() {});

    public MapMarshallerTest() {
    }

    @Test
    public void marshallingTest() throws IOException {
        File tmpFile = File.createTempFile("xdata_hashmap_test", ".xdata");
        tmpFile.deleteOnExit();

        MapMarshaller<Integer, String> serializer = new MapMarshaller<>(HashMap::new, HashMap.class, Integer.class, String.class);

        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "Hello World?");
        map.put(2, "Second Entry");

        DataNode dataNode = new DataNode();
        dataNode.setObject(KEY_MAP, map);
        XData.store(dataNode, tmpFile, serializer);

        DataNode restoredNode = XData.load(tmpFile, serializer);
        HashMap<Integer, String> restoredMap = restoredNode.getMandatoryObject(KEY_MAP);

        assertEquals(map, restoredMap);
    }

    @Test
    public void marshallingTestWithDifferentMaps() throws IOException {
        File tmpFile = File.createTempFile("xdata_hashmap_test", ".xdata");
        tmpFile.deleteOnExit();

        MapMarshaller<Integer, String> serializer = new MapMarshaller<>(HashMap::new, HashMap.class, Integer.class, String.class);
        MapMarshaller<Integer, String> serializer2 = new MapMarshaller<>(LinkedHashMap::new, LinkedHashMap.class, Integer.class, String.class);

        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "Hello World?");
        map.put(2, "Second Entry");

        DataNode dataNode = new DataNode();
        dataNode.setObject(KEY_MAP, map);
        XData.store(dataNode, tmpFile, serializer);

        DataNode restoredNode = XData.load(tmpFile, serializer2);
        HashMap<Integer, String> restoredMap = restoredNode.getMandatoryObject(KEY_MAP);

        assertEquals(LinkedHashMap.class, restoredMap.getClass());
        assertEquals(map, restoredMap);
    }

    @Test (expected = IllegalStateException.class)
    public void marshallingTestWithIncompatibleTypes() throws IOException {
        File tmpFile = File.createTempFile("xdata_hashmap_test", ".xdata");
        tmpFile.deleteOnExit();

        MapMarshaller<Integer, String> serializer = new MapMarshaller<>(HashMap::new, HashMap.class, Integer.class, String.class);
        MapMarshaller<String, String> serializer2 = new MapMarshaller<>(HashMap::new, HashMap.class, String.class, String.class);

        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "Hello World?");
        map.put(2, "Second Entry");

        DataNode dataNode = new DataNode();
        dataNode.setObject(KEY_MAP, map);
        XData.store(dataNode, tmpFile, serializer);

        DataNode restoredNode = XData.load(tmpFile, serializer2);
        HashMap<Integer, String> restoredMap = restoredNode.getMandatoryObject(KEY_MAP);
    }

}
