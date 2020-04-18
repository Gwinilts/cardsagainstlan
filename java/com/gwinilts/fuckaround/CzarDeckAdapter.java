package com.gwinilts.fuckaround;

import android.content.Context;
import androidx.cardview.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;


import java.util.Collections;
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

    @Override
    public void onBindViewHolder(final CardViewHolder holder, final int position) {
        final CardData c = deck.get(position);

        holder.text.setText("Long press to turn over!");
        holder.card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (c.isTurned()) {
                    app.awardRound(c.getHash());
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
