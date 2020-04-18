package com.gwinilts.fuckaround;

public class Hand {
    private long[] cards;
    private String name;
    private int round;

    public Hand(String name) {
        cards = new long[10];
        this.name = name;
    }

    public Hand(String name, int round) {
        cards = new long[10];
        this.name = name;
        this.round = round;
    }

    public void setCard(int index, long card) {
        if (index > -1 && index < 10) {
            cards[index] = card;
        }
    }

    public long getCard(int index) {
        if (index > -1 && index < 10) {
            return cards[index];
        }
        return 0;
    }

    public int indexOf(long card) {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == card) return i;
        }
        return -1;
    }

    public String getName() {
        return name;
    }

    public boolean ready(int round) {
        return round == this.round;
    }

    public void setRound(int round) {
        this.round = round;
    }
}
