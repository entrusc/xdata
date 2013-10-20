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
package com.lastdigitofpi.xdata.marshaller;

import com.lastdigitofpi.xdata.DataKey;
import com.lastdigitofpi.xdata.DataMarshaller;
import com.lastdigitofpi.xdata.DataNode;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Marshalls {@link java.util.Date}
 * 
 * @author Florian Frankenberger
 */
public class DateMarshaller implements DataMarshaller<Date> {

    private static final DataKey<Long> KEY_TIMESTAMP = DataKey.create("timestamp", Long.class);
    
    @Override
    public String getDataClassName() {
        return Date.class.getCanonicalName();
    }

    @Override
    public Class<Date> getDataClass() {
        return Date.class;
    }

    @Override
    public DataNode marshal(Date object) {
        final DataNode dataNode = new DataNode();
        dataNode.setObject(KEY_TIMESTAMP, object.getTime());
        return dataNode;
    }

    @Override
    public Date unMarshal(DataNode node) {
        return new Date(node.getObject(KEY_TIMESTAMP));
    }

    @Override
    public List<DataMarshaller<?>> getRequiredMarshallers() {
        return Collections.emptyList();
    }

}
