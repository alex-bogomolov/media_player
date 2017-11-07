package com.bogomolov.alexander.mediaplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private MediaPlayerService player;
    boolean serviceBound = false;

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.bogomolov.alexander.mediaplayer.PlayNewAudio";

    ArrayList<Audio> audioList;

    private RecyclerView recyclerView;
    private SongsAdapter songsAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private BroadcastReceiver songNameBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String songName = intent.getStringExtra("SONG_NAME");
            nowPlayed.setText(songName);
        }
    };

    private BroadcastReceiver songProgressBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            songProgress.setMax(intent.getIntExtra("DURATION", 0));
            songProgress.setProgress(intent.getIntExtra("POSITION", 0));
        }
    };

    private BroadcastReceiver playNewSongBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playAudio(intent.getIntExtra("POSITION", -1));
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private TextView nowPlayed;
    private SeekBar songProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.recyclerView = (RecyclerView) findViewById(R.id.songs_recycler_view);
        this.recyclerView.setHasFixedSize(true);
        this.layoutManager = new LinearLayoutManager(this);
        this.recyclerView.setLayoutManager(this.layoutManager);

        this.nowPlayed = findViewById(R.id.played_song);
        this.songProgress = findViewById(R.id.song_progress);

        loadAudio();

        this.songsAdapter = new SongsAdapter(this.audioList, getApplicationContext());
        this.recyclerView.setAdapter(songsAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(this.songNameBroadcastReceiver, new IntentFilter(MediaPlayerService.BROADCAST_NAME));
        registerReceiver(this.songProgressBroadcastReceiver, new IntentFilter(MediaPlayerService.BROADCAST_SEEK_BAR));
        registerReceiver(this.playNewSongBroadcastReceiver, new IntentFilter(SongsAdapter.PLAY_SONG_BROADCAST));
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.songNameBroadcastReceiver);
        unregisterReceiver(this.songProgressBroadcastReceiver);
        unregisterReceiver(this.playNewSongBroadcastReceiver);
    }

    public void playAudio(int audioIndex) {
        if (!serviceBound) {
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(audioList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            if (serviceBound) {
                unbindService(serviceConnection);
                player.stopSelf();
            }
        }
    }

    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                audioList.add(new Audio(data, title, album, artist));
            }
            cursor.close();
        }
    }
}
