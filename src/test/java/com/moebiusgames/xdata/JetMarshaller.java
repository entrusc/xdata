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

import com.moebiusgames.xdata.DataKey;
import com.moebiusgames.xdata.DataMarshaller;
import com.moebiusgames.xdata.DataNode;
import java.util.Collections;
import java.util.List;

/**
 * A sample marshaller to marshal a Car object
 * 
 * @author Florian Frankenberger
 */
public class JetMarshaller implements DataMarshaller<Jet> {

    private static final DataKey<Float> KEY_SPEED = DataKey.create("speed", Float.class);
    
    @Override
    public String getDataClassName() {
        return "xdata.test.jet";
    }
    
    @Override
    public Class<Jet> getDataClass() {
        return Jet.class;
    }

    @Override
    public DataNode marshal(Jet object) {
        DataNode node = new DataNode();
        node.setObject(KEY_SPEED, object.getSpeed());
        return node;
    }

    @Override
    public Jet unMarshal(DataNode node) {
        return new Jet(node.getObject(KEY_SPEED));
    }

    @Override
    public List<DataMarshaller<?>> getRequiredMarshallers() {
        return Collections.emptyList();
    }

}
