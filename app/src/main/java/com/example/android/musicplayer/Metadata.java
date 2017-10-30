package com.example.android.musicplayer;

import android.app.Activity;
import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;

/**
 * Created by ViB on 30/10/2017.
 */

public class Metadata extends Activity {

    ImageButton album_art;
    EditText title, artist, album, genre,
            trackNumber, composer, writer, bitrate;

    MediaMetadataRetriever metaRetriever;
    byte[] art;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata_);
        getInit();

        //Metadata retrieval

        //get id
        long currSong = getIntent().getLongExtra("trackID", 0);
        //might be redundant because metadata is extracted again
        String trackTitle = getIntent().getStringExtra("trackTitle");
        String trackArtist = getIntent().getStringExtra("trackArtist");
        //set uri
        Uri trackUri = ContentUris.withAppendedId(
                android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(getApplicationContext(), trackUri );

        try {
            art = metaRetriever.getEmbeddedPicture();
            Bitmap songImage = BitmapFactory
                    .decodeByteArray(art, 0, art.length);
            album_art.setImageBitmap(songImage);
        } catch (Exception e) {
            album_art.setBackgroundColor(Color.GRAY);
        }
        try {
            title.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        } catch (Exception e){
            title.setText(trackTitle);
        }
        try {
            album.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
        } catch (Exception e){
            album.setText("Unknown Album");
        }
        try {
            artist.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
        } catch (Exception e){
            artist.setText(trackArtist);
        }
        try {
            genre.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
        } catch (Exception e){
            genre.setText("Unknown Genre");
        }
        try {
            trackNumber.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER));
        } catch (Exception e){
            trackNumber.setText("Unknown Track Number");
        }
        try {
            composer.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER));
        } catch (Exception e){
            composer.setText("Unknown Composer");
        }
        try {
            writer.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER));
        } catch (Exception e){
            writer.setText("Unknown Writer");
        }
        try {
            bitrate.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
        } catch (Exception e){
            bitrate.setText("Unknown Bitrate");
        }


    }

    // Fetch Id's from xml
    public void getInit() {

        album_art = (ImageButton) findViewById(R.id.imgBtnAlbumArt);
        title = (EditText) findViewById(R.id.editTitle);
        artist = (EditText) findViewById(R.id.editArtist);
        album = (EditText) findViewById(R.id.editAlbum);
        genre = (EditText) findViewById(R.id.editGenre);
        trackNumber = (EditText) findViewById(R.id.editTrackNum);
        composer = (EditText) findViewById(R.id.editComposer);
        writer = (EditText) findViewById(R.id.editWriter);
        bitrate = (EditText) findViewById(R.id.editBitrate);

    }

    public void getMetadata(){//gets metadata from last.fm

        Caller.getInstance().setUserAgent("VirsagoBreak");
        Caller.getInstance().setDebugMode(true);

        String key = "13f5feb595f4b02698b17bb23b82c139";
        String user = "VirsagoBreak";
        String password = "Death's13";
        String secret = "6c2dffa86c3ef43b5a2138df8bcc2ac6";

        Session session = Authenticator.getMobileSession(user,password,key,secret);
        if(artist.getText().toString().equals("Unknown Artist")
                || artist.getText().toString().equals("<unknown>")){
            Collection<Track> matchingTracks = Track.search(title.getText().toString(),key);
            for (Track track : matchingTracks){

            }
        }
        else{
            Collection<Track> matchingTracks = Track.search(title.getText().toString(),
                    artist.getText().toString(), 25, key);
            for (Track track : matchingTracks){

            }
        }


    }

}
