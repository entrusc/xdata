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
  //...
  DataNode node = new DataNode();
  node.setObject(MY_KEY, "hello world");
  XData.store(node, new File("somefile.xdata"));
  //...
  DataNode restoredNode = XData.load(new File("somefile.xdata"));
  //do sth with the data in the node e.g.
  System.out.println(node.getDeSerializedObject(MY_KEY));
 </pre>
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

    private XData() {
    }

    /**
     * loads a xdata file from disk using the given marshallers. For all classes other
     * than these a special marshaller is required to map the class' data to a data node
     * deSerializedObject:
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
     * deSerializedObject:
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
     * deSerializedObject:
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
     * deSerializedObject:
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
     * deSerializedObject:
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
     * deSerializedObject:
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
     * deSerializedObject:
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
     * deSerializedObject:
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

        CountingDataInputStream dIn = new CountingDataInputStream(new GZIPInputStream(in));
        try {
            //check the header
            for (int i = 0; i < XDATA_HEADER.length; ++i) {
                if (dIn.readByte() != XDATA_HEADER[i]) {
                    throw new IOException("not a xdata file");
                }
            }

            final DataNode firstDataNode = deSerialize(dIn, marshallerMap,
                    ignoreMissingMarshallers, progressListener);

            return firstDataNode;
        } finally {
            dIn.close();
        }
    }

    private static DataNode deSerialize(CountingDataInputStream dIn,
            Map<String, AbstractDataMarshaller<?>> marshallerMap,
            boolean ignoreMissingMarshallers,
            ProgressListener progressListener) throws IOException {

        final Deque<DeSerializerFrame> stack = new LinkedList<DeSerializerFrame>();
        final Map<Long, Object> referenceableObjectMap = new HashMap<Long, Object>();

        //first object needs to be a DataNode
        final Object firstObject = deSerializeElement(stack, dIn, referenceableObjectMap);
        if (firstObject == null || !(firstObject instanceof DataNodeDeSerializerFrame)) {
            throw new IOException("First data structure in a xdata file needs to be a DataNode");
        }
        DataNodeDeSerializerFrame firstDataNode = (DataNodeDeSerializerFrame) firstObject;
        progressListener.onTotalSteps(firstDataNode.size);
        stack.push(firstDataNode);

        while (!stack.isEmpty()) {
            final DeSerializerFrame frame = stack.peek();

            if (frame.hasNext()) {
                while (frame.hasNext()) {
                    if (frame.next(stack, dIn, referenceableObjectMap, marshallerMap, ignoreMissingMarshallers)) {
                        if (frame == firstDataNode) {
                            progressListener.onStep();
                        }
                        break; //stack changed so jump out
                    }
                    if (frame == firstDataNode) {
                        progressListener.onStep();
                    }
                }
            } else {
                frame.unMarshal(stack, dIn, referenceableObjectMap, marshallerMap, ignoreMissingMarshallers);
                stack.pop();
            }
        }

        final Object firstDeSerializedObject = firstDataNode.getDeSerializedObject();
        if (!(firstDeSerializedObject instanceof DataNode)) {
            throw new IOException("first object in xdata file MUST be a DataNode but was "
                        + firstDeSerializedObject.getClass().getCanonicalName());
        }

        return (DataNode) firstDeSerializedObject;

    }

    private static Object deSerializeElement(Deque<DeSerializerFrame> stack,
            CountingDataInputStream dIn,
            Map<Long, Object> referenceableObjectMap) throws IOException {
        final long positionInStream = dIn.getPosition();
        final int id = dIn.readByte();
        int length;
        switch (id) {
            case VAL_NULL:
                return null;
            case VAL_ELEMENT:
                return deSerializePrimitive(dIn);
            case VAL_NODE:
                length = dIn.readInt();
                final DataNodeDeSerializerFrame dataNodeDeSerializerFrame =
                        new DataNodeDeSerializerFrame(length, positionInStream);
                stack.push(dataNodeDeSerializerFrame);
                return dataNodeDeSerializerFrame;
            case VAL_LIST:
                length = dIn.readInt();
                final ListDeSerializerFrame listDeSerializerFrame = new ListDeSerializerFrame(length);
                stack.push(listDeSerializerFrame);
                return listDeSerializerFrame;
            case VAL_REFERENCE:
                return deSerializeReference(dIn, referenceableObjectMap);
            default:
                throw new IOException("Unknown value code " + String.format("%02x", id));
        }
    }

    private static Object deSerializePrimitive(CountingDataInputStream dIn) throws IOException {
        final byte elementType = dIn.readByte();
        final Serializer<Object> serializer = (Serializer<Object>)PRIMITIVE_SERIALIZERS_BY_ID.get(elementType);
        if (serializer == null) {
            throw new IOException("can't deserialize type " + Integer.toHexString(elementType) + " (maybe newer format?).");
        }
        return serializer.deserialize(dIn);
    }

    private static Object deSerializeReference(CountingDataInputStream dIn,
            Map<Long, Object> referenceableObjectMap) throws IOException {
        final long refPosition = dIn.readLong();
        final Object referenceableObject = referenceableObjectMap.get(refPosition);
        if (referenceableObject == null) {
            throw new IOException("Reference to position " + refPosition + " points to non existing object.");
        }
        return referenceableObject;
    }

    /**
     * stores a datanode in a xdata file using the given marshallers. For all classes other
     * than these a special marshaller is required to map the class' data to a data node
     * deSerializedObject:
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
     * deSerializedObject:
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
     * deSerializedObject:
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
     * deSerializedObject:
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
    public static void store(DataNode node, OutputStream out, ProgressListener progressListener,
            AbstractDataMarshaller<?>... marshallers) throws IOException {
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
     * wraps an deSerializedObject using the data marshaller for that given deSerializedObject
     *
     * @param marshallerMap
     * @param object
     * @return
     */
    private static DataNode marshalObject(Map<String, AbstractDataMarshaller<?>> marshallerMap, Object object) {
        //can't be null here because it is not resolved: meaning, it is an instance of an unknown class
        final Class<?> clazz = object.getClass();

        final AbstractDataMarshaller<Object> serializer = (AbstractDataMarshaller<Object>) marshallerMap.get(clazz.getCanonicalName());
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
    private static boolean serializeElement(Object element, Deque<SerializationFrame> stack, DataOutputStream dOut,
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
                //unmarshalled deSerializedObject or data node
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
        //to make sure that we store each deSerializedObject only once.
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
                        if (frame == primaryDataNodeFrame) {
                            progressListener.onStep();
                        }
                        break;
                    }
                    if (frame == primaryDataNodeFrame) {
                        progressListener.onStep();
                    }
                }
            } else {
                stack.pop();

                //remember serialized deSerializedObject's addresses
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
     * checks if the given deSerializedObject is not null and has a primitive
     * serializer.
     *
     * @param object
     * @return
     */
    private static boolean isPrimitiveOrString(Object object) {
        return object != null && PRIMITIVE_SERIALIZERS_BY_CLASS.containsKey(object.getClass());
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

    private static abstract class DeSerializerFrame {

        protected Object deSerializedObject;

        public Object getDeSerializedObject() {
            return deSerializedObject;
        }

        public abstract boolean hasNext();

        public abstract boolean next(Deque<DeSerializerFrame> stack,
            CountingDataInputStream dIn,
            Map<Long, Object> referenceableObjectMap,
            Map<String, AbstractDataMarshaller<?>> marshallerMap,
            boolean ignoreMissingMarshallers) throws IOException;

        public abstract void unMarshal(Deque<DeSerializerFrame> stack,
            CountingDataInputStream dIn,
            Map<Long, Object> referenceableObjectMap,
            Map<String, AbstractDataMarshaller<?>> marshallerMap,
            boolean ignoreMissingMarshallers) throws IOException;
    }

    private static class DataNodeDeSerializerFrame extends DeSerializerFrame {

        private final int size;
        private final long positionInStream;
        private int position = 0;
        private final DataNode dataNode = DataNodePool.get().getNew();

        private String currentKey = null;
        private DeSerializerFrame resultFrame = null;

        public DataNodeDeSerializerFrame(int size, long positionInStream) {
            this.size = size;
            this.positionInStream = positionInStream;
        }

        @Override
        public boolean hasNext() {
            return position < size;
        }

        @Override
        public boolean next(Deque<DeSerializerFrame> stack,
            CountingDataInputStream dIn,
            Map<Long, Object> referenceableObjectMap,
            Map<String, AbstractDataMarshaller<?>> marshallerMap,
            boolean ignoreMissingMarshallers) throws IOException {

            if (resultFrame == null) {
                currentKey = dIn.readUTF();

                Object object = deSerializeElement(stack, dIn, referenceableObjectMap);
                if (object instanceof DeSerializerFrame) {
                    resultFrame = (DeSerializerFrame) object;
                    return true; //signal that the stack has been updated
                } else {
                    dataNode.replaceObject(currentKey, object);
                }
            } else {
                final Object object = resultFrame.getDeSerializedObject();
                dataNode.replaceObject(currentKey, object);
                resultFrame = null;
            }
            position++;
            return false;
        }

        @Override
        public void unMarshal(Deque<DeSerializerFrame> stack,
            CountingDataInputStream dIn,
            Map<Long, Object> referenceableObjectMap,
            Map<String, AbstractDataMarshaller<?>> marshallerMap,
            boolean ignoreMissingMarshallers) throws IOException {
            if (dataNode.containsKey(META_CLASS_NAME)) {
                final String className = dataNode.getObject(META_CLASS_NAME);
                final AbstractDataMarshaller<Object> marshaller = (AbstractDataMarshaller<Object>) marshallerMap.get(className);
                if (marshaller == null) {
                    if (!ignoreMissingMarshallers) {
                        throw new IOException("no marshaller found for class " + className);
                    } else {
                        this.deSerializedObject = dataNode;
                    }
                } else {
                    this.deSerializedObject = marshaller.unMarshal(dataNode);
                    DataNodePool.get().giveBack(dataNode);
                }
            } else {
                this.deSerializedObject = dataNode;
            }

            referenceableObjectMap.put(positionInStream, deSerializedObject);
        }

    }

    private static class ListDeSerializerFrame extends DeSerializerFrame {

        private final List<Object> list = new ArrayList<Object>();

        private final int size;
        private int position = 0;

        private DeSerializerFrame resultFrame = null;

        public ListDeSerializerFrame(int size) {
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return position < size;
        }

        @Override
        public boolean next(Deque<DeSerializerFrame> stack, CountingDataInputStream dIn,
                Map<Long, Object> referenceableObjectMap,
                Map<String, AbstractDataMarshaller<?>> marshallerMap,
                boolean ignoreMissingMarshallers) throws IOException {

            if (resultFrame == null) {
                final Object object = deSerializeElement(stack, dIn, referenceableObjectMap);
                if (object instanceof DeSerializerFrame) {
                    resultFrame = (DeSerializerFrame) object;
                    return true; //signal that the stack has been updated
                } else {
                    list.add(object);
                }
            } else {
                list.add(resultFrame.getDeSerializedObject());
                resultFrame = null;
            }
            position++;
            return false;
        }

        @Override
        public void unMarshal(Deque<DeSerializerFrame> stack, CountingDataInputStream dIn,
                Map<Long, Object> referenceableObjectMap,
                Map<String, AbstractDataMarshaller<?>> marshallerMap,
                boolean ignoreMissingMarshallers) throws IOException {
            this.deSerializedObject = list;
        }

    }


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

            return serializeElement(element, stack, dOut, marshallerMap,
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

            return serializeElement(entry.getValue(), stack, dOut, marshallerMap,
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
        private long position = 0;

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

    private static interface Serializer<T> {
        byte getSerializerId();

        Class<T> getClazz();

        void serialize(T object, DataOutputStream dOut) throws IOException;

        T deserialize(DataInputStream dIn) throws IOException;
    }

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
        private final static ThreadLocal<DataNodePool> DATA_NODE_POOLS = new ThreadLocal<DataNodePool>() {
            @Override
            protected DataNodePool initialValue() {
                return new DataNodePool();
            }
        };

        private final Deque<DataNode> dataNodes = new LinkedList<DataNode>();

        public synchronized DataNode getNew() {
            if (dataNodes.isEmpty()) {
                return new DataNode();
            } else {
                return dataNodes.pop();
            }
        }

        public synchronized void giveBack(DataNode dataNode) {
            dataNode.clear();
            this.dataNodes.push(dataNode);
        }

        public static DataNodePool get() {
            return DATA_NODE_POOLS.get();
        }
    }
}
