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

import com.lastdigitofpi.xdata.marshaller.DateMarshaller;
import com.lastdigitofpi.xdata.marshaller.URLMarshaller;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Main class for storing and loading xdata files
 * <p/>
 * Sample:
 * <pre>
 *  final static DataKey&lt;String&gt; MY_KEY = DataKey.create("mykey", String.class);
 *  //...
 *  DataNode node = new DataNode();
 *  node.setObject(MY_KEY, "hello world");
 *  XData.store(node, new File("somefile.xdata"));
 *  //...
 *  DataNode restoredNode = XData.load(new File("somefile.xdata"));
 *  //do sth with the data in the node e.g.
 *  System.out.println(node.getObject(MY_KEY));
 * </pre>
 * @author Florian Frankenberger
 */
public class XData {

    private static final DummyProgressListener DUMMY_PROGRESS_LISTENER = new DummyProgressListener();
    
    private static final byte[] XDATA_HEADER = new byte[] {'x', 'd', 'a', 't', 'a'};
    private static final DataKey<Integer> META_CLASS_ID = DataKey.create("_meta_classid", Integer.class);
    private static final Map<Class<?>, Serializer<?>> PRIMITIVE_SERIALIZERS_BY_CLASS = new HashMap<Class<?>, Serializer<?>>();
    private static final Map<Byte, Serializer<?>> PRIMITIVE_SERIALIZERS_BY_ID = new HashMap<Byte, Serializer<?>>();
    
    private static final List<DataMarshaller<?>> DEFAULT_MARSHALLERS = new ArrayList<DataMarshaller<?>>();
    
    private static final int VAL_NULL = 0;
    private static final int VAL_ELEMENT = 1;
    private static final int VAL_LIST = 2;
    private static final int VAL_NODE = 3;
    
    static {
        //default marshallers
        DEFAULT_MARSHALLERS.add(new DateMarshaller());
        DEFAULT_MARSHALLERS.add(new URLMarshaller());
        
        //primitive serializers
        final IntSerializer intSerializer = new IntSerializer();
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(Integer.class, intSerializer);
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(int.class, intSerializer);
        final LongSerializer longSerializer = new LongSerializer();
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(Long.class, longSerializer);
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(long.class, longSerializer);
        final ShortSerializer shortSerializer = new ShortSerializer();
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(Short.class, shortSerializer);
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(short.class, shortSerializer);
        final ByteSerializer byteSerializer = new ByteSerializer();
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(Byte.class, byteSerializer);
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(byte.class, byteSerializer);
        final CharSerializer charSerializer = new CharSerializer();
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(Character.class, charSerializer);
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(char.class, charSerializer);
        final FloatSerializer floatSerializer = new FloatSerializer();
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(Float.class, floatSerializer);
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(float.class, floatSerializer);
        final DoubleSerializer doubleSerializer = new DoubleSerializer();
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(Double.class, doubleSerializer);
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(double.class, doubleSerializer);
        final BooleanSerializer booleanSerializer = new BooleanSerializer();
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(Boolean.class, booleanSerializer);
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(boolean.class, booleanSerializer);
        final StringSerializer stringSerializer = new StringSerializer();
        PRIMITIVE_SERIALIZERS_BY_CLASS.put(String.class, stringSerializer);
        
        for (Serializer<?> serializer : PRIMITIVE_SERIALIZERS_BY_CLASS.values()) {
            PRIMITIVE_SERIALIZERS_BY_ID.put(serializer.getSerializerId(), serializer);
        }
    }
    
    private static interface Serializer<T> {
        
        byte getSerializerId();
        
        Class<T> getClazz();
        
        void serialize(T object, DataOutputStream dOut) throws IOException;
        
        T deserialize(DataInputStream dIn) throws IOException;
    }
    
    private static class ClassRegistry {
        private int maxId = 0;
        private final Map<String, Integer> classIds = new LinkedHashMap<String, Integer>();
        private final Map<Integer, String> idsClasses = new LinkedHashMap<Integer, String>();
        
        public int getId(String clazzCannonicalName) {
            Integer id = classIds.get(clazzCannonicalName);
            if (id == null) {
                id = maxId;
                maxId++;
                classIds.put(clazzCannonicalName, id);
                idsClasses.put(id, clazzCannonicalName);
            }
            return id;
        }
        
        public void serialize(DataOutputStream dOut) throws IOException {
            dOut.writeInt(classIds.size());
            for (String className : classIds.keySet()) {
                dOut.writeUTF(className);
            }
        }
        
        public void deserialize(DataInputStream dIn) throws IOException {
            classIds.clear();
            int size = dIn.readInt();
            for (int i = 0; i < size; ++i) {
                getId(dIn.readUTF());
            }
        }

        private String getClassName(int classId) {
            return idsClasses.get(classId);
        }
        
    }
    
    private XData() {
    }
    /**
     * loads a xdata file from disk using the given marshallers. For all classes other
     * than these a special marshaller is required to map the class' data to a data node
     * object:
     * <ul>
     * <li>Boolean</li>
     * <li>Long</li>
     * <li>Integer</li>
     * <li>String</li>
     * <li>Float</li>
     * <li>Double</li>
     * <li>Byte</li>
     * <li>Short</li>
     * <li>Character</li>
     * <li>DataNode</li>
     * <li>List&lt;?&gt;</li>
     * </ul>
     * <p/>
     * Also take a look at {@link com.lastdigitofpi.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     * 
     * @param file
     * @param marshallers
     * @return
     * @throws IOException 
     */
    public static DataNode load(File file, DataMarshaller<?>... marshallers) throws IOException {
        return load(file, DUMMY_PROGRESS_LISTENER, marshallers);
    }
    
    /**
     * loads a xdata file from disk using the given marshallers. For all classes other
     * than these a special marshaller is required to map the class' data to a data node
     * object:
     * <ul>
     * <li>Boolean</li>
     * <li>Long</li>
     * <li>Integer</li>
     * <li>String</li>
     * <li>Float</li>
     * <li>Double</li>
     * <li>Byte</li>
     * <li>Short</li>
     * <li>Character</li>
     * <li>DataNode</li>
     * <li>List&lt;?&gt;</li>
     * </ul>
     * <p/>
     * Also take a look at {@link com.lastdigitofpi.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     * 
     * @param file
     * @param progressListener
     * @param marshallers
     * @return
     * @throws IOException 
     */
    public static DataNode load(File file, ProgressListener progressListener, DataMarshaller<?>... marshallers) throws IOException {
        final Map<String, DataMarshaller<?>> marshallerMap = generateMarshallerMap(false, Arrays.asList(marshallers));
        marshallerMap.putAll(generateMarshallerMap(false, DEFAULT_MARSHALLERS));
        
        final ClassRegistry classRegistry = new ClassRegistry();
        
        DataInputStream dIn = new DataInputStream(new GZIPInputStream(new FileInputStream(file)));
        try {
            //check the header
            for (int i = 0; i < XDATA_HEADER.length; ++i) {
                if (dIn.readByte() != XDATA_HEADER[i]) {
                    throw new IOException(file + " is not a xdata file");
                }
            }
            
            final Object raw = deSerialize(dIn, progressListener);
            if (!(raw instanceof DataNode)) {
                throw new IOException("first object in xdata file MUST be a DataNode but was " 
                        + raw == null ? "null" : raw.getClass().getCanonicalName());
            }
            final DataNode dataNode = (DataNode) raw;
            classRegistry.deserialize(dIn);
            
            final Object rawUnMarshalled = unMarshal(classRegistry, marshallerMap, dataNode);
            if (!(rawUnMarshalled instanceof DataNode)) {
                throw new IOException("first object in xdata file MUST be a DataNode but was " 
                        + rawUnMarshalled == null ? "null" : rawUnMarshalled.getClass().getCanonicalName());
            }
            
            return (DataNode) rawUnMarshalled;
            
        } finally {
            dIn.close();
        }
    }
    
    private static Object deSerialize(DataInputStream dIn, ProgressListener progressListener) throws IOException {
        final int type = dIn.readByte();
        switch(type) {
            case VAL_NULL:
                return null;
            case VAL_ELEMENT:
                final byte elementType = dIn.readByte();
                final Serializer<Object> serializer = (Serializer<Object>)PRIMITIVE_SERIALIZERS_BY_ID.get(elementType);
                if (serializer == null) {
                    throw new IOException("can't deserialize type " + Integer.toHexString(elementType) + " (maybe newer format?).");
                }
                return serializer.deserialize(dIn);
            case VAL_LIST:
                final int listSize = dIn.readInt();

                //because of type erasure we just create a list
                //of objects here ...
                final List<Object> list = new ArrayList<Object>();
                
                for (int i = 0; i < listSize; ++i) {
                    final Object object = deSerialize(dIn, DUMMY_PROGRESS_LISTENER);
                    list.add(object);
                }
                return list;
            case VAL_NODE:
                final DataNode dataNode = new DataNode();
                
                int length = dIn.readInt();
                progressListener.onTotalSteps(length);
                
                for (int i = 0; i < length; ++i) {
                    final String key = dIn.readUTF();
                    final Object object = deSerialize(dIn, DUMMY_PROGRESS_LISTENER);
                    dataNode.replaceObject(key, object);
                    
                    progressListener.onStep();
                }

                return dataNode;

            default:
                throw new IOException("expected node (" + Integer.toHexString(type) + ") but found " + Integer.toHexString(type));
        }
        
    }
    
    private static Object unMarshal(ClassRegistry classRegistry, Map<String, DataMarshaller<?>> marshallerMap, DataNode node) throws IOException {
        final Map<String, Object> replacements = new HashMap<String, Object>();
        
        for (Entry<String, Object> entry : node.getAll()) {
            final String key = entry.getKey();
            final Object object = entry.getValue();
            if (object != null) {
                if (object instanceof DataNode) {
                    replacements.put(key, unMarshal(classRegistry, marshallerMap, (DataNode) object));
                } else
                    if (object instanceof List) {
                        final List<Object> list = (List<Object>) object;
                        unMarshalList(list, classRegistry, marshallerMap);
                    }
            }
        }
        
        for (Entry<String, Object> entry : replacements.entrySet()) {
            node.replaceObject(entry.getKey(), entry.getValue());
        }
        
        if (node.containsKey(META_CLASS_ID)) {
            final int classId = node.getObject(META_CLASS_ID);
            final String className = classRegistry.getClassName(classId);
            final DataMarshaller<Object> marshaller = (DataMarshaller<Object>) marshallerMap.get(className);
            if (marshaller == null) {
                throw new IOException("no marshaller found for class " + className);
            }
            return marshaller.unMarshal(node);
        } else {
            return node;
        }
    }
    
    private static void unMarshalList(final List<Object> list, ClassRegistry classRegistry, Map<String, DataMarshaller<?>> marshallerMap) throws IOException {
        for (int i = 0; i < list.size(); ++i) {
            final Object aObject = list.get(i);
            if (aObject != null) {
                if (aObject instanceof DataNode) {
                    list.set(i, unMarshal(classRegistry, marshallerMap, (DataNode) aObject));
                } else
                    if (aObject instanceof List) {
                        final List<Object> aList = (List<Object>) aObject;
                        unMarshalList(aList, classRegistry, marshallerMap);
                    }
            }
        }
    }
    
    /**
     * stores a datanode in a xdata file using the given marshallers. For all classes other
     * than these a special marshaller is required to map the class' data to a data node
     * object:
     * <ul>
     * <li>Boolean</li>
     * <li>Long</li>
     * <li>Integer</li>
     * <li>String</li>
     * <li>Float</li>
     * <li>Double</li>
     * <li>Byte</li>
     * <li>Short</li>
     * <li>Character</li>
     * <li>DataNode</li>
     * <li>List&lt;?&gt;</li>
     * </ul>
     * <p/>
     * Also take a look at {@link com.lastdigitofpi.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     * <p/>
     * Note that the node should not be reused after storing the data as the data has already been
     * marshalled in place!
     *
     * @param node
     * @param file
     * @param marshallers
     * @throws IOException 
     */
    public static void store(DataNode node, File file, DataMarshaller<?>... marshallers) throws IOException {
        store(node, file, DUMMY_PROGRESS_LISTENER, marshallers);
    }
    
    /**
     * stores a datanode in a xdata file using the given marshallers. For all classes other
     * than these a special marshaller is required to map the class' data to a data node
     * object:
     * <ul>
     * <li>Boolean</li>
     * <li>Long</li>
     * <li>Integer</li>
     * <li>String</li>
     * <li>Float</li>
     * <li>Double</li>
     * <li>Byte</li>
     * <li>Short</li>
     * <li>Character</li>
     * <li>DataNode</li>
     * <li>List&lt;?&gt;</li>
     * </ul>
     * <p/>
     * Also take a look at {@link com.lastdigitofpi.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     * <p/>
     * Note that the node should not be reused after storing the data as the data has already been
     * marshalled in place!
     *
     * @param node
     * @param file
     * @param progressListener
     * @param marshallers
     * @throws IOException 
     */
    public static void store(DataNode node, File file, ProgressListener progressListener, DataMarshaller<?>... marshallers) throws IOException {
        
        final Map<String, DataMarshaller<?>> marshallerMap = generateMarshallerMap(true, Arrays.asList(marshallers));
        marshallerMap.putAll(generateMarshallerMap(true, DEFAULT_MARSHALLERS));
        
        final ClassRegistry classRegistry = new ClassRegistry();
        
        DataOutputStream dOut = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
        try {
            //write header
            dOut.write(XDATA_HEADER);

            //serialize the node
            serialize(classRegistry, marshallerMap, dOut, node, progressListener);
            
            //serialize the class registry
            classRegistry.serialize(dOut);
        } finally {
            dOut.close();
        }
    }
    
    /**
     * wraps an object using the data marshaller for that given object or returns
     * a the object when it is already a DataNode, a primitive, array or string.
     * 
     * @param marshallerMap
     * @param object
     * @return 
     */
    private static Object marshalObject(Map<String, DataMarshaller<?>> marshallerMap, ClassRegistry classRegistry, Object object) {
        if (!isMarshalled(object)) {
            //can't be null here because it is not resolved: meaning, it is an instance of an unknown class
            final Class<?> clazz = object.getClass();

            if (object instanceof List) {
                final List<Object> list = (List<Object>) object;
                
                for (int i = 0; i < list.size(); ++i) {
                    final Object aObject = list.get(i);
                    list.set(i, marshalObject(marshallerMap, classRegistry, aObject));
                }
            } else {
                final DataMarshaller<Object> serializer = (DataMarshaller<Object>)marshallerMap.get(clazz.getCanonicalName());
                if (serializer == null) {
                    throw new IllegalStateException("No serializer defined for class " + clazz.getCanonicalName());
                }
                final DataNode node = serializer.marshal(object);
                node.setObject(META_CLASS_ID, classRegistry.getId(serializer.getDataClassName()));
                return node;
            }
        }
        return object;
    }
    
    /**
     * serializes a data node that just contains primitives or references to other data
     * nodes.
     * 
     * @param dataNode 
     */
    private static void serialize(ClassRegistry classRegistry, Map<String, DataMarshaller<?>> marshallerMap, 
            DataOutputStream dOut, DataNode dataNode, ProgressListener progressListener) throws IOException {
        dOut.writeByte(VAL_NODE);
        dOut.writeInt(dataNode.getSize());
        
        progressListener.onTotalSteps(dataNode.getSize());
        
        for (Entry<String, Object> entry : dataNode.getAll()) {
            final String key = entry.getKey();
            final Object object = entry.getValue();
            
            final Object resolvedObject = marshalObject(marshallerMap, classRegistry, object);
            
            dOut.writeUTF(key);
            serializeElement(classRegistry, marshallerMap, dOut, resolvedObject);
            
            progressListener.onStep();
        }
    }
    
    /**
     * serializes a single object
     * 
     * @param serializerMap
     * @param dOut
     * @param resolvedObject
     * @throws IOException 
     */
    private static void serializeElement(ClassRegistry classRegistry, Map<String, DataMarshaller<?>> marshallerMap, 
            DataOutputStream dOut, Object resolvedObject) throws IOException {
        if (resolvedObject == null) {
            dOut.writeByte(VAL_NULL);
        } else
            if (resolvedObject instanceof List) {
                final List<Object> list = (List<Object>) resolvedObject;
                final int size = list.size();
                
                dOut.writeByte(VAL_LIST);
                dOut.writeInt(size);
                
                for (int i = 0; i < size; ++i) {
                    serializeElement(classRegistry, marshallerMap, dOut, list.get(i));
                }                
            } else
                if (resolvedObject instanceof DataNode) {
                    final DataNode aDataNode = (DataNode) resolvedObject;
                    serialize(classRegistry, marshallerMap, dOut, aDataNode, DUMMY_PROGRESS_LISTENER);
                } else {
                    dOut.writeByte(VAL_ELEMENT);
                    final Class<?> resolvedObjectClass = resolvedObject.getClass();
                    Serializer<Object> serializer = (Serializer<Object>) PRIMITIVE_SERIALIZERS_BY_CLASS.get(resolvedObjectClass);
                    if (serializer == null) {
                        throw new IllegalStateException("Can't serialize resolved class " + resolvedObjectClass.getCanonicalName());
                    }
                    dOut.writeByte(serializer.getSerializerId());
                    serializer.serialize(resolvedObject, dOut);
                }
    }
    
    /**
     * checkes if the given object is already marshalled, that is
     * if it is a DataNode, primitive object or array.
     * 
     * @param object
     * @return 
     */
    private static boolean isMarshalled(Object object) {
        return (object == null 
            || PRIMITIVE_SERIALIZERS_BY_CLASS.containsKey(object.getClass())
            || object instanceof DataNode);
    }
    
    private static Map<String, DataMarshaller<?>> generateMarshallerMap(boolean fullyQualifiedClassName, List<DataMarshaller<?>> marshallers) {
        final Map<String, DataMarshaller<?>> map = new HashMap<String, DataMarshaller<?>>();
        for (DataMarshaller<?> marshaller : marshallers) {
            final String key = fullyQualifiedClassName ? marshaller.getDataClass().getCanonicalName() : marshaller.getDataClassName();
            map.put(key, marshaller);
            map.putAll(generateMarshallerMap(fullyQualifiedClassName, marshaller.getRequiredMarshallers()));
        }
        return map;
    }
    
    // --- Serializers ---
    private static class BooleanSerializer implements Serializer<Boolean> {

        @Override
        public byte getSerializerId() {
            return 0x00;
        }
        
        @Override
        public Class<Boolean> getClazz() {
            return Boolean.class;
        }
        
        @Override
        public void serialize(Boolean object, DataOutputStream dOut) throws IOException {
            dOut.writeBoolean(object);
        }

        @Override
        public Boolean deserialize(DataInputStream dIn) throws IOException {
            return dIn.readBoolean();
        }
        
    }     
    
    private static class ByteSerializer implements Serializer<Byte> {

        @Override
        public byte getSerializerId() {
            return 0x01;
        }
        
        @Override
        public Class<Byte> getClazz() {
            return Byte.class;
        }
        
        @Override
        public void serialize(Byte object, DataOutputStream dOut) throws IOException {
            dOut.writeByte(object);
        }

        @Override
        public Byte deserialize(DataInputStream dIn) throws IOException {
            return dIn.readByte();
        }
        
    }   
    
    private static class CharSerializer implements Serializer<Character> {

        @Override
        public byte getSerializerId() {
            return 0x02;
        }
        
        @Override
        public Class<Character> getClazz() {
            return Character.class;
        }
        
        @Override
        public void serialize(Character object, DataOutputStream dOut) throws IOException {
            dOut.writeChar(object);
        }

        @Override
        public Character deserialize(DataInputStream dIn) throws IOException {
            return dIn.readChar();
        }
        
    }       
    
    private static class ShortSerializer implements Serializer<Short> {

        @Override
        public byte getSerializerId() {
            return 0x03;
        }
          
        @Override
        public Class<Short> getClazz() {
            return Short.class;
        }
      
        @Override
        public void serialize(Short object, DataOutputStream dOut) throws IOException {
            dOut.writeShort(object);
        }

        @Override
        public Short deserialize(DataInputStream dIn) throws IOException {
            return dIn.readShort();
        }
        
    }     
    
    private static class IntSerializer implements Serializer<Integer> {
        
        @Override
        public byte getSerializerId() {
            return 0x04;
        }
        
        @Override
        public Class<Integer> getClazz() {
            return Integer.class;
        }

        @Override
        public void serialize(Integer object, DataOutputStream dOut) throws IOException {
            dOut.writeInt(object);
        }

        @Override
        public Integer deserialize(DataInputStream dIn) throws IOException {
            return dIn.readInt();
        }
        
    }
   
    
    private static class LongSerializer implements Serializer<Long> {
        
        @Override
        public byte getSerializerId() {
            return 0x05;
        }
        
        @Override
        public Class<Long> getClazz() {
            return Long.class;
        }

        @Override
        public void serialize(Long object, DataOutputStream dOut) throws IOException {
            dOut.writeLong(object);
        }

        @Override
        public Long deserialize(DataInputStream dIn) throws IOException {
            return dIn.readLong();
        }
        
    }    
    
    private static class FloatSerializer implements Serializer<Float> {
        
        @Override
        public byte getSerializerId() {
            return 0x06;
        }
        
        @Override
        public Class<Float> getClazz() {
            return Float.class;
        }

        @Override
        public void serialize(Float object, DataOutputStream dOut) throws IOException {
            dOut.writeFloat(object);
        }

        @Override
        public Float deserialize(DataInputStream dIn) throws IOException {
            return dIn.readFloat();
        }
        
    }   
    
    
    private static class DoubleSerializer implements Serializer<Double> {
        
        @Override
        public byte getSerializerId() {
            return 0x07;
        }
        
        @Override
        public Class<Double> getClazz() {
            return Double.class;
        }

        @Override
        public void serialize(Double object, DataOutputStream dOut) throws IOException {
            dOut.writeDouble(object);
        }

        @Override
        public Double deserialize(DataInputStream dIn) throws IOException {
            return dIn.readDouble();
        }
        
    }  
    
    private static class StringSerializer implements Serializer<String> {
        
        @Override
        public byte getSerializerId() {
            return 0x08;
        }
        
        @Override
        public Class<String> getClazz() {
            return String.class;
        }

        @Override
        public void serialize(String object, DataOutputStream dOut) throws IOException {
            dOut.writeUTF(object);
        }

        @Override
        public String deserialize(DataInputStream dIn) throws IOException {
            return dIn.readUTF();
        }
        
    }     
    
    private static class DummyProgressListener implements ProgressListener {

        @Override
        public void onTotalSteps(int totalSteps) {
        }

        @Override
        public void onStep() {
        }
        
    }
}
