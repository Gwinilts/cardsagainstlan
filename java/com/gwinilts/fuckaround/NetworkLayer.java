package com.gwinilts.fuckaround;

import java.util.LinkedList;
import java.util.HashMap;
import java.util.ArrayList;

public class NetworkLayer implements Runnable {
    public enum Verb {
        POKE (0x1, 0x0),
        INVITE (0x2, 0x0),
        JOIN (0x3, 0x0),
        CLAIM (0x4, 0x0),
        CONTEST (0x5, 0x0),
        ROUND (0x6, 0x0),
        DEAL (0x7, 0x0),
        PLAY (0x8, 0x0),
        DECK (0x9, 0x0);

        private byte a;
        private byte b;

        Verb(int a, int b) {
            this.a = (byte)a;
            this.b = (byte)b;
        }

        public static Verb from (byte a, byte b) throws NetworkLayerException {
            if (b == 0x0) {
                switch (a) {
                    case 0x1: return POKE;
                    case 0x2: return INVITE;
                    case 0x3: return JOIN;
                    case 0x4: return CLAIM;
                    case 0x5: return CONTEST;
                    case 0x6: return ROUND;
                    case 0x7: return DEAL;
                    case 0x8: return PLAY;
                    case 0x9: return DECK;
                }
            }
            throw new NetworkLayerException(NetworkLayerException.Kind.VERB);
        }

        public byte[] get(int length) {
            byte[] r = new byte[length];
            r[0] = a;
            r[1] = b;
            return r;
        }

        public byte getA() {
            return a;
        }

        public byte getB() {
            return b;
        }
    }

    public class Invite {
        private long lastSeen;
        private String hostName;

        public Invite(String hostName) {
            this.lastSeen = System.currentTimeMillis();
            this.hostName = hostName;
        }

        public long lastSeenSince() {
            return System.currentTimeMillis() - lastSeen;
        }

        public void update() {
            this.lastSeen = System.currentTimeMillis();
        }

        public String hostName() {
            return this.hostName;
        }
    }

    private boolean nameConfirmed;
    private String name;
    private LinkedList<byte[]> newMsgs;
    private HashMap<String, Long> peerLastSeen;
    private HashMap<String, Invite> gameData;
    private HashMap<String, Long>  currentGameLastSeen;
    private ArrayList<String> peerNames;
    private ArrayList<String> gameNames;
    private ArrayList<String> currentGamePeers;
    private Storage whitecards;
    private Storage blackcards;
    private NetworkListener listener;
    private NetworkSpeaker speaker;
    private Thread me;
    private HostGame currentHostGame;
    private Game currentGame;

    private String game;
    private boolean host;

    private byte claimCount;

    private MainActivity app;

    public NetworkLayer(MainActivity app) throws NetworkLayerException {
        this.app = app;

        nameConfirmed = false;
        newMsgs = new LinkedList<byte[]>();
        peerLastSeen = new HashMap<String, Long>();
        gameData = new HashMap<String, Invite>();
        currentGameLastSeen = new HashMap<String, Long>();
        currentGamePeers = new ArrayList<String>();
        peerNames = new ArrayList<String>();
        gameNames = new ArrayList<String>();
        listener = new NetworkListener(this, 11582);
        speaker = new NetworkSpeaker(this, 11583);

        String path = app.getDataDir().getPath() + "/";

        whitecards = new Storage(path + "whitecards");
        blackcards = new Storage(path + "blackcards");
    }

    public synchronized void addMsg(byte[] msg) {
        newMsgs.add(msg);
    }

    private synchronized byte[] nextMsg() {
        if (newMsgs.peek() == null) return null;
        return newMsgs.remove();
    }

    @Override
    public void run()  {
        boolean run = true;
        long lastClaimTime = -1, lastPokeTime = System.currentTimeMillis(), lastPollTime = System.currentTimeMillis();
        long sinceLastClaim = 1000, sinceLastPoke = 0;
        claimCount = 0;

        Thread listen = new Thread(listener), speak = new Thread(speaker);

        listen.start();
        speak.start();

        while (run) {
            if (nameConfirmed) {
                // poke every 1200 ms so other clients know we're here
                sinceLastPoke = System.currentTimeMillis() - lastPokeTime;

                if (sinceLastPoke > 1200)  {
                    lastPokeTime = System.currentTimeMillis();
                    speaker.addMsg(generatePoke());

                    if (game != null) {
                        speaker.addMsg(generateJoin());
                        if (host) {
                            speaker.addMsg(generateInvite());

                            if (currentHostGame != null) {
                                speaker.addMsg(currentHostGame.generateRound());
                            }
                        }
                    }
                }
            } else {
                if (name != null) {
                    // claim
                    if (claimCount < 4) {
                        if (lastClaimTime < 0) {
                            claimCount = 1;
                            lastClaimTime = System.currentTimeMillis();
                            speaker.addMsg(generateClaim());
                        } else {
                            sinceLastClaim = System.currentTimeMillis() - lastClaimTime;
                            if (sinceLastClaim > 1000) {
                                claimCount++;
                                lastClaimTime = System.currentTimeMillis();
                                speaker.addMsg(generateClaim());
                            }
                        }
                    } else {
                        nameConfirmed = true;
                        app.claimSuccess();
                    }
                }
            }
            handleMsg();
            if ((System.currentTimeMillis() - lastPollTime) > 1000) {
                userPoll();
                invitePoll();
                gamePoll();
                lastPollTime = System.currentTimeMillis();
            }
            timeOut();
        }
    }

    private void handleMsg() {
        byte[] msg;
        long t = System.currentTimeMillis() + 600;

        while (((msg = nextMsg()) != null) && System.currentTimeMillis() < t) {
            byte[] data = new byte[msg.length - 4];

            for (int i = 0; i < data.length && i < 2044; i++) {
                data[i] = msg[i + 2];
            }

            try {
                Verb v = Verb.from(msg[0], msg[1]);

                switch (v) {
                    case POKE: {
                        checkPoke(data);
                        break;
                    }
                    case CLAIM: {
                        checkClaim(data);
                        break;
                    }
                    case JOIN: {
                        checkJoin(data);
                        break;
                    }
                    case INVITE: {
                        checkInvite(data);
                        break;
                    }
                    case CONTEST: {
                        checkContest(data);
                        break;
                    }
                    case ROUND: {
                        checkRound(data);
                        break;
                    }
                    case DECK: {
                        checkDeck(data);
                        break;
                    }
                    case DEAL: {
                        checkDeal(data);
                        break;
                    }
                    case PLAY: {
                        checkPlay(data);
                        break;
                    }
                    default: {
                        System.out.println("run out of things");
                    }
                }
            } catch (NetworkLayerException e) {
                if (e.getKind() == NetworkLayerException.Kind.VERB) {
                    System.out.println("dropping nonsense message!");
                } else {
                    System.out.println("unhandled networkLayer exception in handleMsg");
                }
            }
        }
    }

    private void userPoll() {
        long lastSeen = 0;
        ArrayList<String> hitList = new ArrayList<String>();
        for (String user: peerNames) {
            lastSeen = System.currentTimeMillis() - peerLastSeen.get(user);
            if (lastSeen > 6500) {
                System.out.println(user + " has been down for > 6500ms, removing them.");
                hitList.add(user);
            }
        }
        for (String hit: hitList) {
            peerNames.remove(hit);
            peerLastSeen.remove(hit);
        }
        app.updateOnline(peerNames.toArray(new String[0]));
    }

    private void invitePoll() {
        ArrayList<String> hitList = new ArrayList<String>();
        Invite game;

        for (String g: gameNames) {
            game = gameData.get(g);
            if (game.lastSeenSince() > 6500) {
                System.out.println(g + " has been down for > 6500ms, removing it.");
                hitList.add(g);
            }
        }

        for (String hit: hitList) {
            gameNames.remove(hit);
            gameData.remove(hit);
        }

        app.updateJoinable(gameNames.toArray(new String[0]));
    }

    private void gamePoll() {
        ArrayList<String> hitList = new ArrayList<String>();
        long lastSeen = 0;

        for (String g: currentGamePeers) {
            lastSeen = System.currentTimeMillis() - currentGameLastSeen.get(g);

            if (lastSeen > 6500) {
                System.out.println(g + " has been down for > 6500ms, removing it.");
                hitList.add(g);
            }
        }

        for (String hit: hitList) {
            currentGamePeers.remove(hit);
            currentGameLastSeen.remove(hit);
        }

        app.updateCurrentGamePeers(currentGamePeers.toArray(new String[0]));
    }

    private void checkPoke(byte[] msg) {
        String nom = new String(msg);

        if (peerNames.contains(nom)) {
            System.out.println("Got POKE from known peer");
            peerLastSeen.put(nom, System.currentTimeMillis());
        } else {
            System.out.println("Discovered new peer " + nom);
            peerNames.add(nom);
            peerLastSeen.put(nom, System.currentTimeMillis());
        }
    }

    private void checkClaim(byte[] msg) {
        if (!nameConfirmed) {
            System.out.println("cannot check claims, name not confirmed");
            return;
        }
        String claim = new String(msg);
        if (claim.equals(name)) {
            System.out.println("someone is claiming my username!");
            speaker.addMsg(generateContest());
        }
    }

    private void checkDeal(byte[] msg) {
        if ((game == null) || (currentGame == null)) return;

        long[] deck = new long[10];
        int round = 0, split;
        String gName;
        String pName;
        String data;

        /*
          for (int i = 0; i < 4; i++) {
            deal[i + 2] = (byte)(this.round >> (i * 8));
        }
         */

        for (int i = 0; i < 4; i++) {
            round |= ((int)(msg[i]) & 0x000000FF) << (i * 8);
            msg[i] = ' ';
        }

        /*
        for (int i = 0; i < 10; i++) {
            c = deck.getCard(i);
            for (int x = 0; x < 8; x++) {
                deal[((8 * i) + 6) + x] = (byte)(c >> (x * 8));
            }
        }
         */

        for (int i = 0; i < 10; i++) {
            deck[i] = 0;
            for (int x = 0; x < 8; x++) {
                deck[i] |= ((long)(msg[4 + (8 * i) + x]) & 0x00000000000000FF) << (x * 8);
                msg[4 + (8 * i) + x] = ' ';
            }
        }

        data = (new String(msg)).trim();
        split = data.indexOf("&--&");
        gName = data.substring(0, split);
        pName = data.substring(split + 4);

        System.out.println(data);

        if (game.equals(gName) && name.equals(pName)) {
            System.out.println("got valid deal");
            currentGame.setDeck(round, deck);
        } else {
            System.out.println("got invalid deal for " + gName + "/" + pName);
        }
    }

    private void checkPlay(byte[] msg) {
        if (game == null || currentGame == null) return;

        long card = 0;
        int split;
        String data;

        for (int i = 0; i < 8; i++) {
            card |= ((long)(msg[i]) & 0x00000000000000FF) << (i * 8);
            msg[i] = ' ';
        }

        System.out.println("got play " + String.format("0x%016X", card));

        data = (new String(msg)).trim();

        split = data.indexOf("&--&");

        if (game.equals(data.substring(0, split))) {
            if (currentHostGame != null) {
                currentHostGame.submitCard(data.substring(split + 4), card);
            }
            if (currentGame.isCzar()) {
                addWhiteCard(card);
            }
        }
    }

    private void checkDeck(byte[] msg) {
        if ((game == null) || (host == false) || currentHostGame == null) return;

        String data = new String(msg);
        int split = data.indexOf("&--&");

        if (game.equals(data.substring(0, split))) {
            System.out.println("got valid deck");
            speaker.addMsg(currentHostGame.generateDeal(data.substring(split + 4)));
        }
    }

    private void checkRound(byte[] msg) {
        if (game == null) return;
        if (currentGame == null) currentGame = new Game(this, game, name);

        int round = 0, split;
        long card = 0;
        String gName;
        String cName;
        String data;

        for (int i = 0; i < 4; i++) {
            round |= ((int)(msg[i]) & 0x000000FF) << (8 * i);
            msg[i] = ' ';
        }

        String dbg = ""; // transport is ok, reconstruction is not ok

        for (int i = 0; i < 8; i++) {
            card |= ((long)(msg[i + 4]) & 0x00000000000000FF) << (8 * i);
            msg[i + 4] = ' ';
        }

        data = (new String(msg)).trim();
        split = data.indexOf("&--&");
        gName = data.substring(0, split);
        cName = data.substring(split + 4);

        if (game.equals(gName)) {
            System.out.println("got valid round");
            currentGame.setRound(round, cName, card);
        }
    }

    private void checkInvite(byte[] msg) {
        String[] data = new String[2];
        String d = new String(msg);
        int split = d.indexOf("&--&");
        data[0] = d.substring(0, split);
        data[1] = d.substring(split + 4);

        Invite game;

        if (gameNames.contains(data[0])) {
            System.out.println("Got INVITE for known game");

            game = gameData.get(data[0]);

            if (data[1].equals(game.hostName())) {
                game.update();
            } else {
                // TODO: contest INVITE
            }
        } else {
            System.out.println("Discovered new game " + data[0]);
            gameNames.add(data[0]);
            gameData.put(data[0], new Invite(data[1]));
        }
    }

    private void checkJoin(byte[] msg) {
        // join is only interesting if the game being joined is the same game we're in
        if (game == null) return;
        String[] data = new String[2];
        String d = new String(msg);

        int split = d.indexOf("&--&");
        data[0] = d.substring(0, split);
        data[1] = d.substring(split + 4);

        if (!game.equals(data[1])) {
            System.out.println("Dropped JOIN for " + data[1] + " my game is " + game);
            return;
        }

        if (currentGamePeers.contains(data[0])) {
            System.out.println("Got JOIN from known peer");
            currentGameLastSeen.put(data[0], System.currentTimeMillis());
        } else {
            System.out.println("Got JOIN from new peer");
            currentGamePeers.add(data[0]);
            currentGameLastSeen.put(data[0], System.currentTimeMillis());
        }
    }

    private void checkContest(byte[] msg) {
        if (nameConfirmed) {
            if (name.equals(new String(msg))) {
                System.out.println("Someone contested my username but it's already confirmed. Ignoring.");
            }
        } else {
            if (name != null) {
                if (name.equals(new String(msg))) {
                    System.out.println("Someone already has my username, I must restart.");
                    name = null;
                    claimCount = 0;
                    app.claimFailed();
                }
            }
        }
    }

    private byte[] generatePoke() {
        byte[] nom = name.getBytes();
        byte[] poke = Verb.POKE.get(nom.length + 4);

        for (int i = 0; i < nom.length; i++) {
            poke[i + 2] = nom[i];
        }

        poke[poke.length - 2] = 0x00;
        poke[poke.length - 1] = 0x01;

        return poke;
    }

    private byte[] generateContest() {
        byte[] nom = name.getBytes();
        byte[] contest = Verb.CONTEST.get(nom.length + 4);

        for (int i = 0; i < nom.length; i++) {
            contest[i + 2] = nom[i];
        }

        contest[contest.length - 2] = 0x00;
        contest[contest.length - 1] = 0x01;

        return contest;
    }

    private byte[] generateClaim() {
        byte[] nom = name.getBytes();
        byte[] claim = Verb.CLAIM.get(nom.length + 4);

        for (int i = 0; i < nom.length; i++) {
            claim[i + 2] = nom[i];
        }

        claim[claim.length - 2] = 0x00;
        claim[claim.length - 1] = 0x00;

        return claim;
    }

    private byte[] generateInvite() {
        byte[] nom = (game + "&--&" + name).getBytes();
        byte[] invite = Verb.INVITE.get(nom.length + 4);

        for (int i = 0; i < nom.length; i++) {
            invite[i + 2] = nom[i];
        }

        invite[invite.length - 2] = 0x00;
        invite[invite.length - 1] = 0x00;

        return invite;
    }

    private byte[] generateJoin() {
        byte[] nom = (name + "&--&" + game).getBytes();
        byte[] join = Verb.JOIN.get(nom.length + 4);

        for (int i = 0; i < nom.length; i++) {
            join[i + 2] = nom[i];
        }

        join[join.length - 2] = 0x00;
        join[join.length - 1] = 0x00;

        return join;
    }

    private void timeOut() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            System.out.println("Layer woke up early");
        }
    }

    public void start() {
        me = new Thread(this);
        me.start();
    }

    public void claim(String name) {
        this.name = name;
    }

    public void setGame(String name, boolean host) {
        this.currentGameLastSeen.clear();
        this.currentGamePeers.clear();
        this.game = name;
        this.host = host;
    }

    public void startGame() {
        if (this.game != null && this.host) {
            this.currentHostGame = new HostGame(this, this.game);
            for (String peer: currentGamePeers) {
                this.currentHostGame.addPeer(peer);
            }
            this.currentHostGame.nextRound();
        }
    }

    public void openCzarView() {
        String cardText = new String(blackcards.get(currentGame.getBlackCard()));

        app.openCzarView(cardText);
    }

    public void addWhiteCard(long card) {
        CardData c = new CardData(new String(whitecards.get(card)), card);
        app.updateCzarWhiteCards(c);
    }

    public void updateHand() {
        CardData[] d = new CardData[10];

        Hand h = currentGame.getHand();

        for (int i = 0; i < 10; i++) {
            d[i] = new CardData(new String(whitecards.get(h.getCard(i))), h.getCard(i));
            System.out.println(String.format("0x%016X", h.getCard(i)));
        }

        app.updateHand(d);
    }

    public void updateBlackCard() {
        app.updateBlackCard(new String(blackcards.get(this.currentGame.getBlackCard())));
    }

    public void openPlayView() {
        app.openGameView();
    }

    public void playCard(long card) {
        speaker.addMsg(currentGame.generatePlay(card));
    }

    public boolean peerIsLive(String name) {
        return this.peerNames.contains(name);
    }

    public long[] getFullWhiteDeck() {
        return this.whitecards.getAllKeys();
    }

    public long[] getFullBlackDeck() {
        return this.blackcards.getAllKeys();
    }

    public void exitGame() {
        this.currentGameLastSeen.clear();
        this.currentGamePeers.clear();
        this.game = null;
        this.host = false;
    }
}
