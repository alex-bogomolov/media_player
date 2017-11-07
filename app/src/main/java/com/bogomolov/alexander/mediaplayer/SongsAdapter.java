package com.bogomolov.alexander.mediaplayer;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;

/**
 * Created by admin on 07.11.2017.
 */

public class SongsAdapter extends RecyclerView.Adapter<SongHolder> {
    private ArrayList<Audio> songs;
    private Context context;

    public static final String PLAY_SONG_BROADCAST = "com.bogomolov.alexander.mediaplayer.PlaySong";

    public SongsAdapter(ArrayList<Audio> songs, Context context) {
        this.songs = songs;
        this.context = context;
    }

    @Override
    public SongHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout linearLayout = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.song_holder, parent, false);
        return new SongHolder(linearLayout);
    }

    @Override
    public void onBindViewHolder(final SongHolder holder, final int position) {
        Audio song = this.songs.get(position);

        holder.artistTextView.setText(song.getArtist());
        holder.nameTextView.setText(song.getTitle());

        holder.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PLAY_SONG_BROADCAST);
                intent.putExtra("POSITION", holder.getAdapterPosition());
                context.sendBroadcast(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }
}
