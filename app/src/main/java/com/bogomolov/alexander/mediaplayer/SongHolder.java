package com.bogomolov.alexander.mediaplayer;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by admin on 07.11.2017.
 */

public class SongHolder extends RecyclerView.ViewHolder {
    public TextView nameTextView;
    public TextView artistTextView;
    public Button playButton;

    public SongHolder(LinearLayout v) {
        super(v);

        this.nameTextView = v.findViewById(R.id.song_name);
        this.artistTextView = v.findViewById(R.id.song_artist);
        this.playButton = v.findViewById(R.id.song_play);
    }
}
