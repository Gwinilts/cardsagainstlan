package com.gwinilts.fuckaround;

import androidx.cardview.widget.CardView;

import android.content.ClipData;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class CardDeckAdapter extends RecyclerView.Adapter<CardDeckAdapter.CardViewHolder> {
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

    public CardDeckAdapter(MainActivity context, ArrayList<CardData> deck) {
        inflater = LayoutInflater.from(context);
        this.deck = deck = new ArrayList<CardData>();
        this.app = context;
    }

    public void addIfUnique(CardData c) {
        for (CardData card: deck) {
            if (card.getHash() == c.getHash()) return;
        }
        deck.add(c);
    }

    @Override
    public CardDeckAdapter.CardViewHolder onCreateViewHolder(ViewGroup parent, int vType) {
        View view = inflater.inflate(R.layout.whitecard, parent, false);
        CardViewHolder holder = new CardViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(final CardViewHolder holder, final int position) {
        holder.text.setText(deck.get(position).getText());
        holder.card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Game g = Game.get();
                if (g.isPlayed()) return false;

                String d = ((char)position) + deck.get(position).getText();

                ClipData data = ClipData.newPlainText("card-data", d);
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDrag(data, shadowBuilder, v, 0);
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return deck.size();
    }


}
