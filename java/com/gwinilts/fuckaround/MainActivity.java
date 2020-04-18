package com.gwinilts.fuckaround;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.io.File;

public class MainActivity extends AppCompatActivity {
    private View splashFrame;
    private View lobbyFrame;
    private View gameFrame;
    private View hostFrame;
    private View gameLobbyFrame;
    private View czarGameFrame;

    private String username;
    private NetworkLayer layer;

    private ArrayAdapter<String> onlineList;
    private ArrayAdapter<String> gameList;
    private ArrayAdapter<String> currentGamePeerList;
    private CardDeckAdapter cardDeckAdapter;
    private CzarDeckAdapter czarPlayCards;
    private RecyclerView.LayoutManager cardDeckAdapterManager;
    private RecyclerView.LayoutManager czarPlayCardsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String dPath = getDataDir().getPath();
        try {
            File white = new File(dPath + "/whitecards._d");

            if (!white.exists()) {
                shipAssets();
            }
        } catch (Exception e) {

        }

        this.splashFrame = findViewById(R.id.frame1);
        this.lobbyFrame = findViewById(R.id.frame2);
        this.gameFrame = findViewById(R.id.frame3);
        this.hostFrame = findViewById(R.id.frame4);
        this.gameLobbyFrame = findViewById(R.id.frame5);
        this.czarGameFrame = findViewById(R.id.frame6);

        this.lobbyFrame.setVisibility(View.GONE);
        this.gameFrame.setVisibility(View.GONE);
        this.hostFrame.setVisibility(View.GONE);
        this.gameLobbyFrame.setVisibility(View.GONE);
        this.splashFrame.setVisibility(View.VISIBLE);

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

        onlineList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        gameList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice);
        currentGamePeerList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        cardDeckAdapter = new CardDeckAdapter(this, new ArrayList<CardData>());
        czarPlayCards = new CzarDeckAdapter(this, new ArrayList<CardData>());

        online.setAdapter(onlineList);
        joinable.setAdapter(gameList);
        currentGamePeers.setAdapter(currentGamePeerList);
        cardDeck.setAdapter(cardDeckAdapter);
        playDeck.setAdapter(czarPlayCards);

        cardDeckAdapterManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        cardDeck.setLayoutManager(cardDeckAdapterManager);

        czarPlayCardsManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        playDeck.setLayoutManager(czarPlayCardsManager);
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
        }
    }

    public void openCzarView(final String cardText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gameLobbyFrame.setVisibility(View.GONE);
                gameFrame.setVisibility(View.GONE);
                czarGameFrame.setVisibility(View.VISIBLE);
            }
        });
    }

    public void updateHand(final CardData[] cards) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (CardData card: cards) {
                    if (!cardDeckAdapter.deck.contains(card)) {
                        cardDeckAdapter.deck.add(card);
                        System.out.println(card.getText());
                        cardDeckAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
    }

    public void openGameView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                gameLobbyFrame.setVisibility(View.GONE);
                czarGameFrame.setVisibility(View.GONE);
                gameFrame.setVisibility(View.VISIBLE);
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
    }

    public void gameLobbyStartClick(View view) {
        layer.startGame();
    }

    public void cardTest(View view) {
        splashFrame.setVisibility(View.GONE);
        gameFrame.setVisibility(View.VISIBLE);

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

    public void submitWhitecard(int index) {
        if (cardDeckAdapter.deck.size() < 10) return;
        long card = cardDeckAdapter.deck.get(index).getHash();
        cardDeckAdapter.deck.remove(index);
        cardDeckAdapter.notifyDataSetChanged();

        layer.playCard(card);
    }

    public void awardRound(long card) {
        czarPlayCards.deck.clear();

        // TODO award round
    }

    public void hostCancelButtonClick(View view) {
        hostFrame.setVisibility(View.GONE);
        lobbyFrame.setVisibility(View.VISIBLE);
    }
}
