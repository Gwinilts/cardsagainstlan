package com.gwinilts.fuckaround;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.IOException;

public class NetworkListener implements Runnable {
    private NetworkLayer layer;
    private DatagramSocket socket;
    private byte[] buffer;
    private DatagramPacket packet;
    private boolean run;


    public NetworkListener(NetworkLayer layer, int port) {
        this.layer = layer;
        this.buffer = new byte[2122];
        this.packet = new DatagramPacket(buffer, buffer.length);
        this.run = true;

        try {
            this.socket = new DatagramSocket(port);
        } catch (IOException e) {
            System.out.println("Listener could not open socket");
        }

        System.out.println("Listener Init End");
    }

    public void stop() {
        this.run = false;
    }

    @Override
    public void run() {
        System.out.println("Listener started");
        int size;
        boolean valid;
        byte[] d, p, sig = {0xb, 0xe, 0xe, 0xf, 0xc, 0xa, 0xc, 0xa};
        while (this.run) {
            try {
                this.socket.receive(packet);
                d = packet.getData();

                valid = true;

                for (byte i = 0; i < sig.length; i++) {
                    valid &= (sig[i] == d[i]);
                }

                if (valid) {
                    size = (d[8] << 8)  + d[9];
                    if (size > 0 && size < 2060) {
                        p = new byte[size];
                        for (int i = 0; i < p.length; i++) {
                            p[i] = d[i + 10];
                        }

                        this.layer.addMsg(p);
                    } else {
                        System.out.println("Packet has bullshit size info. (" + size + ")");
                    }
                } else {
                    System.out.println("Dropping packet with invalid signature.");
                }
            } catch (IOException e) {
                // fatal, app should restart TODO
            }
        }
    }
}
