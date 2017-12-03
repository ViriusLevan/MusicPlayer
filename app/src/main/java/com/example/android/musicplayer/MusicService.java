package com.example.android.musicplayer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import android.content.ContentUris;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.PowerManager;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
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
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener,
        MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener{

    //media player
    private MediaPlayer player;
    //song list
    private ArrayList<Song> songArrayList;
    private List<Song> songList, repeatList;
//    private Queue<Song> songQueue;
    private Stack<Song> songStack;
    //current position
    private int songPosn, rCount=0, resumePosition;

    //List of available Audio files
    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private Audio activeAudio; //an object of the currently playing audio


    private AudioManager audioManaer;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private final IBinder musicBind = new MusicBinder();

    private String songTitle= "", mediaFile;
    private static final int NOTIFY_ID=1;

    private boolean shuffle=false, ongoingCall = false;
    private byte repeat = 0;
    private Random rand;

//used to notify which action is triggered from the MediaSession callback listener.
    public static final String ACTION_PLAY = "com.example.android.musicplayer.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.android.musicplayer.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.example.android.musicplayer.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.example.android.musicplayer.ACTION_NEXT";
    public static final String ACTION_STOP = "com.example.android.musicplayer.ACTION_STOP";

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;

    public void onCreate(){

        songStack = new Stack<>();
        songList = new LinkedList<>();


        //create the service
        super.onCreate();
        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        //Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();

        //initialize position
        songPosn=0;
        //create player
        player = new MediaPlayer();
        rand=new Random();
    }

    public void initMusicPlayer(){
        player = new MediaPlayer();

        //set up media player event listeners
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);
        player.setOnPreparedListener(this);
        player.setOnBufferingUpdateListener(this);
        player.setOnSeekCompleteListener(this);
        player.setOnInfoListener(this);

        //set player properties
        player.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK);

//        player.setAudioAttributes(
//                new AudioAttributes.Builder()
//                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                .setUsage(AudioAttributes.USAGE_MEDIA)
//                .build());
        //Reset so that the MediaPlayer is not pointing to another data source
        player.reset();

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            // Set the data source to the mediaFile location
            player.setDataSource(activeAudio.getData());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        player.prepareAsync();
    }

//    public void setList(ArrayList<Song> theSongs){
//        songArrayList=theSongs;
//    }


    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

//    public void playSong(){
//        if(!player.isPlaying()){
//            player.start();
//        }
//
//        //play a song
//        player.reset();
//
//        //get song
//        //Song playSong = songQueue.peek();
//        Audio playSong = audioList.get(songPosn);
//        songStack.add(songList.remove(songPosn));
//        songTitle = playSong.getTitle();
//
//        //get id
//        long currSong = playSong.getID();
//        //set uri
//        Uri trackUri = ContentUris.withAppendedId(
//                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                currSong);
//
//        try{
//            player.setDataSource(getApplicationContext(), trackUri);
//        }
//        catch(Exception e){
//            Log.e("MUSIC SERVICE", "Error setting data source", e);
//        }
//        player.prepareAsync();
//    }
    public void playSong() {
        if (!player.isPlaying()) {
            player.start();
        }
    }

    public void pausePlayer(){
        if(player.isPlaying()){
            player.pause();
            resumePosition = player.getCurrentPosition();
            player.seekTo(resumePosition);
        }
    }

    public void resumeMedia(){
        if(!player.isPlaying()){
            player.seekTo(resumePosition);
            player.start();
        }
    }

    public void stopPlayer(){
        if(player == null)return;
        if(player.isPlaying()){
            player.stop();
        }
    }

    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pausePlayer();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (player != null) {
                            pausePlayer();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (player != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            if(){
//                return;
//            }
            //Get the new media index from SharedPreferences
            audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                Log.d("Broadcast Receiver","AAAAAAA");
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }

            //A PLAY_NEW_AUDIO action received
            //reset mediaPlayer to play the new Audio
            stopPlayer();
            player.reset();
            initMusicPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };

    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                super.onPause();
                pausePlayer();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),
                R.drawable.music); //replace with medias albumArt
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
                .build());
    }

    public void skipToNext() {

        if (audioIndex == audioList.size() - 1) {
            //if last in playlist
            if(shuffle){
                audioIndex = rand.nextInt(audioList.size()-2);
            }else{
                if(repeat == 2){
                    repeat=0;
                    audioIndex = 0;
                }else{
                    audioIndex = 0;
                    new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
                    stopPlayer();
                    return;
                }
            }
            activeAudio = audioList.get(audioIndex);
        } else {
            //get next in playlist
            if(shuffle){
                int check = audioIndex;
                while(check == audioIndex){
                    check = rand.nextInt(audioList.size()-1);
                }
                audioIndex = check;
                activeAudio = audioList.get(check);
            }else{
                activeAudio = audioList.get(++audioIndex);
            }
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopPlayer();
        //reset mediaPlayer
        player.reset();
        initMusicPlayer();
    }

    public void skipToPrevious() {

        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get previous in playlist
            activeAudio = audioList.get(--audioIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopPlayer();
        //reset mediaPlayer
        player.reset();
        initMusicPlayer();
    }

    public enum PlaybackStatus {
        PLAYING,
        PAUSED
    }

    private void buildNotification(PlaybackStatus playbackStatus) {

        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.music); //replace with your own image

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show our playback controls in the compact notification view.
                        .setShowActionsInCompactView(0, 1, 2))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimary))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
                .setContentText(activeAudio.getArtist())
                .setContentTitle(activeAudio.getAlbum())
                .setContentInfo(activeAudio.getTitle())
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous,
                        "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(android.R.drawable.ic_media_next,
                        "next", playbackAction(2));

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).
                notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MusicService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }


//    public void setSong(int songIndex){
//        songPosn=songIndex;
//        //resetting
//        songList.clear();
//        songStack.clear();
//        //create a song queue everytime the user touches a song
//        for (int i = 0; i < songArrayList.size(); i++){
//            if(i != songPosn){
//                songList.add(songArrayList.get(i));
//            }
//        }
//        songList.add(songArrayList.get(songPosn));
//        songPosn = songList.size()-1;
//        if(repeat == 2){
//            repeatList = songList;
//        }
//    }

    public int getPosn(){
        return player.getCurrentPosition();
    }

    public int getDur(){
        return player.getDuration();
    }

    public boolean isPlaying(){
        return player.isPlaying();
    }

    public void seek(int posn){
        player.seekTo(posn);
    }

    public void loadNewAudioList(){
        StorageUtil storage = new StorageUtil(getApplicationContext());
        audioList = storage.loadAudio();
    }

//    public void playPrev(){
//        if (songStack.size()>1){
//            songList.add(songStack.pop());
//            songPosn = songList.size();
//            songList.add(songStack.pop());
//            playSong();
//        }else{
//            player.stop();
//        }
//        /*songPosn--;
//        if(songPosn<0) songPosn=songList.size()-1;*/
//    }
//
//    //skip to next
//    public void playNext(){
//        if(songList.isEmpty()){
//            if(repeat == 2){
//                songList = repeatList;
//                songPosn = 0;
//                playSong();
//            }else{
//                player.stop();
//            }
//        }
//        else if(shuffle){
//            int newSong = songPosn;
//            /*while(newSong==songPosn){
//                newSong=rand.nextInt(songList.size());
//            }*/
//            newSong = rand.nextInt(songList.size()-1);
//            songPosn=newSong;
//            playSong();
//        }
//        else{
//            /*songPosn++;
//            if(songPosn>=songArrayList.size()) songPosn=0;*/
//            songPosn = 0;
//            playSong();
//            /*if(repeat == 1){
//                if(rCount == 0){
//                    songPosn = songList.size();
//                    songList.add(songStack.pop());
//                    rCount++;
//                    playSong();
//                }else{
//                    songPosn = songList.size()-1;
//                    rCount=0;
//                    playSong();
//                }
//            }*/
//        }
//
//    }

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

    public Song getSong(int songIndex){
        if(songArrayList.get(songIndex) == null){
            return null;
        }
        return (songArrayList.get(songIndex));
    }

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
        //Invoked when playback of a media source has completed.
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
            skipToNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation.
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error",
                        "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        mp.reset();
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) { //Invoked when the media source is ready for playback.
        //start playback
        //playSong();
        playSong();
        //mp.start();

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
        super.onDestroy();
        if (player != null) {
            stopPlayer();
            player.release();
        }
        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();

        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);

        //clear cached playlist
        new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();

        //stopForeground(true);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        //Invoked when the audio focus of the system is updated.
        switch(focusChange){
            case AudioManager.AUDIOFOCUS_GAIN:
                //the service gained audio focus
                if(player == null) initMusicPlayer();
                else if(!player.isPlaying())
                    player.start();
                player.setVolume(1.0f,1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                //service lost audio focus, user has probably moved
                //to playing media on another app
                if(player.isPlaying())
                    player.stop();
                player.release();
                player = null;
                break;
            case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                //focus lost for a short time
                if(player.isPlaying())
                    player.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                //focus lost for a short time, probably from a notification
                if(player.isPlaying())
                    player.setVolume(0.1f,0.1f);
                else if (!player.isPlaying())
                    player.stop();
                break;
        }
    }

    private boolean requestAudioFocus(){
        audioManaer = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int result = audioManaer.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(result ==  AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            //focus gained
            return true;
        }else{//could not gain focus
            return false;
        }
    }

    private boolean removeAudioFocus(){
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManaer.abandonAudioFocus(this);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //Invoked to communicate some info.
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation.
    }

    //The system calls this method when an activity, requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            //Load data from SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();
            Log.d("audioIndex",Integer.toString(audioIndex));
            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                Log.d("On Start Command","activeAudio GOT");
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMusicPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

}
