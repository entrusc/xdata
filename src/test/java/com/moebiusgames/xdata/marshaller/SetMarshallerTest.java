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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Florian Frankenberger
 */
public class SetMarshallerTest {

    private static final DataKey<HashSet<String>> KEY_MAP = DataKey.create("set", new GenericType<HashSet<String>>() {});

    public SetMarshallerTest() {
    }

    @Test
    public void marshallingTest() throws IOException {
        File tmpFile = File.createTempFile("xdata_hashset_test", ".xdata");
        tmpFile.deleteOnExit();

        SetMarshaller<HashSet> serializer = new SetMarshaller<>(HashSet::new, HashSet.class);

        HashSet<String> set = new HashSet<>();
        set.add("Hello World?");
        set.add("Second Entry");

        DataNode dataNode = new DataNode();
        dataNode.setObject(KEY_MAP, set);
        XData.store(dataNode, tmpFile, serializer);

        DataNode restoredNode = XData.load(tmpFile, serializer);
        Set<String> restoredSet = restoredNode.getMandatoryObject(KEY_MAP);

        assertEquals(set, restoredSet);
    }

    @Test
    public void marshallingTestWithDifferentMaps() throws IOException {
        File tmpFile = File.createTempFile("xdata_hashset_test", ".xdata");
        tmpFile.deleteOnExit();

        SetMarshaller serializer = new SetMarshaller(HashSet::new, HashSet.class);
        SetMarshaller serializer2 = new SetMarshaller(LinkedHashSet::new, LinkedHashSet.class);

        HashSet<String> map = new HashSet<>();
        map.add("Hello World?");
        map.add("Second Entry");

        DataNode dataNode = new DataNode();
        dataNode.setObject(KEY_MAP, map);
        XData.store(dataNode, tmpFile, serializer);

        DataNode restoredNode = XData.load(tmpFile, serializer2);
        Set<String> restoredSet = restoredNode.getMandatoryObject(KEY_MAP);

        assertEquals(LinkedHashSet.class, restoredSet.getClass());
        assertEquals(map, restoredSet);
    }


}
