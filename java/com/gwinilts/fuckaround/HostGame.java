package com.gwinilts.fuckaround;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

public class HostGame {
    public static class PeerPlay {
        private long card;
        private int index;

        public PeerPlay(long card, int index) {
            this.card = card;
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public long getCard() {
            return card;
        }
    }

    private LinkedList<String> allPeers;
    private NetworkLayer layer;
    private HashMap<String, Hand> currentDecks;
    private HashMap<String, PeerPlay> currentPlay;
    private long currentBlackCard;
    private ArrayList<Long> whiteDeck;
    private ArrayList<Long> blackDeck;
    private ArrayList<Crown> crowns;
    private SecureRandom rng;
    private String gameName;

    private int round;

    public HostGame(NetworkLayer l, String name) {
        layer = l;
        allPeers = new LinkedList<String>();
        currentDecks = new HashMap<String, Hand>();
        currentPlay = new HashMap<>();
        crowns = new ArrayList<>();
        rng = new SecureRandom();
        this.gameName = name;

        whiteDeck = new ArrayList<Long>();
        blackDeck = new ArrayList<Long>();
        round = 0;
    }

    public void addPeer(String name) {
        if (!allPeers.contains(name)) allPeers.add(name);
    }

    public void submitCard(String peer, long card) {
        Hand h = currentDecks.get(peer);
        /*PeerPlay p = currentPlay.get(peer);

        if (p != null) return;*/

        int index = h.indexOf(card);

        System.out.println("submit " + card + " at " + index + " for " + peer);

        if (index > -1) {
            currentPlay.put(peer, new PeerPlay(card, index));
            h.setCard(index, pickRandomWhiteCard());
        }
    }

    public void submitCard(String peer, long[] cards) {
        if (cards.length > 0) {
            if (cards.length > 1) {
                for (int i = cards.length - 1; i >= 0; i--) {
                    submitCard(peer, cards[i]);
                }
            } else {
                submitCard(peer, cards[0]);
            }
        }
    }

    public boolean hasCrowns() {
        return crowns.size() > 0;
    }

    public byte[][] generateCrowns() {
        byte[][] crowns = new byte[this.crowns.size()][];

        for (int i = 0; i < crowns.length; i++) {
            crowns[i] = this.crowns.get(i).generate(gameName);
        }

        return crowns;
    }

    public void awardRound(long card, int round) {
        if (round != this.round) {
            System.out.println("wrong round for award (" + round + " instead of " + this.round + ")");
            return;
        }
        PeerPlay play;

        for (String peer: currentPlay.keySet()) {
            play = currentPlay.get(peer);
            if (play.card == card) {
                crowns.add(new Crown(peer, currentBlackCard));
            }
        }

        nextRound();
    }

    public void nextRound() {
        round++;
        Hand deck;
        PeerPlay play;
        try {
            currentBlackCard = layer.blackcards.next();
        } catch (Exception e) {
            System.out.println("could not pick a black card (fatal)");
            System.out.flush();
            System.exit(-1);
        }

        for (String peer: allPeers) { // deal the white cards
            deck = currentDecks.get(peer);
            if (deck == null) {
                currentDecks.put(peer, deck = new Hand(peer));
                for (int i = 0; i < 10; i++) {
                    deck.setCard(i, pickRandomWhiteCard());
                }
            }
        }

        // wipe previous submissions

        currentPlay.clear();

        // set next turn
        String peer;

        do {
            allPeers.add(allPeers.remove());
            peer = allPeers.peek();
        } while (!layer.peerIsLive(peer)); // can't let the card czar be someone who isn't available right now

        // set black card
    }

    public byte[] generateRound() {
        byte[] sData = (gameName + "&--&" + getCardCZar()).getBytes();
        byte[] round = NetworkLayer.Verb.ROUND.get(sData.length + 16);

        for (int i = 0; i < sData.length; i++) {
            round[i + 14] = sData[i];
        }

        for (int i = 0; i < 4; i++) {
            round[i + 2] = (byte)(this.round >> (i * 8));
        }


        for (int i = 0; i < 8; i++) {
            round[i + 6] = (byte)(this.currentBlackCard >> (i * 8));
        }

        return round;
    }

    public byte[] generateDeal(String name) {
        Hand deck = currentDecks.get(name);

        if (deck == null) {
            currentDecks.put(name, deck = new Hand(name));

            for (int i = 0; i < 10; i++) {
                deck.setCard(i, pickRandomWhiteCard());
            }
        }

        byte[] sData = (gameName + "&--&" + name).getBytes();

        byte[] deal = NetworkLayer.Verb.DEAL.get(sData.length + 88);

        for (int i = 0; i < 4; i++) {
            deal[i + 2] = (byte)(this.round >> (i * 8));
        }

        long c;

        for (int i = 0; i < 10; i++) {
            c = deck.getCard(i);
            for (int x = 0; x < 8; x++) {
                deal[((8 * i) + 6) + x] = (byte)(c >> (x * 8));
            }
        }

        for (int i = 0; i < sData.length; i++) {
            deal[86 + i] = sData[i];
        }

        return deal;
    }

    public String getCardCZar() {
        return allPeers.peek();
    }

    public long pickRandomWhiteCard() {
        try {
            return layer.whitecards.next();
        } catch (Exception e) {
            System.out.println("could not pick a white card (fatal)");
            System.exit(-1);
        }
        return 0;
    }
}
