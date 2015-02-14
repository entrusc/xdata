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

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Florian Frankenberger
 */
public class MessageDigestOutputStream extends OutputStream {

    private final OutputStream out;
    private final MessageDigest md;

    public MessageDigestOutputStream(OutputStream out, String algorithm) throws NoSuchAlgorithmException {
        this.out = out;
        this.md = MessageDigest.getInstance(algorithm);
    }

    @Override
    public void write(int b) throws IOException {

        this.md.update((byte) b);
        this.out.write(b);
    }

    @Override
    public void flush() throws IOException {
        this.out.flush();
    }

    @Override
    public void close() throws IOException {
        this.out.close();
    }

    public byte[] getDigest() {
        return this.md.digest();
    }

    public String getDigestString() {
        byte[] bytes = this.md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

}
