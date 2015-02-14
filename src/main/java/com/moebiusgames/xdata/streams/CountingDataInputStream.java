/*
 * Copyright (C) 2015 Florian Frankenberger.
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
package com.moebiusgames.xdata.streams;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A DataInputStream implementation that keeps track of the amount
 * of data that has been written.
 * 
 * @author Florian Frankenberger
 */
public class CountingDataInputStream extends DataInputStream {

    private final CountingInputStream countingInputStream;

    public CountingDataInputStream(InputStream in) {
        super(new CountingInputStream(in));
        this.countingInputStream = (CountingInputStream) this.in;
    }

    /**
     * returns the current position of the stream
     *
     * @return
     */
    public long getPosition() {
        return this.countingInputStream.getPosition();
    }

    @Override
    public int read() throws IOException {
        return super.read();
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

}
