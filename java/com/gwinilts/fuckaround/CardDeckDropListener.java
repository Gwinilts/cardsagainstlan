package com.gwinilts.fuckaround;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.DragEvent;
import android.view.View;

public class CardDeckDropListener implements View.OnDragListener {
    private CardDropCallback onDrop;

    public CardDeckDropListener(CardDropCallback o) {
        super();
        onDrop = o;
    }

    public boolean onDrag(View v, DragEvent event) {
        Game g = Game.get();

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DRAG_STARTED: {
                v.getBackground().setColorFilter(Color.parseColor("#B7B2B0"), PorterDuff.Mode.MULTIPLY);
                v.invalidate();
                return true;
            }
            case DragEvent.ACTION_DRAG_ENTERED: {
                v.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.XOR);
                v.invalidate();
                return true;
            }
            case DragEvent.ACTION_DRAG_LOCATION: {
                return true;
            }
            case DragEvent.ACTION_DRAG_ENDED: {
                v.getBackground().clearColorFilter();
                v.invalidate();
                return true;
            }
            case DragEvent.ACTION_DROP: {
                CharSequence drop = event.getClipData().getItemAt(0).getText();
                int i = drop.charAt(0);
                String text = drop.toString().substring(1);

                if (onDrop != null) onDrop.run(text, i);
                return true;
            }
        }
        return false;
    }
}
