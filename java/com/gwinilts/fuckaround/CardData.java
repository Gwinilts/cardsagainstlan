package com.gwinilts.fuckaround;


public class CardData {
    private String text;
    private long hash;
    private boolean turned;

    public CardData(String text, long hash) {
        this.text = text;
        this.hash = hash;
        this.turned = false;
    }

    public boolean equals(CardData d) {
        return this.hash == d.hash;
    }

    public boolean isTurned() {
        return turned;
    }

    public void turnOver() {
        turned = true;
    }

    public String getText() {
        return text;
    }

    public long getHash() {
        return hash;
    }
}
