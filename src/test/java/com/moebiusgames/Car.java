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
package com.moebiusgames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A sample object
 * 
 * @author Florian Frankenberger
 */
public class Car {

    private final int wheels;
    private final float horsePower;
    
    private final Date buildDate;
    private List<Date> checkDates = new ArrayList<Date>();

    public Car(int wheels, float horsePower, Date buildDate) {
        this.wheels = wheels;
        this.horsePower = horsePower;
        this.buildDate = buildDate;
    }

    public Date getBuildDate() {
        return buildDate;
    }

    public float getHorsePower() {
        return horsePower;
    }

    public int getWheels() {
        return wheels;
    }
    
    public List<Date> getCheckDates() {
        return Collections.unmodifiableList(this.checkDates);
    }

    public void setCheckDates(List<Date> checkDates) {
        this.checkDates = checkDates;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + this.wheels;
        hash = 29 * hash + Float.floatToIntBits(this.horsePower);
        hash = 29 * hash + (this.buildDate != null ? this.buildDate.hashCode() : 0);
        hash = 29 * hash + (this.checkDates != null ? this.checkDates.hashCode() : 0);
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
        final Car other = (Car) obj;
        if (this.wheels != other.wheels) {
            return false;
        }
        if (Float.floatToIntBits(this.horsePower) != Float.floatToIntBits(other.horsePower)) {
            return false;
        }
        if (this.buildDate != other.buildDate && (this.buildDate == null || !this.buildDate.equals(other.buildDate))) {
            return false;
        }
        return this.checkDates == other.checkDates || (this.checkDates != null && this.checkDates.equals(other.checkDates));
    }

    @Override
    public String toString() {
        return "Car{" + "wheels=" + wheels + ", horsePower=" + horsePower + ", buildDate=" + buildDate + ", checkDates=" + checkDates + '}';
    }

}
