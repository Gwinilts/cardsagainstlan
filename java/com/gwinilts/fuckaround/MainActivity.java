package com.gwinilts.fuckaround;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.text.DateFormat;
import android.view.inputmethod.InputMethodManager;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import com.google.android.material.tabs.TabLayout;

import org.w3c.dom.Text;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    private View splashFrame;
    private View lobbyFrame;
    private View gameFrame;
    private View hostFrame;
    private View gameLobbyFrame;
    private View czarGameFrame;
    private View tabbedGameView;
    private View gameScoreTab;

    private String username;
    private NetworkLayer layer;

    private ArrayAdapter<String> onlineList;
    private ArrayAdapter<String> gameList;
    private ArrayAdapter<String> currentGamePeerList;
    private CardDeckAdapter cardDeckAdapter;
    private CzarDeckAdapter czarPlayCards;
    private ScoreDeckAdapter scoreCards;
    private RecyclerView.LayoutManager cardDeckAdapterManager;
    private RecyclerView.LayoutManager czarPlayCardsManager;
    private RecyclerView.LayoutManager scoreCardManager;
    private boolean czarMode;
    private boolean helpOpen;
    private boolean multiPlay;

    private View activeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String logContent = "No Log Content";
        try {
            File log = new File(getExternalFilesDir("logs").getAbsolutePath() + "/main.log");

            if (log.exists()) {
                Scanner s = new Scanner(log);
                logContent = "";
                while (s.hasNextLine()) {
                    logContent += s.nextLine() + "\n";
                }
                s.close();
            }
            PrintStream out = new PrintStream(new File(getExternalFilesDir("logs").getAbsolutePath() + "/main.log"));
            System.setErr(out);
            System.setOut(out);
        } catch (Exception e) {
            System.out.println("couldn't set new log dest");
        }

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                System.err.println("Uncaught exception in thread " + t.getName());
                System.err.println(e.getMessage());

                StackTraceElement[] s = e.getStackTrace();

                for (StackTraceElement i: s) {
                    System.err.println(i.toString());
                }

                System.err.println("Fatality!");

                System.err.flush();
                System.out.flush();
                System.exit(-1);
            }
        });

        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("dd-mm-yyyy hh:mm:ss");
        String strDate = dateFormat.format(date);

        System.out.println("com.gwinilts.fuckaround init");
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println(strDate);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ((TextView)findViewById(R.id.textView8)).setText(logContent);

        String dPath = getDataDir().getPath();
        try {
            File white = new File(dPath + "/whitecards._d");

            if (!white.exists()) {
                shipAssets();
            }
        } catch (Exception e) {

        }

        this.splashFrame = findViewById(R.id.entryPointFrame);
        this.lobbyFrame = findViewById(R.id.networkStatusFrame);
        this.gameFrame = findViewById(R.id.gameNormalView);
        this.hostFrame = findViewById(R.id.createMatchFrame);
        this.gameLobbyFrame = findViewById(R.id.gameLobbyFrame);
        this.czarGameFrame = findViewById(R.id.cardCzarGameView);
        this.tabbedGameView = findViewById(R.id.gameLayoutFrame);
        this.gameScoreTab = findViewById(R.id.gameScoreView);

        this.lobbyFrame.setVisibility(View.GONE);
        this.gameFrame.setVisibility(View.GONE);
        this.hostFrame.setVisibility(View.GONE);
        this.gameLobbyFrame.setVisibility(View.GONE);
        this.splashFrame.setVisibility(View.VISIBLE);
        this.multiPlay = false;

        helpOpen = false;
        activeView = splashFrame;

        this.czarMode = false;

        try {
            this.layer = new NetworkLayer(this);
        } catch (NetworkLayerException e) {
            if (e.getKind() == NetworkLayerException.Kind.BCAST) {
                System.out.println("could not get bcast addr, fucking fatal m8");
            }
        }


        this.layer.start();

        ListView online = (ListView) findViewById(R.id.online);
        ListView joinable = (ListView) findViewById(R.id.joinable);
        ListView currentGamePeers = (ListView) findViewById(R.id.currentGameList);
        RecyclerView cardDeck = (RecyclerView) findViewById(R.id.whitecards);
        RecyclerView playDeck = (RecyclerView) findViewById(R.id.czarPlayDeck);
        RecyclerView scoreDeck = (RecyclerView) findViewById(R.id.playerScoreDeck);

        View splashCard = findViewById(R.id.entryPointCard);

        splashCard.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                tryClaimName(v);
                return true;
            }
        });

        onlineList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        gameList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice);
        currentGamePeerList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        cardDeckAdapter = new CardDeckAdapter(this, new ArrayList<CardData>());
        czarPlayCards = new CzarDeckAdapter(this, new ArrayList<CardData>());
        scoreCards = new ScoreDeckAdapter(this, new ArrayList<CardData>());

        online.setAdapter(onlineList);
        joinable.setAdapter(gameList);
        currentGamePeers.setAdapter(currentGamePeerList);
        cardDeck.setAdapter(cardDeckAdapter);
        playDeck.setAdapter(czarPlayCards);
        scoreDeck.setAdapter(scoreCards);

        cardDeckAdapterManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        cardDeck.setLayoutManager(cardDeckAdapterManager);

        czarPlayCardsManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        playDeck.setLayoutManager(czarPlayCardsManager);

        scoreCardManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        scoreDeck.setLayoutManager(scoreCardManager);

        TabLayout t = findViewById(R.id.gameTabLayout);
        t.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    openGameTab();
                } else {
                    openScoreTab();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        t = findViewById(R.id.helpDialogTabLayout);
        t.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) { // help
                    openHelpTab();
                } else { // rules
                    openRulesTab();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        findViewById(R.id.normalWhiteCardView).setOnDragListener(new CardDeckDropListener(new CardDropCallback() {
            @Override
            public void run(String text, int index) {
                findViewById(R.id.normalWhiteCardView).setAlpha(1);
                submitWhitecard(text, index);
            }
        }));

        findViewById(R.id.cardCzarWhiteZone).setOnDragListener(new CardDeckDropListener(new CardDropCallback() {
            @Override
            public void run(String text, int index) {
                 CardData c = czarPlayCards.deck.get(index);
                ((TextView)findViewById(R.id.czarWhiteCardText)).setText(c.getText());
                findViewById(R.id.cardCzarWhiteZone).setAlpha(1);
                awardRound(c.getHash());
            }
        }));
    }

    public void exitGame(View view) {
        layer.exitGame();
        activeView.setVisibility(View.GONE);
        lobbyFrame.setVisibility(View.VISIBLE);
    }

    public void quit(View view) {
        layer.shutDown();
        finish();
        System.exit(0);
    }

    public void toggleHelp(View view) {
        if (helpOpen) {
            findViewById(R.id.helpfulDialog).setVisibility(View.GONE);
            activeView.setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.helpTip)).setText("Need help? Tap here.");
            helpOpen = false;
        } else {
            activeView.setVisibility(View.GONE);
            findViewById(R.id.helpfulDialog).setVisibility(View.VISIBLE);
            TabLayout t = findViewById(R.id.helpDialogTabLayout);
            ((TextView)findViewById(R.id.helpTip)).setText("Go back.");
            t.getTabAt(0).select();
            openHelpTab();
            helpOpen = true;
        }
    }

    private void shipAssets() {
        System.out.println("Shipping assets!");
        String[] names = {
                "blackcards._d",
                "blackcards._t",
                "whitecards._d",
                "whitecards._t"};

        String path = getDataDir().getPath() + "/";
        InputStream in;
        FileOutputStream out;
        byte[] buffer = new byte[2048];
        int length;

        try {
            for (String name: names) {
                in = getAssets().open("ship/" + name);
                out = new FileOutputStream(path + name);

                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                out.flush();
                out.close();
                in.close();
            }
        } catch (Exception e) {
            System.out.println("ahhh");
            System.out.println(e);
        }
    }

    public void updateCurrentGamePeers(final String[] names) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentGamePeerList.clear();
                currentGamePeerList.addAll(names);
            }
        });
    }

    public void updateJoinable(final String[] games) {
        runOnUiThread(new Runnable() {
           @Override
           public void run() {
               gameList.clear();
               gameList.addAll(games);
           }
        });
    }

    public void updateOnline(final String[] names) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onlineList.clear();
                onlineList.addAll(names);
            }
        });
    }

    public void hideKbd(final View view) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
            }
        });
    }

    public void tryClaimName(View view) {
        EditText cname = (EditText)findViewById(R.id.uname);
        TextView prompt = (TextView)findViewById(R.id.prompt);
        String uname = cname.getText().toString().trim();
        hideKbd(view);

        if (uname.length() < 2) {
            prompt.setText("You need to set a fucking name, bitch");
            return;
        }
        if (uname.length() > 20) {
            prompt.setText("That nam'es too fucking long.");
            return;
        }

        this.username = uname;

        this.layer.claim(this.username);
    }

    public void claimSuccess() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                splashFrame.setVisibility(View.GONE);
                lobbyFrame.setVisibility(View.VISIBLE);
                activeView = lobbyFrame;
            }
        });
    }

    public void claimFailed() {
        final TextView prompt = (TextView)findViewById(R.id.prompt);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EditText cname = (EditText)findViewById(R.id.uname);
                prompt.setText("Someone is already using this name, please change it.");
                hideKbd(cname);
            }
        });
    }

    public void createButtonClick(View view) {
        lobbyFrame.setVisibility(View.GONE);
        hostFrame.setVisibility(View.VISIBLE);
        activeView = hostFrame;
        hideKbd(view);
    }


    public void hostButtonClick(View view) {
        EditText nameInput = findViewById(R.id.hostName);
        TextView prompt = findViewById(R.id.hostPrompt);
        String name = nameInput.getText().toString().trim();
        hideKbd(view);

        if (name.length() > 1 && name.length() < 100) {
            if (name.contains("&")) {
                prompt.setText("Name can't contain ampersands (&).");
            } else {
                layer.setGame(name, true);
                hostFrame.setVisibility(View.GONE);
                gameLobbyFrame.setVisibility(View.VISIBLE);
                activeView = gameLobbyFrame;
            }
        } else {
            prompt.setText("Set a proper name please (1 < length > 200)");
        }
    }

    public void joinGameClick(View view) {
        ListView games = findViewById(R.id.joinable);

        int index = games.getCheckedItemPosition();

        System.out.println(index);

        if (index > -1 && index < gameList.getCount()) {
            layer.setGame(gameList.getItem(index), false);
            lobbyFrame.setVisibility(View.GONE);
            gameLobbyFrame.setVisibility(View.VISIBLE);
            activeView = gameLobbyFrame;
        }
    }

    public void updateHand(final CardData[] cards) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (CardData card: cards) {
                    cardDeckAdapter.addIfUnique(card);
                }
                cardDeckAdapter.notifyDataSetChanged();
            }
        });
    }

    public void openHelpTab() {
        findViewById(R.id.rulesTabContent).setVisibility(View.GONE);
        findViewById(R.id.helpTabContent).setVisibility(View.VISIBLE);
    }

    public void openRulesTab() {
        findViewById(R.id.rulesTabContent).setVisibility(View.VISIBLE);
        findViewById(R.id.helpTabContent).setVisibility(View.GONE);
    }

    public void openGameTab() {
        gameLobbyFrame.setVisibility(View.GONE);
        gameScoreTab.setVisibility(View.GONE);
        tabbedGameView.setVisibility(View.VISIBLE);
        activeView = tabbedGameView;

        if (czarMode) {
            gameFrame.setVisibility(View.GONE);
            czarGameFrame.setVisibility(View.VISIBLE);
        } else {
            gameFrame.setVisibility(View.VISIBLE);
            czarGameFrame.setVisibility(View.GONE);
        }
    }

    public void openScoreTab() {
        gameFrame.setVisibility(View.GONE);
        czarGameFrame.setVisibility(View.GONE);
        tabbedGameView.setVisibility(View.VISIBLE);
        gameScoreTab.setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.scoreInfo)).setText("You've won " + scoreCards.deck.size() + " black cards.");
        activeView = tabbedGameView;
    }

    public void openGameView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                czarMode = false;
                TabLayout t = findViewById(R.id.gameTabLayout);
                t.getTabAt(0).select();
                ((TextView)findViewById(R.id.normalWhiteText1)).setText(R.string.normal_white_card_prompt);
                ((TextView)findViewById(R.id.normalWhiteText2)).setText(R.string.normal_white_card_prompt_2);
                findViewById(R.id.normalWhiteText2).setVisibility(View.GONE);
                findViewById(R.id.normalWhiteCardView).setAlpha((float) 0.5);
                openGameTab();
            }
        });
    }

    public void openCzarView(final String cardText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                czarMode = true;
                TabLayout t = findViewById(R.id.gameTabLayout);
                t.getTabAt(0).select();
                czarPlayCards.deck.clear();
                czarPlayCards.notifyDataSetChanged();
                ((TextView)findViewById(R.id.czarWhiteCardText)).setText(R.string.czar_white_card_prompt);
                findViewById(R.id.cardCzarWhiteZone).setAlpha((float) 0.5);
                openGameTab();
            }
        });
    }

    public void updateBlackCard(final String cardText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView blackCard = findViewById(R.id.gameBlackCard);
                blackCard.setText(cardText);
                TextView text = (TextView) findViewById(R.id.czarCardText);
                text.setText(cardText);
            }
        });
    }



    public void gameLobbyLeaveClick(View view) {
        layer.exitGame();
        gameLobbyFrame.setVisibility(View.GONE);
        lobbyFrame.setVisibility(View.VISIBLE);
        activeView = lobbyFrame;
    }

    public void gameLobbyStartClick(View view) {
        layer.startGame();
    }

    public void cardTest(View view) {
        splashFrame.setVisibility(View.GONE);
        tabbedGameView.setVisibility(View.VISIBLE);
        activeView = tabbedGameView;
        czarMode = false;


        ((TextView)findViewById(R.id.normalWhiteText1)).setText("apple");
        ((TextView)findViewById(R.id.normalWhiteText2)).setText("bananna");

        cardDeckAdapter.addIfUnique(new CardData("bing bong bash", 100));

        cardDeckAdapter.addIfUnique(new CardData("stinky willy", 30));

        cardDeckAdapter.addIfUnique(new CardData("stinky willy", 3000));

        openGameView();


        findViewById(R.id.normalWhiteText2).setVisibility(View.VISIBLE);
        findViewById(R.id.normalWhiteCardView).setVisibility(View.VISIBLE);

    }

    public void updateCzarWhiteCards(final CardData card) {
        if (czarPlayCards.hasCard(card.getHash())) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                czarPlayCards.deck.add(card);
                czarPlayCards.notifyDataSetChanged();
            }
        });
    }

    public void submitWhitecard(String text, int index) {
        CardData c = cardDeckAdapter.deck.get(index);
        Game g = Game.get();

        switch (g.play(c.getHash())) {
            case 0:
            case 1: {
                ((TextView)findViewById(R.id.normalWhiteText1)).setText(text);
                break;
            }
            case 2: {
                ((TextView)findViewById(R.id.normalWhiteText2)).setText(text);
                findViewById(R.id.normalWhiteText2).setVisibility(View.VISIBLE);
            }
        }

        cardDeckAdapter.deck.remove(index);
        cardDeckAdapter.notifyDataSetChanged();
    }

    public void awardRound(long card) {
        System.out.println(String.format("0x%016X", card) + " wins the round");
        Game g = Game.get();
        g.award(card);
    }

    public void addCrown(final CardData c) {
        for (CardData card: scoreCards.deck) {
            if (card.equals(c)) return;
        }
        scoreCards.deck.add(c);
        scoreCards.notifyDataSetChanged();
    }

    public void hostCancelButtonClick(View view) {
        hostFrame.setVisibility(View.GONE);
        lobbyFrame.setVisibility(View.VISIBLE);
        activeView = lobbyFrame;
    }
}
