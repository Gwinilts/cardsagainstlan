package com.gwinilts.fuckaround;

public class Game {
    private static Game game;
    private static void setGame(Game g) {
        game = g;
    }
    public static Game get() {
        return game;
    }

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
    private long[] multiPlay;
    private int playIndex;
    private long currentAward;
    private boolean awarded;
    private boolean multi;

    public Game(NetworkLayer layer, String gameName, String peerName) {
        this.name = gameName;
        this.peer = peerName;
        this.layer = layer;
        this.currentHand = new Hand(peer, 0);
        this.currentPlay = 0;
        this.played = false;
        this.awarded = false;
        this.currentAward = 0;
        multiPlay = new long[2];
        playIndex = 0;
        round = 0;
        Game.setGame(this);
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
        this.currentAward = 0;
        this.awarded = false;
        this.currentPlay = 0;
        playIndex = 0;

        String cardText = new String(layer.blackcards.get(blackCard)).replaceAll("_{2,}", "______");
        multi = (cardText.indexOf("______") != cardText.lastIndexOf("______")) || cardText.startsWith("2 things ");

        System.out.println(multi);
        System.out.println(cardText);

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

    public boolean checkRound(int round) {
        return this.round == round;
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
        byte[] play = NetworkLayer.Verb.PLAY.get(nom.length + 16);

        for (int i = 0; i < nom.length; i++) {
            play[i + 14] = nom[i];
        }

        for (int i = 0; i < 4; i++) {
            play[i + 2] = (byte)(this.round >> (i * 8));
        }

        for (int i = 0; i < 8; i++) {
            play[i + 6] = (byte)(this.currentPlay >> (i * 8));
        }

        return play;
    }

    public byte[] generateMPlay() {
        byte[] nom = (name + "&--&" + peer).getBytes();
        byte[] play = NetworkLayer.Verb.MPLAY.get(nom.length + 24);

        for (int i = 0; i < nom.length; i++) {
            play[i + 22] = nom[i];
        }

        for (int i = 0; i < 4; i++) {
            play[i + 2] = (byte)(this.round >> (i * 8));
        }

        for (int i = 0; i < 8; i++) {
            play[i + 6] = (byte)(this.multiPlay[0] >> (i * 8));
            play[i + 14] = (byte)(this.multiPlay[1] >> (i * 8));
        }

        return play;
    }

    public byte[] generateAward() {
        byte[] nom = (name).getBytes();
        byte[] award = NetworkLayer.Verb.AWARD.get(nom.length + 20);

        for (int i = 0; i < nom.length; i++) {
            award[i + 14] = nom[i];
        }

        for (int i = 0; i < 4; i++) {
            award[i + 2] = (byte)(this.round >> (i * 8));
        }

        for (int i = 0; i < 8; i++) {
            award[i + 6] = (byte)(this.currentAward >> (i * 8));
        }

        return award;
    }



    public int play(long card) {
        if (!multi) {
            this.currentPlay = card;
            this.played = true;
            return 0;
        } else {
            multiPlay[playIndex] = card;
            playIndex++;

            if (playIndex > 1) {
                this.played = true;
            }

            return playIndex;
        }
    }

    public boolean isMulti() {
        return multi;
    }

    public void award(long card) {
        if (this.awarded) return;
        System.out.println("card is now awarded. check for award being sent");
        this.currentAward = card;
        this.awarded = true;
    }

    public boolean isAwarded() {
        return this.awarded;
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
