package com.gwinilts.fuckaround;

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.IOException;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Iterator;

import java.util.LinkedList;

public class NetworkSpeaker implements Runnable {
    private NetworkLayer layer;
    private DatagramSocket socket;
    private byte[] buffer;
    private DatagramPacket packet;
    private InetAddress bcast;
    private boolean run;

    private LinkedList<byte[]> queue;

    public NetworkSpeaker(NetworkLayer layer, int port) throws NetworkLayerException {
        try {
            socket = new DatagramSocket(port);
            buffer = new byte[2048];
            packet = new DatagramPacket(buffer,buffer.length);
            bcast = getBcast();
            queue = new LinkedList<byte[]>();
            run = true;
        } catch (IOException e) {
            System.out.println("Could not open speaker");
        }

        System.out.println("Speaker init done");
    }

    public synchronized void addMsg(byte[] msg) {
        if (msg.length < 2048) {
            queue.add(msg);
        } else {
            System.out.println("Not adding message as it's too long.");
        }
    }

    public synchronized byte[] nextMsg() {
        if (queue.peek() == null) return null;
        buffer = queue.remove();
        byte[] data = new byte[buffer.length + 10];
        data[0] = 0xb;
        data[1] = 0xe;
        data[2] = 0xe;
        data[3] = 0xf;
        data[4] = 0xc;
        data[5] = 0xa;
        data[6] = 0xc;
        data[7] = 0xa;
        data[9] = (byte) buffer.length;
        data[8] = (byte) (buffer.length >>> 8);

        for (int i = 0; i < buffer.length; i++) {
            data[i + 10] = buffer[i];
        }
        return data;
    }

    public void stop() {
        run = false;
    }

    private void timeOut() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            System.out.println("Layer woke up early");
        }
    }

    public void shutDown() {
        this.socket.close();
        this.run = false;
    }

    private InetAddress getBcast() throws NetworkLayerException {
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            NetworkInterface i;
            Iterator addr;
            InterfaceAddress a;
            InetAddress bcast = null;

      /*
      We need to find a valid IPv4 bcast address to look for clients with.

      We won't accept interfaces that aren't up or are loopback interfaces.

      We will stupidly accept the valid bcast address we can find.
      */

            boolean valid;

            while (ifaces.hasMoreElements()) {
                i = ifaces.nextElement();
                System.out.println("Found Interface: " + i.getDisplayName());

                valid = true;

                if (i.isLoopback()) {
                    System.out.println("Is a loopback, ignoring.");
                    valid = false;
                }
                if (!i.isUp()) {
                    System.out.println("Is not up, ignoring.");
                    valid = false;
                }

                if (valid) {
                    addr = i.getInterfaceAddresses().iterator();

                    while (addr.hasNext()) {
                        a = (InterfaceAddress) addr.next();

                        if (a == null) {
                            System.out.println("Found null address.");
                            continue;
                        }
                        if ((bcast = a.getBroadcast()) == null) {
                            System.out.println("Found an unusable IPv6 addr.");
                        } else {
                            System.out.println("Found valid IPv4 bcast addr: " + bcast.getHostAddress());
                            return bcast;
                        }
                    }
                }
                if (bcast != null) break;
            }
        } catch (SocketException e) {
            System.out.println("CRASH");
        }
        System.out.println("could not find a usable ipv4 bcast address");
        throw new NetworkLayerException(NetworkLayerException.Kind.BCAST);
    }

    @Override
    public void run() {
        System.out.println("Speaker Started");

        try {
            while (run) {
                while ((buffer = nextMsg()) != null) {
                    packet = new DatagramPacket(buffer, buffer.length, bcast, 11582);
                    socket.send(packet);
                }

                timeOut();
            }
        } catch (IOException e) {
            // TODO
        }
    }
}
