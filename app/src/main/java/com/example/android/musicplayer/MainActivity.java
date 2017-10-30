package com.example.android.musicplayer;

import android.Manifest;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.DrawableRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.RelativeLayout;


public class MainActivity extends AppCompatActivity implements MediaPlayerControl {

    private ArrayList<Song> songList;
    private ListView songView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;
    private MusicController controller;
    private boolean paused=false, playbackPaused=false;
    private MenuItem repeat, shuffle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

                // READ_EXTERNAL_STORAGE is an app-defined int constant

                return;
            }
        }

        songView = (ListView)findViewById(R.id.song_list);
        songList = new ArrayList<Song>();

        getSongList();

        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);


        //You need a reference to a layout element
        RelativeLayout RLM = (RelativeLayout) findViewById(R.id.RLMain);

        ViewTreeObserver vto = RLM.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new  ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                //Call your controller set-up now that the layout is loaded
                setController();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex//doesn't work as there is no getBitmap
                    (MediaStore.Audio.Media.ALBUM);//probably have to switch to MetadataRetriever
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
    }

    private void setController(){
        //set the controller up
        controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.song_list));
        controller.setEnabled(true);
        controller.show();
    }

    //play next
    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    //play previous
    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //menu item selected
        switch (item.getItemId()) {
            case R.id.action_search:
                //search
                break;
            case R.id.action_repeat:
                musicSrv.toggleRepeat();

                /*repeat = (MenuItem) findViewById(R.id.action_repeat);
                if(musicSrv.getRepeat() == 0){
                    repeat.setIcon(R.drawable.repeat_none);
                }else if(musicSrv.getRepeat() == 0){
                    repeat.setIcon(R.drawable.repeat_once);
                }else {
                    repeat.setIcon(R.drawable.repeat_all);
                }*/
                break;
            case R.id.action_shuffle:
                musicSrv.setShuffle();

                /*shuffle = (MenuItem) findViewById(R.id.action_shuffle);
                if(musicSrv.getShuffle()){
                    shuffle.setIcon(R.drawable.shuffle_on);
                }else{
                    shuffle.setIcon(R.drawable.shuffle_off);
                }*/
                break;
            case R.id.action_close:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (musicBound) unbindService(musicConnection);
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    @Override
    public void start() {
        musicSrv.go();
    }

    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
            return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
            return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    protected void goToMetadataEdit(View view){
        Intent i = new Intent(this, Metadata.class);
        //try to use same method as songPicked(), probably need to tag the button,
        // i mean if it CAN be tagged, also need to change parameter if it isn't a view
        Song selected = musicSrv.getSong(Integer.parseInt(view.getTag().toString()));
        if(selected !=null){
            long id = selected.getID();
            String title = selected.getTitle();
            String artist = selected.getArtist();
            if(id == -1){
                //do nothing
                //stopgap measure till i find a better method to pass
                //it's probably better now, but i haven't tested it yet
            }else{
                i.putExtra("trackID", id);
                i.putExtra("trackTitle", title);
                i.putExtra("trackArtist", artist);
            }
        }
    }

}
