package com.example.android.musicplayer;

import android.app.NotificationChannel;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

import android.app.Notification;
import android.app.PendingIntent;


/**
 * Created by ViB on 14/10/2017.
 */

public class MusicService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener{

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songArrayList;
    private List<Song> songList, repeatList;
//    private Queue<Song> songQueue;
    private Stack<Song> songStack;
    //current position
    private int songPosn, rCount=0;

    private final IBinder musicBind = new MusicBinder();

    private String songTitle= "";
    private static final int NOTIFY_ID=1;

    private boolean shuffle=false;
    private byte repeat = 0;
    private Random rand;

    public void onCreate(){
        songStack = new Stack<>();
        songList = new LinkedList<>();

        //create the service
        super.onCreate();
        //initialize position
        songPosn=0;
        //create player
        player = new MediaPlayer();

        rand=new Random();

        initMusicPlayer();
    }

    public void initMusicPlayer(){
        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);

        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes aat1 = (new AudioAttributes.Builder())
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build();
            player.setAudioAttributes(aat1);
        }else{
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }

        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

    }

    public void setList(ArrayList<Song> theSongs){
        songArrayList=theSongs;
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    public void playSong(){
        //play a song
        player.reset();

        //get song
        //Song playSong = songQueue.peek();
        Song playSong = songList.get(songPosn);
        songStack.add(songList.remove(songPosn));
        songTitle = playSong.getTitle();

        //get id
        long currSong = playSong.getID();
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        try{
            player.setDataSource(getApplicationContext(), trackUri);
        }
        catch(Exception e){
            Log.e("MUSIC SERVICE", "Error setting data source", e);
        }
        player.prepareAsync();
    }

    public void setSong(int songIndex){
        songPosn=songIndex;
        //resetting
        songList.clear();
        songStack.clear();
        //create a song queue everytime the user touches a song
        for (int i = 0; i < songArrayList.size(); i++){
            if(i != songPosn){
                songList.add(songArrayList.get(i));
            }
        }
        songList.add(songArrayList.get(songPosn));
        songPosn = songList.size()-1;
        if(repeat == 2){
            repeatList = songList;
        }
    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPng(){
        return player.isPlaying();
    }

    public void pausePlayer(){
        player.pause();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void go(){
        player.start();
    }

    public void playPrev(){
        if (songStack.size()>1){
            songList.add(songStack.pop());
            songPosn = songList.size();
            songList.add(songStack.pop());
            playSong();
        }else{
            player.stop();
        }
        /*songPosn--;
        if(songPosn<0) songPosn=songList.size()-1;*/
    }

    //skip to next
    public void playNext(){
        if(songList.isEmpty()){
            if(repeat == 2){
                songList = repeatList;
                songPosn = 0;
                playSong();
            }else{
                player.stop();
            }
        }
        else if(shuffle){
            int newSong = songPosn;
            /*while(newSong==songPosn){
                newSong=rand.nextInt(songList.size());
            }*/
            newSong = rand.nextInt(songList.size()-1);
            songPosn=newSong;
            playSong();
        }
        else{
            /*songPosn++;
            if(songPosn>=songArrayList.size()) songPosn=0;*/
            songPosn = 0;
            playSong();
            /*if(repeat == 1){
                if(rCount == 0){
                    songPosn = songList.size();
                    songList.add(songStack.pop());
                    rCount++;
                    playSong();
                }else{
                    songPosn = songList.size()-1;
                    rCount=0;
                    playSong();
                }
            }*/
        }

    }

    public void setShuffle(){
        if(shuffle) shuffle=false;
        else shuffle=true;
    }


    public boolean getShuffle(){return shuffle;}

    public void toggleRepeat (){
        if(repeat>1){
            repeat = 0;
        }else{
            repeat++;
        }
    }

    public byte getRepeat(){return  repeat;}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public boolean onUnbind(Intent intent){
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if(repeat == 1){
            while (rCount == 0){
                rCount++;
                player.seekTo(0);
                player.start();
            }
            rCount=0;
        }
        if(player.getCurrentPosition()>0){
            mp.reset();
            playNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //start playback
        mp.start();

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder builder = new
                    Notification.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID);

            builder.setContentIntent(pendInt)
                    .setSmallIcon(R.drawable.play)
                    .setTicker(songTitle)
                    .setOngoing(true)
                    .setContentTitle("Playing")
                    .setContentText(songTitle);
            Notification not = builder.build();

            startForeground(NOTIFY_ID, not);
        }else {
            Notification.Builder builder = new Notification.Builder(this);

            builder.setContentIntent(pendInt)
                    .setSmallIcon(R.drawable.play)
                    .setTicker(songTitle)
                    .setOngoing(true)
                    .setContentTitle("Playing")
                    .setContentText(songTitle);
            Notification not = builder.build();

            startForeground(NOTIFY_ID, not);
        }

    }

    @Override
    public void onDestroy() {
        player.stop();
        player.release();
        stopForeground(true);
    }

    public Song getSong(int songIndex){
        if(songArrayList.get(songIndex) == null){
            return null;
        }
        return (songArrayList.get(songIndex));
    }
}
