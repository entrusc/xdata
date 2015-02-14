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
import com.moebiusgames.xdata.ListDataKey;
import com.moebiusgames.xdata.marshaller.DateMarshaller;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A sample marshaller to marshal a Car object
 *
 * @author Florian Frankenberger
 */
public class CarMarshaller implements DataMarshaller<Car> {

    private static final DataKey<Integer> KEY_WHEELS = DataKey.create("wheels", Integer.class);
    private static final DataKey<Float> KEY_HORSE_POWER = DataKey.create("horse_power", Float.class);
    private static final DataKey<Date> KEY_BUILD_DATE = DataKey.create("build_date", Date.class);
    private static final ListDataKey<Date> KEY_CHECK_DATES = ListDataKey.create("check_dates", Date.class);

    @Override
    public String getDataClassName() {
        return "xdata.test.car";
    }

    @Override
    public Class<Car> getDataClass() {
        return Car.class;
    }

    @Override
    public DataNode marshal(Car object) {
        DataNode node = new DataNode();
        node.setObject(KEY_WHEELS, object.getWheels());
        node.setObject(KEY_HORSE_POWER, object.getHorsePower());
        node.setObject(KEY_BUILD_DATE, object.getBuildDate());
        node.setObjectList(KEY_CHECK_DATES, object.getCheckDates());
        return node;
    }

    @Override
    public Car unMarshal(DataNode node) {
        final int wheels = node.getObject(KEY_WHEELS);
        final float horsePower = node.getObject(KEY_HORSE_POWER);
        final Date buildDate = node.getObject(KEY_BUILD_DATE);
        final List<Date> checkDates = node.getObjectList(KEY_CHECK_DATES);
        Car car = new Car(wheels, horsePower, buildDate);
        car.setCheckDates(checkDates);
        return car;
    }

    @Override
    public List<DataMarshaller<?>> getRequiredMarshallers() {
        List<DataMarshaller<?>> list = new ArrayList<DataMarshaller<?>>();
        list.add(new DateMarshaller());
        return list;
    }

}
