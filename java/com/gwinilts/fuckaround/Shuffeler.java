package com.gwinilts.fuckaround;

import java.security.SecureRandom;

public class Shuffeler extends SecureRandom {

    private int bit;
    private int height;

    public Shuffeler(int height) {
        bit = Integer.toBinaryString(height).length();
        this.height = height;
    }

    public int get() {
        int i = next(bit);

        while (i >= height) {
            i %= height;
        }

        return i;
    }
}
