package com.gwinilts.fuckaround;

import androidx.cardview.widget.CardView;

import android.content.ClipData;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class CzarDeckAdapter extends RecyclerView.Adapter<CzarDeckAdapter.CardViewHolder> {
    class CardViewHolder extends RecyclerView.ViewHolder {

        TextView text;
        CardView card;

        public CardViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.cardText);
            card = itemView.findViewById(R.id.card);
        }

    }

    private LayoutInflater inflater;
    ArrayList<CardData> deck;
    private MainActivity app;

    public CzarDeckAdapter(MainActivity context, ArrayList<CardData> deck) {
        inflater = LayoutInflater.from(context);
        this.deck = deck = new ArrayList<CardData>();
        this.app = context;
    }

    @Override
    public CzarDeckAdapter.CardViewHolder onCreateViewHolder(ViewGroup parent, int vType) {
        View view = inflater.inflate(R.layout.whitecard, parent, false);
        CardViewHolder holder = new CardViewHolder(view);

        return holder;
    }

    public boolean hasCard(long card) {
        for (CardData c: deck) {
            if (c.getHash() == card) return true;
        }
        return false;
    }

    public void shuffle() {
        CardData[] cards = deck.toArray(new CardData[0]);

        Shuffeler rng = new Shuffeler(cards.length);

        CardData tmp;
        int index;


        for (int i = 0; i < cards.length; i++) {
            index = rng.get();

            tmp = cards[i];
            cards[i] = cards[index];
            cards[index] = tmp;
        }

        deck.clear();

        for (CardData card: cards) {
            deck.add(card);
        }
    }

    @Override
    public void onBindViewHolder(final CardViewHolder holder, final int position) {
        final CardData c = deck.get(position);

        holder.text.setText("Long press to turn over!");
        holder.card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (c.isTurned()) {
                    //app.awardRound(c.getHash());
                    Game g = Game.get();

                    if (g.isAwarded()) return false;

                    String d = ((char)position) + deck.get(position).getText();

                    ClipData data = ClipData.newPlainText("card-data", d);
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);

                    v.startDrag(data, shadowBuilder, v, 0);
                    return true;
                } else {
                    c.turnOver();
                    holder.text.setText(c.getText());
                }
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return deck.size();
    }


}
