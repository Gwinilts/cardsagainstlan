package com.gwinilts.fuckaround;


public class CardData {
    private String text;
    private long hash;

    public CardData(String text, long hash) {
        this.text = text;
        this.hash = hash;
    }

    public boolean equals(CardData d) {
        return this.hash == d.hash;
    }

    public String getText() {
        return text;
    }

    public long getHash() {
        return hash;
    }
}
