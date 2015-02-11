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
import com.moebiusgames.xdata.type.GenericType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Some tests
 */
public class XDataTest {

    private static final DataKey<Boolean> KEY_BOOL = DataKey.create("bool", Boolean.class);
    private static final DataKey<Byte> KEY_BYTE = DataKey.create("byte", Byte.class);
    private static final DataKey<Character> KEY_CHAR = DataKey.create("char", Character.class);
    private static final DataKey<Short> KEY_SHORT = DataKey.create("short", Short.class);
    private static final DataKey<Integer> KEY_INT = DataKey.create("int", Integer.class);
    private static final DataKey<Long> KEY_LONG = DataKey.create("long", Long.class);
    private static final DataKey<Float> KEY_FLOAT = DataKey.create("float", Float.class);
    private static final DataKey<Double> KEY_DOUBLE = DataKey.create("double", Double.class);
    private static final DataKey<String> KEY_STRING = DataKey.create("string", String.class);
    private static final DataKey<Date> KEY_DATE = DataKey.create("date", Date.class);

    private static final ListDataKey<String> KEY_STRING_LIST = ListDataKey.create("string_list", String.class);

    private static final DataKey<DataNode> KEY_CAR_INFO = DataKey.create("car_info", DataNode.class);
    private static final DataKey<Car> KEY_CAR = DataKey.create("car", Car.class);
    private static final DataKey<Car> KEY_CAR_A = DataKey.create("car a", Car.class);
    private static final DataKey<Car> KEY_CAR_B = DataKey.create("car b", Car.class);
    private static final DataKey<Car> KEY_CAR_C = DataKey.create("car c", Car.class);
    private static final ListDataKey<Car> KEY_CAR_LIST = ListDataKey.create("car", Car.class);

    private static final ListDataKey<Object> KEY_OBJECT_LIST = ListDataKey.create("objects", Object.class);
    private static final ListDataKey<List<Car>> KEY_CAR_META_LIST = ListDataKey.create("carsofcars", new GenericType<List<Car>>() {});

    private static final DataKey<String> KEY_STRING_NOT_NULL = DataKey.create("stringnn", String.class, false);
    private static final DataKey<String> KEY_STRING_DEFAULT = DataKey.create("stringdef", String.class, "fasel");

    private static final ListDataKey<String> KEY_STRING_LIST_NOT_NULL = ListDataKey.create("stringlist", String.class, false);
    /**
     * A simple store and retrieve test (no marshalling)
     * @throws java.io.IOException
     */
    @Test
    public void simpleTest() throws IOException {
        File tmpFile = File.createTempFile("xdata_test1", ".xdata");
        tmpFile.deleteOnExit();

        DataNode dataNode = new DataNode();
        dataNode.setObject(KEY_BOOL, true);
        dataNode.setObject(KEY_STRING, "blafasel");
        dataNode.setObject(KEY_BYTE, (byte) 0x05);
        dataNode.setObject(KEY_CHAR, 'ö');
        dataNode.setObject(KEY_SHORT, (short) 13);
        dataNode.setObject(KEY_INT, 67567);
        dataNode.setObject(KEY_LONG, 786783647846876879L);
        dataNode.setObject(KEY_FLOAT, 42.24f);
        dataNode.setObject(KEY_DOUBLE, Math.PI);
        dataNode.setObjectList(KEY_STRING_LIST, Arrays.asList(new String[] { "abc", "def", "ghi" }));

        XData.store(dataNode, tmpFile);

        DataNode restoredNode = XData.load(tmpFile);
        assertEquals(Boolean.TRUE, restoredNode.getObject(KEY_BOOL));
        assertEquals("blafasel", restoredNode.getObject(KEY_STRING));
        assertEquals((byte) 0x05, (byte) restoredNode.getObject(KEY_BYTE));
        assertEquals('ö', (char) restoredNode.getObject(KEY_CHAR));
        assertEquals((short) 13, (short) restoredNode.getObject(KEY_SHORT));
        assertEquals(67567, (int) restoredNode.getObject(KEY_INT));
        assertEquals(786783647846876879L, (long) restoredNode.getObject(KEY_LONG));
        assertEquals(42.24f, (float) restoredNode.getObject(KEY_FLOAT), 0.0001f);
        assertEquals(Math.PI, (double) restoredNode.getObject(KEY_DOUBLE), 0.0001f);
        assertEquals(Arrays.asList(new String[] { "abc", "def", "ghi" }), restoredNode.getObjectList(KEY_STRING_LIST));
    }

    @Test
    public void advancedTest() throws IOException {
        Date now = new Date(); //now

        File tmpFile = File.createTempFile("xdata_test2", ".xdata");
        tmpFile.deleteOnExit();

        DataNode node = new DataNode();
        node.setObject(KEY_DATE, now);

        XData.store(node, tmpFile, new DateMarshaller());

        DataNode restoredNode = XData.load(tmpFile, new DateMarshaller());
        assertEquals(now, restoredNode.getObject(KEY_DATE));
    }

    @Test
    public void customMarshallerTest() throws IOException {
        File tmpFile = File.createTempFile("xdata_test3", ".xdata");
        tmpFile.deleteOnExit();

        Car car = new Car(4, 180.5f, new Date());
        DataNode node = new DataNode();
        DataNode subNode = new DataNode();
        node.setObject(KEY_CAR_INFO, subNode);
        node.setObject(KEY_STRING, "some car info");

        subNode.setObject(KEY_CAR, car);


        XData.store(node, tmpFile, new CarMarshaller());

        DataNode restoredNode = XData.load(tmpFile, new CarMarshaller());
        assertTrue(restoredNode.containsKey(KEY_CAR_INFO));
        assertTrue(restoredNode.containsKey(KEY_STRING));
        DataNode restoredSubNode = restoredNode.getObject(KEY_CAR_INFO);
        assertTrue(restoredSubNode.containsKey(KEY_CAR));
        assertEquals("some car info", restoredNode.getObject(KEY_STRING));
        assertEquals(car, restoredSubNode.getObject(KEY_CAR));
    }

    @Test
    public void customMarshallerListTest() throws IOException {
        File tmpFile = File.createTempFile("xdata_test3", ".xdata");
        tmpFile.deleteOnExit();

        Random random = new Random();
        List<Car> cars = new ArrayList<Car>();
        for (int i = 0; i < 100; ++i) {
            final Car car = new Car(random.nextInt(5), random.nextFloat() * 400f, new Date());
            cars.add(random.nextBoolean() ? car : null);
        }

        DataNode node = new DataNode();
        DataNode subNode = new DataNode();
        node.setObject(KEY_CAR_INFO, subNode);
        node.setObject(KEY_STRING, "some car info");

        subNode.setObjectList(KEY_CAR_LIST, cars);

        XData.store(node, tmpFile, new CarMarshaller());

        DataNode restoredNode = XData.load(tmpFile, new CarMarshaller());

        assertTrue(restoredNode.containsKey(KEY_CAR_INFO));
        assertTrue(restoredNode.containsKey(KEY_STRING));
        DataNode restoredSubNode = restoredNode.getObject(KEY_CAR_INFO);
        assertTrue(restoredSubNode.containsKey(KEY_CAR_LIST));
        assertEquals("some car info", restoredNode.getObject(KEY_STRING));
        assertEquals(cars, restoredSubNode.getObjectList(KEY_CAR_LIST));

    }

    @Test
    public void customMarshallerListTypeTest() throws IOException {
        File tmpFile = File.createTempFile("xdata_test5", ".xdata");
        tmpFile.deleteOnExit();

        Car car = new Car(4, 180.5f, new Date());
        Jet jet = new Jet(100f);
        DataNode node = new DataNode();

        List<Object> objects = Arrays.asList(new Object[] { car, jet });

        node.setObjectList(KEY_OBJECT_LIST, objects);

        XData.store(node, tmpFile, new CarMarshaller(), new JetMarshaller());
        DataNode restoredNode = XData.load(tmpFile, new CarMarshaller(), new JetMarshaller());

        assertEquals(objects, restoredNode.getObjectList(KEY_OBJECT_LIST));
    }

    @Test
    public void customMarshallerListOfListsTest() throws IOException {
        File tmpFile = File.createTempFile("xdata_test6", ".xdata");
        tmpFile.deleteOnExit();

        Car car = new Car(4, 180.5f, new Date());
        List<Date> checkDates = new ArrayList<Date>();
        checkDates.add(new Date());
        checkDates.add(new Date(578987L));
        car.setCheckDates(checkDates);

        DataNode node = new DataNode();

        List<List<Car>> carMetaList = new ArrayList<List<Car>>();
        List<Car> carList = new ArrayList<Car>();
        carList.add(car);
        carMetaList.add(carList);

        node.setObjectList(KEY_CAR_META_LIST, carMetaList);

        XData.store(node, tmpFile, new CarMarshaller());
        DataNode restoredNode = XData.load(tmpFile, new CarMarshaller(), new JetMarshaller());

        assertEquals(restoredNode.getObjectList(KEY_CAR_META_LIST), carMetaList);
    }

    @Test(expected=IllegalArgumentException.class)
    public void keyTest1() throws IOException {
        DataNode node = new DataNode();

        node.setObject(KEY_STRING_NOT_NULL, null);
    }

    @Test
    public void keyTest2() throws IOException {
        File tmpFile = File.createTempFile("xdata_test_key1", ".xdata");
        tmpFile.deleteOnExit();

        DataNode node = new DataNode();

        XData.store(node, tmpFile);
        DataNode restoredNode = XData.load(tmpFile);

        assertEquals(KEY_STRING_DEFAULT.getDefaultValue(), restoredNode.getObject(KEY_STRING_DEFAULT));
    }

    @Test(expected=IllegalStateException.class)
    public void keyTest3() throws IOException {
        File tmpFile = File.createTempFile("xdata_test_key2", ".xdata");
        tmpFile.deleteOnExit();

        DataNode node = new DataNode();

        XData.store(node, tmpFile);
        DataNode restoredNode = XData.load(tmpFile, new CarMarshaller(), new JetMarshaller());

        restoredNode.getMandatoryObject(KEY_STRING_DEFAULT);
    }

    @Test()
    public void keyTest4() throws IOException {
        File tmpFile = File.createTempFile("xdata_test_key3", ".xdata");
        tmpFile.deleteOnExit();

        DataNode node = new DataNode();

        XData.store(node, tmpFile);
        DataNode restoredNode = XData.load(tmpFile);

        final List<String> list = restoredNode.getObjectList(KEY_STRING_LIST_NOT_NULL);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void objectPersistenceTest() throws IOException {
        File tmpFile = File.createTempFile("xdata_test_persistence1", ".xdata");
        tmpFile.deleteOnExit();

        Car car = new Car(4, 180.5f, new Date());
        DataNode node = new DataNode();
        DataNode subNode = new DataNode();
        node.setObject(KEY_CAR_INFO, subNode);
        node.setObject(KEY_STRING, "some car info");

        subNode.setObject(KEY_CAR, car);

        DataNode nodeCopy = node.copy();

        XData.store(node, tmpFile, new CarMarshaller());

        assertEquals(nodeCopy, node);
    }

    @Test
    public void testReference() throws IOException {
        File tmpFile = File.createTempFile("xdata_test_reference", ".xdata");
        tmpFile.deleteOnExit();

        DataNode node = new DataNode();
        Car car = new Car(4, 180.5f, new Date());
        node.setObject(KEY_CAR_A, car);
        node.setObject(KEY_CAR_B, car);
        node.setObject(KEY_CAR_C, car);

        XData.store(node, tmpFile, new CarMarshaller());
        DataNode result = XData.load(tmpFile, new CarMarshaller());

        assertNotNull(result.getObject(KEY_CAR_A));
        assertNotNull(result.getObject(KEY_CAR_B));
        assertNotNull(result.getObject(KEY_CAR_C));

        assertEquals(car, result.getObject(KEY_CAR_A));
        assertEquals(car, result.getObject(KEY_CAR_B));
        assertEquals(car, result.getObject(KEY_CAR_C));
    }
}
