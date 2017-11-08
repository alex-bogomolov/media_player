package com.bogomolov.alexander.mediaplayer;

import android.app.NotificationManager;
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
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
    private MediaPlayerService player;
    boolean serviceBound = false;

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.bogomolov.alexander.mediaplayer.PlayNewAudio";
    public static final String BROADCAST_SEEK_TO = "com.bogomolov.alexander.mediaplayer.SeekTo";
    public static final String BROADCAST_APP_CONTROLS = "com.bogomolov.alexander.mediaplayer.AppControls";

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
            nowPlayed.setText(intent.getStringExtra("SONG_NAME"));
            if (!seekBarDragged) {
                songProgress.setMax(intent.getIntExtra("DURATION", 0));
                songProgress.setProgress(intent.getIntExtra("POSITION", 0));
            }
        }
    };

    private BroadcastReceiver playNewSongBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playAudio(intent.getIntExtra("POSITION", -1));
        }
    };

    private BroadcastReceiver playButtonTitleBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playPauseButton.setText(intent.getStringExtra("TITLE"));
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

    private Button previousButton;
    private Button playPauseButton;
    private Button nextButton;

    private Button orderButton;

    private PlayOrder playOrder;

    private boolean seekBarDragged;

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
        this.songProgress.setOnSeekBarChangeListener(this);

        loadAudio();

        this.songsAdapter = new SongsAdapter(this.audioList, getApplicationContext());
        this.recyclerView.setAdapter(songsAdapter);

        this.previousButton = findViewById(R.id.button_previous);
        this.playPauseButton = findViewById(R.id.button_pause_play);
        this.nextButton = findViewById(R.id.button_next);

        View.OnClickListener buttonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BROADCAST_APP_CONTROLS);
                int code = 0;
                switch (v.getId()) {
                    case R.id.button_previous:
                        code = 0;
                        break;
                    case R.id.button_pause_play:
                        code = 1;
                        break;
                    case R.id.button_next:
                        code = 2;
                        break;
                }
                intent.putExtra("ACTION_CODE", code);
                sendBroadcast(intent);
            }
        };

        this.previousButton.setOnClickListener(buttonListener);
        this.playPauseButton.setOnClickListener(buttonListener);
        this.nextButton.setOnClickListener(buttonListener);

        StorageUtil storage = new StorageUtil(getApplicationContext());
        this.playOrder = storage.loadOrder();

        this.orderButton = findViewById(R.id.order_button);

        switch (this.playOrder) {
            case Loop:
                this.orderButton.setText("Loop");
                break;
            case Shuffle:
                this.orderButton.setText("Shuffle");
                break;
        }

        this.orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (playOrder) {
                    case Loop:
                        orderButton.setText("Shuffle");
                        playOrder = PlayOrder.Shuffle;
                        new StorageUtil(getApplicationContext()).storeOrder(playOrder);
                        break;
                    case Shuffle:
                        orderButton.setText("Loop");
                        playOrder = PlayOrder.Loop;
                        new StorageUtil(getApplicationContext()).storeOrder(playOrder);
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(this.songNameBroadcastReceiver, new IntentFilter(MediaPlayerService.BROADCAST_NAME));
        registerReceiver(this.songProgressBroadcastReceiver, new IntentFilter(MediaPlayerService.BROADCAST_SEEK_BAR));
        registerReceiver(this.playNewSongBroadcastReceiver, new IntentFilter(SongsAdapter.PLAY_SONG_BROADCAST));
        registerReceiver(this.playButtonTitleBroadcastReceiver, new IntentFilter(MediaPlayerService.BROADCAST_PLAY_TITLE));
    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.songNameBroadcastReceiver);
        unregisterReceiver(this.songProgressBroadcastReceiver);
        unregisterReceiver(this.playNewSongBroadcastReceiver);
        unregisterReceiver(this.playButtonTitleBroadcastReceiver);
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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        this.seekBarDragged = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        this.seekBarDragged = false;
        Intent intent = new Intent(BROADCAST_SEEK_TO);
        intent.putExtra("PROGRESS", seekBar.getProgress());
        sendBroadcast(intent);

    }
}
