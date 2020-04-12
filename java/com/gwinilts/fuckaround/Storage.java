package com.gwinilts.fuckaround;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Storage {
    private static long[] p = {
            2087,
            4481,
            25667,
            36583,
            101771,
            104053,
            103997,
            96973,
            72661,
            59407,
            52691,
            19207,
            13933,
            1061,
            3917,
            26339,
            37273,
            38501,
            76717,
            89273,
            100447,
            103171,
            100003,
            88609,
            78059,
            27127,
            7549
    };
    public static long hash(byte[] buf) {
        int step = 1;

        long hash = p[0];

        for (int i = 0; i < buf.length; i++, step++) {
            if (step == p.length - 1) step = 0;
            if ((i % 2) == 0) {
                hash &= (buf[i] << ((i % 64) * step)) * p[step];
            } else {
                if ((step % 2) == 0) {
                    hash |= (~buf[i] << ((step % 64)) * i) * p[step];
                } else {
                    hash *= (buf[i] % p[step]);
                }
            }
        }
        return hash;
    }

    private RandomAccessFile data;
    private RandomAccessFile tree;

    private long dput(byte[] buf) throws IOException {
        long end, pos;
        try {
            this.data.seek(0);
            end = this.data.readLong();
        } catch (EOFException e) {
            end = 8;
        }

        pos = end;

        this.data.seek(end);

        this.data.writeInt(buf.length);
        end += 4;
        this.data.seek(end);
        this.data.write(buf);
        end += buf.length;

        this.data.seek(0);
        this.data.writeLong(end);

        return pos;
    }

    private byte[] dget(long pos) {
        byte[] buf;
        try {
            this.data.seek(pos);
            buf = new byte[this.data.readInt()];
            this.data.seek(pos + 4);
            this.data.read(buf);
            return buf;
        } catch (IOException e) {
            return null;
        }
    }

    public long[] getAllKeys() {
        long[] keys;
        try {
            keys =  new long[(int)(this.tree.length() / 32)];
            for (long i = 0, x = 0; i < this.tree.length(); i += 32, x++) {
                this.tree.seek(i);
                keys[(int)x] = this.tree.readLong();
            }
            return keys;
        } catch (IOException e) {
            System.out.println("failed to get all keys");
        }
        return null;
    }

    public byte[] get(long hash) {
        long test, pos = 0;
        boolean match = false;

        try {
            while (!match) {
                if (pos < 0) {
                    return null;
                }
                this.tree.seek(pos);
                test = this.tree.readLong();
                if (test == hash) {
                    match = true;
                } else {
                    if (test > hash) { // right
                        this.tree.seek(pos + 16);
                        pos = this.tree.readLong();
                    } else {
                        this.tree.seek(pos + 8);
                        pos = this.tree.readLong();
                    }
                }
            }
            if (match) {
                this.tree.seek(pos + 24);
                pos = this.tree.readLong();
                return this.dget(pos);
            } else {
                System.out.println("no match?");
            }
        } catch (IOException e) {
            System.out.println("ran out of file");
            return null;
        }
        System.out.println("end out of loop");
        return null;
    }

    public long put(byte[] data) {
        long hash = hash(data), test, pos = 0, t;

        while (true) {
            try {
                System.out.println(this.tree.length());
                this.tree.seek(pos);
                test = this.tree.readLong();

                if (test == hash) return hash;
                if (test > hash) {
                    this.tree.seek(t = (pos + 16));
                } else {
                    this.tree.seek(t = (pos + 8));
                }
                test = this.tree.readLong();
                if (test > 0) {
                    pos = test;
                } else {
                    pos = this.tree.length();
                    this.tree.seek(t);
                    this.tree.writeLong(pos);
                    break;
                }
            } catch (IOException e) {
                System.out.println(e);
                break;
            }
        }

        try {
            this.tree.seek(pos);
            this.tree.writeLong(hash);
            this.tree.seek(pos + 8);
            this.tree.writeLong(-1);
            this.tree.seek(pos + 16);
            this.tree.writeLong(-1);
            this.tree.seek(pos + 24);
            test = this.dput(data);
            this.tree.writeLong(this.dput(data));
        } catch (IOException e) {
            System.out.print("oh fuck");
        }
        return hash;
    }

    public Storage(String path) {
        try {
            this.data = new RandomAccessFile(path + "._d", "rw");
            this.tree = new RandomAccessFile(path + "._t", "rw");
        } catch (IOException e) {
            System.out.println("couldn't open filed");
        }
    }
}
