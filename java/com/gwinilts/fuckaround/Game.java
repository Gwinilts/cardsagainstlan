package com.gwinilts.fuckaround;

public class Game {
    private String name;
    private String peer;
    private String currentCzar;
    private Hand currentHand;
    private int round;
    private NetworkLayer layer;
    private boolean czar;
    private long currentBlackCard;
    private boolean played;
    private long currentPlay;

    public Game(NetworkLayer layer, String gameName, String peerName) {
        this.name = gameName;
        this.peer = peerName;
        this.layer = layer;
        this.currentHand = new Hand(peer, 0);
        this.currentPlay = 0;
        this.played = false;
        round = 0;
    }

    public boolean setRound(int round, String czar, long blackCard) {
        if (this.round == round) {
            if (!currentHand.ready(round)) {
                return true;
            }
            return false;
        }

        this.round = round;
        this.currentCzar = czar;
        this.currentBlackCard = blackCard;
        this.played = false;
        this.currentPlay = 0;

        this.czar = (this.currentCzar.equals(this.peer));
        layer.addMsg(generateDeck());
        layer.updateBlackCard();

        if (this.czar) {
            layer.openCzarView();
            return false;
        } else {
            layer.openPlayView();
            return true;
        }
    }

    public void setDeck(int round, long[] cards) {
        if (currentHand.ready(round)) return;

        System.out.println("setting deck");

        for (int i = 0; i < 10; i++) {
            currentHand.setCard(i, cards[i]);
        }

        currentHand.setRound(round);

        layer.updateHand();

        System.out.println("finished setting deck");
    }

    public long getBlackCard() {
        return this.currentBlackCard;
    }

    public byte[] generateDeck() {
        byte[] data = (name + "&--&" + peer).getBytes();

        byte[] deck = NetworkLayer.Verb.DECK.get(data.length + 4);

        for (int i = 0; i < data.length; i++) {
            deck[i + 2] = data[i];
        }

        return deck;
    }

    public byte[] generatePlay() {
        byte[] nom = (name + "&--&" + peer).getBytes();
        byte[] play = NetworkLayer.Verb.PLAY.get(nom.length + 12);

        for (int i = 0; i < nom.length; i++) {
            play[i + 10] = nom[i];
        }

        for (int i = 0; i < 8; i++) {
            play[i + 2] = (byte)(this.currentPlay >> (i * 8));
        }

        return play;
    }

    public void play(long card) {
        this.currentPlay = card;
        this.played = true;
    }

    public boolean isPlayed() {
        return this.played;
    }

    public Hand getHand() {
        return this.currentHand;
    }

    public boolean isCzar() {
        return this.czar;
    }
}
