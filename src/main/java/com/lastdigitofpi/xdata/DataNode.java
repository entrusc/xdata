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
package com.lastdigitofpi.xdata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Contains data that can be stored as xdata file. Note that you can also store
 * data nodes as objects within this data node.
 * 
 * @author Florian Frankenberger
 */
public class DataNode {

    private final Map<String, Object> data = new HashMap<String, Object>();
    
    /**
     * returns the associated key (if it exists). If the key has no associated
     * value but the key has defined a default value then the value is returned
     * instead.
     * 
     * @param <T>
     * @param key
     * @return 
     */
    public <T> T getObject(DataKey<T> key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        final Object object = data.get(key.getName());
        if (containsKey(key)) {
            if (object != null) {
                if (!key.getDataClass().equals(object.getClass())) {
                    throw new IllegalStateException("the type of the key (" + key.getClass().getCanonicalName() + ") does not match the type of the value (" 
                            + object.getClass().getCanonicalName() + ")");
                }
            }
        } else {
            if (key.getDefaultValue() != null) {
                return key.getDefaultValue();
            }
        }
        if (object == null && !key.allowNull()) {
            throw new IllegalStateException("key \"" + key.getName() + "\" does not allow null values but the object is null");
        }
        return (T) object;
    }
    
    public <T> T getMandatoryObject(DataKey<T> key) {
        if (!containsKey(key)) {
            throw new IllegalStateException("no value for key \"" + key.getName() + "\" found, but was mandatory");
        }
        return getObject(key);
    }

    public <T> List<T> getObjectList(ListDataKey<T> key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        
        final Object object = data.get(key.getName());
        
        if (object == null && !key.allowNull()) {
            return new ArrayList<T>();
        }
        
        return (List<T>) object;
    }
    
    public <T> List<T> getMandatoryObjectList(ListDataKey<T> key) {
        if (!containsKey(key)) {
            throw new IllegalStateException("no value for list key \"" + key.getName() + "\" found, but was mandatory");
        }
        return getObjectList(key);
    }    
    
    /**
     * stores an object for the given key
     * 
     * @param <T>
     * @param key
     * @param object 
     */
    public <T> void setObject(DataKey<T> key, T object) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (object == null && !key.allowNull()) {
            throw new IllegalArgumentException("key \"" + key.getName() + "\" disallows null values but object was null");
        }
        data.put(key.getName(), object);
    }
    
    /**
     * stores a list of objects for a given key
     * 
     * @param <T>
     * @param key a list data key (where isList is set to true)
     * @param objects 
     */
    public <T> void setObjectList(ListDataKey<T> key, List<T> objects) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (objects == null && !key.allowNull()) {
            throw new IllegalArgumentException("list key \"" + key.getName() + "\" disallows null values but object was null");
        }
        data.put(key.getName(), deepListCopy(objects));
    }
    
    /**
     * checks if a value for that key exists (even if this value is null)
     * 
     * @param <T>
     * @param key
     * @return 
     */
    public <T> boolean containsKey(Key<T> key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return data.containsKey(key.getName());
    }
    
    public int getSize() {
        return data.size();
    }
    
    void replaceObject(String key, Object object) {
        this.data.put(key, object);
    }
    
    public void clearObject(DataKey<?> key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        data.remove(key.getName());
    }
    
    Set<Entry<String, Object>> getAll() {
        return data.entrySet();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("O Data Node\n");
        sb.append(this.toString(0));
        return sb.toString();
    }
    
    public String toString(int depth) {
        final String tabs = spaces(depth);
        
        StringBuilder out = new StringBuilder();
        
        for (Entry<String, Object> entry : getAll()) {
            final String key = entry.getKey();
            final Object object = entry.getValue();

            if (object instanceof DataNode) {
                DataNode otherNode = (DataNode) object;
                out.append(String.format("%s|---O %s: DataNode\n", tabs, key));
                out.append(otherNode.toString(depth + 4));
            } else
                if (object == null || !(object instanceof List)) {
                    out.append(String.format("%s|- %-30s = %s\n", tabs, key, object == null ? "[null]" : object.toString()));
                } else {
                    List<Object> list = (List<Object>) object;
                    out.append(String.format("%s|---O %s: List (%d)\n", tabs, key, list.size()));
                    listToString(out, depth, list);
                }
        }
        return out.toString();
    }
    
    private static void listToString(StringBuilder out, int depth, List<Object> list) {
            int size = list.size();
            int strLen = String.valueOf(size).length();
            final String spacePlus4 = spaces(depth + 4);
            for (int i = 0; i < size; ++i) {
                final Object element = list.get(i);
                if (element instanceof DataNode) {
                    final DataNode aNode = (DataNode) element;
                    out.append(String.format("%s|-[%" + strLen + "d]-O DataNode\n", spacePlus4, i));
                    out.append(aNode.toString(depth + 4 + strLen + 5));
                } else
                    if (element instanceof List) {
                        List<Object> aList = (List<Object>) element;
                        out.append(String.format("%s|-[%" + strLen + "d]-O List (%d)\n", spacePlus4, i, aList.size()));
                        listToString(out, depth + strLen + 5, aList);
                    } else {
                        out.append(String.format("%s|-[%" + strLen + "d] %s\n", spacePlus4, i, element != null ? element.toString() : "[null]"));
                    }
            }
        
    }
    
    private static String spaces(int length) {
        StringBuilder tabsBuilder = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            tabsBuilder.append(' ');
        }
        return tabsBuilder.toString();
    }
    
    private static <T> List<T> deepListCopy(List<T> list) {
        final List<T> newList = new ArrayList<T>();
        for (T element : list) {
            T newElement = element;
            if (element instanceof List) {
                newElement = (T) deepListCopy((List<Object>) element);
            }
            newList.add(newElement);
        }
        return newList;
    }

}
