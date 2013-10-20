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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * Marshalls URLs
 * @author Florian Frankenberger
 */
public class URLMarshaller implements DataMarshaller<URL> {

    private static final DataKey<String> KEY_URL_STRING = DataKey.create("url_string", String.class, false);
    
    @Override
    public String getDataClassName() {
        return URL.class.getCanonicalName();
    }

    @Override
    public Class<URL> getDataClass() {
        return URL.class;
    }

    @Override
    public List<DataMarshaller<?>> getRequiredMarshallers() {
        return Collections.emptyList();
    }

    @Override
    public DataNode marshal(URL object) {
        final DataNode node = new DataNode();
        node.setObject(KEY_URL_STRING, object.toExternalForm());
        return node;
    }

    @Override
    public URL unMarshal(DataNode node) {
        final String urlString = node.getObject(KEY_URL_STRING);
        try {
            return new URL(urlString);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("the stored url (" + urlString + ") is not valid");
        }
    }

}
