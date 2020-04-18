package com.gwinilts.fuckaround;

public class Crown {

    private String peer;
    private long card;

    public Crown(String peer, long card) {
        this.peer = peer;
        this.card = card;
    }

    public byte[] generate(String game) {
        byte[] data = (game + "&--&" + peer).getBytes();
        byte[] crown = NetworkLayer.Verb.CROWN.get(data.length + 14);

        for (int i = 0; i < data.length; i++) {
            crown[i + 10] = data[i];
        }

        for (int i = 0; i < 8; i++) {
            crown[i + 2] = (byte)(this.card >> (i * 8));
        }

        return crown;
    }

}
