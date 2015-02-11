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

import com.moebiusgames.xdata.marshaller.DateMarshaller;
import com.moebiusgames.xdata.marshaller.URLMarshaller;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
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
    private static final DataKey<String> META_CLASS_NAME = DataKey.create("_meta_classname", String.class);
    private static final Map<Class<?>, Serializer<?>> PRIMITIVE_SERIALIZERS_BY_CLASS = new HashMap<Class<?>, Serializer<?>>();
    private static final Map<Byte, Serializer<?>> PRIMITIVE_SERIALIZERS_BY_ID = new HashMap<Byte, Serializer<?>>();

    private static final List<DataMarshaller<?>> DEFAULT_MARSHALLERS = new ArrayList<DataMarshaller<?>>();

    private static final int VAL_NULL = 0;
    private static final int VAL_ELEMENT = 1;
    private static final int VAL_LIST = 2;
    private static final int VAL_NODE = 3;
    private static final int VAL_REFERENCE = 4;

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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param file
     * @param marshallers
     * @return
     * @throws IOException
     */
    public static DataNode load(File file, AbstractDataMarshaller<?>... marshallers) throws IOException {
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param file
     * @param ignoreMissingMarshallers if this is set true then no IOException is thrown
     *                                 when a marshaller is missing.
     * @param marshallers
     * @return
     * @throws IOException
     */
    public static DataNode load(File file, boolean ignoreMissingMarshallers, DataMarshaller<?>... marshallers) throws IOException {
        return load(file, DUMMY_PROGRESS_LISTENER, ignoreMissingMarshallers, marshallers);
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param file
     * @param progressListener
     * @param marshallers
     * @return
     * @throws IOException
     */
    public static DataNode load(File file, ProgressListener progressListener, AbstractDataMarshaller<?>... marshallers) throws IOException {
        return load(file, progressListener, false, marshallers);
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param file
     * @param progressListener
     * @param ignoreMissingMarshallers if this is set true then no IOException is thrown
     *                                 when a marshaller is missing.
     * @param marshallers
     * @return
     * @throws IOException
     */
    public static DataNode load(File file, ProgressListener progressListener,
            boolean ignoreMissingMarshallers, AbstractDataMarshaller<?>... marshallers) throws IOException {
        return load(new FileInputStream(file), progressListener, ignoreMissingMarshallers, marshallers);
    }

    /**
     * loads a xdata file from from an inputstream using the given marshallers. For all classes other
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param in
     * @param marshallers
     * @return
     * @throws IOException
     */
    public static DataNode load(InputStream in, AbstractDataMarshaller<?>... marshallers) throws IOException {
        return load(in, DUMMY_PROGRESS_LISTENER, marshallers);
    }

    /**
     * loads a xdata file from from an inputstream using the given marshallers. For all classes other
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param in
     * @param ignoreMissingMarshallers if this is set true then no IOException is thrown
     *                                 when a marshaller is missing.
     * @param marshallers
     * @return
     * @throws IOException
     */
    public static DataNode load(InputStream in, boolean ignoreMissingMarshallers, AbstractDataMarshaller<?>... marshallers) throws IOException {
        return load(in, DUMMY_PROGRESS_LISTENER, ignoreMissingMarshallers, marshallers);
    }

    /**
     * loads a xdata file from from an inputstream using the given marshallers. For all classes other
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param in
     * @param progressListener
     * @param marshallers
     * @return
     * @throws IOException
     */
    public static DataNode load(InputStream in, ProgressListener progressListener, AbstractDataMarshaller<?>... marshallers) throws IOException {
        return load(in, progressListener, false, marshallers);
    }

    /**
     * loads a xdata file from an inputstream using the given marshallers. For all classes other
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param in
     * @param progressListener
     * @param ignoreMissingMarshallers if this is set true then no IOException is thrown
     *                                 when a marshaller is missing.
     * @param marshallers
     * @return
     * @throws IOException
     */
    public static DataNode load(InputStream in, ProgressListener progressListener,
            boolean ignoreMissingMarshallers, AbstractDataMarshaller<?>... marshallers) throws IOException {
        final Map<String, AbstractDataMarshaller<?>> marshallerMap = generateMarshallerMap(false, Arrays.asList(marshallers));
        marshallerMap.putAll(generateMarshallerMap(false, DEFAULT_MARSHALLERS));
        final DataNodePool dataNodePool = new DataNodePool();

        CountingDataInputStream dIn = new CountingDataInputStream(new GZIPInputStream(in));
        try {
            //check the header
            for (int i = 0; i < XDATA_HEADER.length; ++i) {
                if (dIn.readByte() != XDATA_HEADER[i]) {
                    throw new IOException("not a xdata file");
                }
            }

            final Object raw = deSerialize(dIn, dataNodePool, progressListener);
            if (!(raw instanceof ReferenceableMarshalledObject)) {
                throw new IOException("first object in xdata file MUST be a DataNode but was "
                        + raw == null ? "null" : raw.getClass().getCanonicalName());
            }

            final ReferenceableMarshalledObject referenceableObject = (ReferenceableMarshalledObject) raw;

            final Map<Long, Object> refereceableObjects = new HashMap<Long, Object>();
            final Object rawUnMarshalled = unMarshal(marshallerMap, refereceableObjects, dataNodePool,
                    ignoreMissingMarshallers, referenceableObject.dataNode);
            if (!(rawUnMarshalled instanceof DataNode)) {
                throw new IOException("first object in xdata file MUST be a DataNode but was "
                        + rawUnMarshalled == null ? "null" : rawUnMarshalled.getClass().getCanonicalName());
            }

            return (DataNode) rawUnMarshalled;
        } finally {
            dIn.close();
        }
    }

    private static Object deSerialize(CountingDataInputStream dIn, DataNodePool dataNodePool,
            ProgressListener progressListener) throws IOException {
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
                    final Object object = deSerialize(dIn, dataNodePool, DUMMY_PROGRESS_LISTENER);
                    list.add(object);
                }
                return list;
            case VAL_NODE:
                final DataNode dataNode = dataNodePool.getNew();

                final long position = dIn.getPosition();
                final int length = dIn.readInt();
                progressListener.onTotalSteps(length);

                for (int i = 0; i < length; ++i) {
                    final String key = dIn.readUTF();
                    final Object object = deSerialize(dIn, dataNodePool, DUMMY_PROGRESS_LISTENER);
                    dataNode.replaceObject(key, object);

                    progressListener.onStep();
                }
                return new ReferenceableMarshalledObject(dataNode, position);
            case VAL_REFERENCE:
                final long streamPosition = dIn.readLong();
                Reference reference = new Reference(streamPosition);
                return reference;
            default:
                throw new IOException("expected node (" + Integer.toHexString(type) + ") but found " + Integer.toHexString(type));
        }

    }

    private static Object unMarshal(Map<String, AbstractDataMarshaller<?>> marshallerMap,
            Map<Long, Object> referenceableObjects, DataNodePool dataNodePool,
            boolean ignoreMissingMarshallers, DataNode node) throws IOException {
        final Map<String, Object> replacements = new HashMap<String, Object>();

        for (Entry<String, Object> entry : node.getAll()) {
            final String key = entry.getKey();
            final Object object = entry.getValue();
            if (object != null) {
                if (object instanceof ReferenceableMarshalledObject) {
                    final ReferenceableMarshalledObject referenceableObject = (ReferenceableMarshalledObject) object;
                    Object unmarshalledObject = unMarshal(marshallerMap, referenceableObjects,
                            dataNodePool, ignoreMissingMarshallers, referenceableObject.dataNode);
                    referenceableObjects.put(referenceableObject.positionInStream, unmarshalledObject);
                    replacements.put(key, unmarshalledObject);
                } else
                    if (object instanceof Reference) {
                        Reference reference = (Reference) object;
                        Object unmarshalledObject = referenceableObjects.get(reference.positionInStream);
                        if (unmarshalledObject == null) {
                            throw new IOException("Could not find a referenced object (position " + reference.positionInStream + ")");
                        }
                        replacements.put(key, unmarshalledObject);
                    } else
                        if (object instanceof List) {
                            final List<Object> list = (List<Object>) object;
                            unMarshalList(list, marshallerMap, referenceableObjects,
                                    dataNodePool, ignoreMissingMarshallers);
                        }
            }
        }

        for (Entry<String, Object> entry : replacements.entrySet()) {
            final Object rawObject = node.getRawObject(entry.getKey());
            if (rawObject instanceof DataNode && rawObject != entry.getValue()) {
                dataNodePool.giveBack((DataNode) node.getRawObject(entry.getKey()));
            }
            node.replaceObject(entry.getKey(), entry.getValue());
        }

        if (node.containsKey(META_CLASS_NAME)) {
            final String className = node.getObject(META_CLASS_NAME);
            final DataMarshaller<Object> marshaller = (DataMarshaller<Object>) marshallerMap.get(className);
            if (marshaller == null) {
                if (!ignoreMissingMarshallers) {
                    throw new IOException("no marshaller found for class " + className);
                } else {
                    return node;
                }
            }
            return marshaller.unMarshal(node);
        } else {
            return node;
        }
    }

    private static void unMarshalList(final List<Object> list, Map<String, AbstractDataMarshaller<?>> marshallerMap,
            Map<Long, Object> referenceableObjects, DataNodePool dataNodePool,
            boolean ignoreMissingMarshallers) throws IOException {
        for (int i = 0; i < list.size(); ++i) {
            final Object aObject = list.get(i);
            if (aObject != null) {
                if (aObject instanceof ReferenceableMarshalledObject) {
                    ReferenceableMarshalledObject referenceableObject = (ReferenceableMarshalledObject) aObject;
                    final Object unmarshalledObject = unMarshal(marshallerMap, referenceableObjects, dataNodePool,
                            ignoreMissingMarshallers, referenceableObject.dataNode);
                    referenceableObjects.put(referenceableObject.positionInStream, unmarshalledObject);
                    list.set(i, unmarshalledObject);
                } else
                    if (aObject instanceof Reference) {
                        Reference reference = (Reference) aObject;
                        Object unmarshalledObject = referenceableObjects.get(reference.positionInStream);
                        if (unmarshalledObject == null) {
                            throw new IOException("Could not find a referenced object (position " + reference.positionInStream + ")");
                        }
                        list.set(i, unmarshalledObject);
                    } else
                        if (aObject instanceof List) {
                            final List<Object> aList = (List<Object>) aObject;
                            unMarshalList(aList, marshallerMap, referenceableObjects, dataNodePool, ignoreMissingMarshallers);
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param node
     * @param file
     * @param progressListener
     * @param marshallers
     * @throws IOException
     */
    public static void store(DataNode node, File file, ProgressListener progressListener, DataMarshaller<?>... marshallers) throws IOException {
        store(node, new FileOutputStream(file), progressListener, marshallers);
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param node
     * @param out
     * @param marshallers
     * @throws IOException
     */
    public static void store(DataNode node, OutputStream out, DataMarshaller<?>... marshallers) throws IOException {
        store(node, out, DUMMY_PROGRESS_LISTENER, marshallers);
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
     * Also take a look at {@link com.moebiusgames.xdata.marshaller}. There are a bunch of
     * standard marshallers that ARE INCLUDED by default. So you don't need to add them here
     * to work.
     *
     * @param node
     * @param out
     * @param progressListener
     * @param marshallers
     * @throws IOException
     */
    public static void store(DataNode node, OutputStream out, ProgressListener progressListener, AbstractDataMarshaller<?>... marshallers) throws IOException {
        final Map<String, AbstractDataMarshaller<?>> marshallerMap = generateMarshallerMap(true, Arrays.asList(marshallers));
        marshallerMap.putAll(generateMarshallerMap(true, DEFAULT_MARSHALLERS));

        DataOutputStream dOut = new DataOutputStream(new GZIPOutputStream(out));
        try {
            //write header
            dOut.write(XDATA_HEADER);

            //serialize the node
            serialize(marshallerMap, dOut, node, progressListener);
        } finally {
            dOut.close();
        }
    }

    /**
     * wraps an object using the data marshaller for that given object
     *
     * @param marshallerMap
     * @param object
     * @return
     */
    private static DataNode marshalObject(Map<String, AbstractDataMarshaller<?>> marshallerMap, Object object) {
        //can't be null here because it is not resolved: meaning, it is an instance of an unknown class
        final Class<?> clazz = object.getClass();

        final DataMarshaller<Object> serializer = (DataMarshaller<Object>) marshallerMap.get(clazz.getCanonicalName());
        if (serializer == null) {
            throw new IllegalStateException("No serializer defined for class " + clazz.getCanonicalName());
        }
        final DataNode node = serializer.marshal(object);
        node.setObject(META_CLASS_NAME, serializer.getDataClassName());
        return node;
    }

    /**
     * processes one element and either serializes it directly to the stream, or pushes
     * a corresponding frame to the stack.
     *
     * @param element
     * @param stack
     * @param dOut
     * @param marshallerMap
     * @param serializedObjects
     * @param testSerializedObject
     * @return true if a new element has been pushed to the stack, false otherwise
     * @throws IOException
     */
    private static boolean processElement(Object element, Deque<SerializationFrame> stack, DataOutputStream dOut,
            Map<String, AbstractDataMarshaller<?>> marshallerMap,
            Map<SerializedObject, SerializedObject> serializedObjects,
            SerializedObject testSerializedObject) throws IOException {
        if (element instanceof List) {
            stack.push(new ListFrame(element, (List<?>) element));
            return true;
        } else
            if (element == null || isPrimitiveOrString(element)) {
                serializePrimitive(dOut, element);
            } else {
                //unmarshalled object or data node
                testSerializedObject.object = element;
                if (serializedObjects.containsKey(testSerializedObject)) {
                    final SerializedObject serializedObject = serializedObjects.get(testSerializedObject);
                    serializeReference(serializedObject, dOut);
                } else {
                    DataNodeFrame dataNodeFrame;
                    if (element instanceof DataNode) {
                        dataNodeFrame = new DataNodeFrame(element, (DataNode) element);
                    } else {
                        dataNodeFrame = new DataNodeFrame(element, marshalObject(marshallerMap, element));
                    }
                    stack.push(dataNodeFrame);
                    return true;
                }
            }
        return false;
    }

    /**
     * actually serializes the data
     *
     * @param marshallerMap
     * @param dOut
     * @param primaryNode
     * @param progressListener
     * @throws IOException
     */
    private static void serialize(Map<String, AbstractDataMarshaller<?>> marshallerMap,
            DataOutputStream dOut, DataNode primaryNode, ProgressListener progressListener) throws IOException {

        //a map containing all serialized objects. This is used
        //to make sure that we store each object only once.
        final Map<SerializedObject, SerializedObject> serializedObjects = new HashMap<SerializedObject, SerializedObject>();

        final Deque<SerializationFrame> stack = new LinkedList<SerializationFrame>();
        final DataNodeFrame primaryDataNodeFrame = new DataNodeFrame(null, primaryNode);
        
        progressListener.onTotalSteps(primaryDataNodeFrame.entries.size());
        stack.add(primaryDataNodeFrame);


        final SerializedObject testSerializedObject = new SerializedObject();

        while (!stack.isEmpty()) {
            final SerializationFrame frame = stack.peek();

            //writes the header for that frame (if needed)
            frame.writeHeader(dOut);

            if (frame.hasNext()) {
                while (frame.hasNext()) {
                    if (frame.next(stack, dOut, marshallerMap,
                            serializedObjects, testSerializedObject)) {
                        break;
                    }
                }

                if (frame == primaryDataNodeFrame) {
                    progressListener.onStep();
                }
            } else {
                stack.pop();

                //remember serialized object's addresses
                if (frame instanceof DataNodeFrame) {
                    DataNodeFrame dataNodeFrame = (DataNodeFrame) frame;
                    SerializedObject newSerializedObject = new SerializedObject();
                    newSerializedObject.object = frame.object;
                    newSerializedObject.positionInStream = dataNodeFrame.positionInStream;
                    serializedObjects.put(newSerializedObject, newSerializedObject);
                }
            }
        }
    }

    private static void serializeReference(SerializedObject ref, DataOutputStream dOut) throws IOException {
        dOut.writeByte(VAL_REFERENCE);
        dOut.writeLong(ref.positionInStream);
    }

    /**
     * serializes a primitive or null
     *
     * @param serializerMap
     * @param dOut
     * @param primitive
     * @throws IOException
     */
    private static void serializePrimitive(DataOutputStream dOut, Object primitive) throws IOException {
        if (primitive == null) {
            dOut.writeByte(VAL_NULL);
        } else {
            dOut.writeByte(VAL_ELEMENT);
            final Class<?> resolvedObjectClass = primitive.getClass();
            Serializer<Object> serializer = (Serializer<Object>) PRIMITIVE_SERIALIZERS_BY_CLASS.get(resolvedObjectClass);
            if (serializer == null) {
                throw new IllegalStateException("Can't serialize resolved class " + resolvedObjectClass.getCanonicalName());
            }
            dOut.writeByte(serializer.getSerializerId());
            serializer.serialize(primitive, dOut);
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

    /**
     * checks if the given object is not null and has a primitive
     * serializer.
     *
     * @param object
     * @return
     */
    private static boolean isPrimitiveOrString(Object object) {
        return object != null && PRIMITIVE_SERIALIZERS_BY_CLASS.containsKey(object.getClass());
    }

    private static boolean isPrimitiveOrStringOrList(Object object) {
        return object != null && (PRIMITIVE_SERIALIZERS_BY_CLASS.containsKey(object.getClass()) || object instanceof List);
    }

    /**
     * checks if this is an object other than a primitive type, a list or a data node
     * @param object
     * @return
     */
    private static boolean isObject(Object object) {
        return object != null && !isPrimitiveOrStringOrList(object);// && !(object instanceof DataNode);
    }

    private static Map<String, AbstractDataMarshaller<?>> generateMarshallerMap(boolean fullyQualifiedClassName,
            List<? extends AbstractDataMarshaller<?>> marshallers) {
        final Map<String, AbstractDataMarshaller<?>> map = new HashMap<String, AbstractDataMarshaller<?>>();
        for (AbstractDataMarshaller<?> marshaller : marshallers) {
            Class<?> marshallerDataClass;
            if (marshaller instanceof GenericDataMarshaller) {
                marshallerDataClass = ((GenericDataMarshaller) marshaller).getDataGenericType().getRawType();
            } else
                if (marshaller instanceof DataMarshaller) {
                    marshallerDataClass = ((DataMarshaller) marshaller).getDataClass();
                } else {
                    throw new IllegalArgumentException("You are not to use AbstractDataMarshaller directly, please use one of its descendents!");
                }
            final String key = fullyQualifiedClassName ? marshallerDataClass.getCanonicalName() : marshaller.getDataClassName();
            map.put(key, marshaller);
            map.putAll(generateMarshallerMap(fullyQualifiedClassName, marshaller.getRequiredMarshallers()));
        }
        return map;
    }

    // ####################################
    // ## DeSerialization Helper Classes ##
    // ####################################


    private static class ReferenceableMarshalledObject {
        private final DataNode dataNode;
        private final long positionInStream;

        public ReferenceableMarshalledObject(DataNode dataNode, long positionInStream) {
            this.dataNode = dataNode;
            this.positionInStream = positionInStream;
        }
    }

    private static class Reference {
        private final long positionInStream;

        public Reference(long positionInStream) {
            this.positionInStream = positionInStream;
        }

    }

    // ##################################
    // ## Serialization Helper Classes ##
    // ##################################

    private static class SerializedObject {
        private Object object;
        private long positionInStream;

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + (this.object != null ? this.object.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SerializedObject other = (SerializedObject) obj;
            return this.object == other.object;
        }
    }

    private static abstract class SerializationFrame {
        private final Object object;
        private boolean headerWritten = false;

        public SerializationFrame(Object object) {
            this.object = object;
        }

        public void writeHeader(DataOutputStream dOut) throws IOException {
            if (!headerWritten) {
                this.doWriteHeader(dOut);
                this.headerWritten = true;
            }
        }

        public abstract void doWriteHeader(DataOutputStream dOut) throws IOException;

        public abstract boolean hasNext();

        public abstract boolean next(Deque<SerializationFrame> stack, DataOutputStream dOut, Map<String,
                AbstractDataMarshaller<?>> marshallerMap,
                Map<SerializedObject, SerializedObject> serializedObjects,
                SerializedObject testSerializedObject) throws IOException;
    }

    private static class ListFrame extends SerializationFrame {
        private final List<Object> entries = new LinkedList<Object>();

        public ListFrame(Object object, List<?> list) {
            super(object);
            this.entries.addAll(list);
        }

        @Override
        public void doWriteHeader(DataOutputStream dOut) throws IOException {
                dOut.writeByte(VAL_LIST);
                dOut.writeInt(this.entries.size());
        }

        @Override
        public boolean hasNext() {
            return !this.entries.isEmpty();
        }

        @Override
        public boolean next(Deque<SerializationFrame> stack, DataOutputStream dOut,
                Map<String, AbstractDataMarshaller<?>> marshallerMap,
                Map<SerializedObject, SerializedObject> serializedObjects,
                SerializedObject testSerializedObject) throws IOException {
            final Object element = this.entries.remove(0);

            return processElement(element, stack, dOut, marshallerMap,
                    serializedObjects, testSerializedObject);
        }

    }

    private static class DataNodeFrame extends SerializationFrame {
        private final DataNode dataNode;
        private long positionInStream;
        private final Queue<Entry<String, Object>> entries = new LinkedList<Entry<String, Object>>();

        public DataNodeFrame(Object object, DataNode dataNode) {
            super(object);
            this.dataNode = dataNode;
            this.entries.addAll(dataNode.getAll());
        }

        @Override
        public void doWriteHeader(DataOutputStream dOut) throws IOException {
            this.positionInStream = dOut.size();
            dOut.writeByte(VAL_NODE);
            dOut.writeInt(dataNode.getSize());
        }

        @Override
        public boolean hasNext() {
            return !entries.isEmpty();
        }

        @Override
        public boolean next(Deque<SerializationFrame> stack, DataOutputStream dOut,
                Map<String, AbstractDataMarshaller<?>> marshallerMap,
                Map<SerializedObject, SerializedObject> serializedObjects,
                SerializedObject testSerializedObject) throws IOException {
            final Entry<String, Object> entry = entries.poll();

            //write the field's key
            dOut.writeUTF(entry.getKey());

            return processElement(entry.getValue(), stack, dOut, marshallerMap,
                    serializedObjects, testSerializedObject);
        }

    }

    // #########################
    // ## Specialized Streams ##
    // #########################

    private static class CountingDataInputStream extends DataInputStream {

        private final CountingInputStream countingInputStream;

        public CountingDataInputStream(InputStream in) {
            super(new CountingInputStream(in));
            this.countingInputStream = (CountingInputStream) this.in;
        }

        /**
         * returns the current position of the stream
         * @return
         */
        public long getPosition() {
            return this.countingInputStream.getPosition();
        }

        @Override
        public int read() throws IOException {
            return super.read();
        }

    }

    private static class CountingInputStream extends InputStream {

        private final InputStream in;
        private long position = -1;

        public CountingInputStream(InputStream in) {
            this.in = in;
        }

        public long getPosition() {
            return position;
        }

        @Override
        public int read() throws IOException {
            int read = in.read();
            position++;
            return read;
        }
    }

    // ##########################################
    // ## Serializers for primitive data types ##
    // ##########################################

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

    private static class DataNodePool {
        private final List<DataNode> dataNodes = new ArrayList<DataNode>();

        public synchronized DataNode getNew() {
            if (dataNodes.isEmpty()) {
                return new DataNode();
            } else {
                return dataNodes.remove(dataNodes.size() - 1);
            }
        }

        public synchronized void giveBack(DataNode dataNode) {
            dataNode.clear();
            this.dataNodes.add(dataNode);
        }

    }
}
