package com.example.android.musicplayer;

import android.app.Activity;
import android.content.ContentUris;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;

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
            trackNumber, composer, writer;
    Button saveButton, getMetaButton, resetButton;

    MediaMetadataRetriever metaRetriever;

    ListView lvMetadata;
    ArrayList<Metadata> arrmd= new ArrayList<>();
    ResultMetadataAdapter adapter;
    TextView result= (TextView) findViewById(R.id.textResult);

    byte[] art;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metadata_);
        //Declarations
        getInit();
        //Metadata retrieval
        //get id
        long currSong = Long.parseLong(getIntent().getStringExtra("trackID"));
        //set uri
        final Uri trackUri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                currSong);

        metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(getApplicationContext(), trackUri );
        setDefaultValues();


        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            setDefaultValues();
            }
        });

        lvMetadata=(ListView) findViewById(R.id.listResult);
        adapter= new ResultMetadataAdapter(this,arrmd);
        lvMetadata.setAdapter(adapter);

        getMetaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                result.setText("Result");
                //got lost here too :(    alv
                //Metadata metadata= new Metadata();
                //arrmd(metadata);
                //^^^^that must br wrong, dun know lel
                adapter.notifyDataSetChanged();
                getMetadata();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //MEDIA METADATA DOESNT WORK, HAVE TO USE MyID3

//                Intent previousScreen = new Intent(getApplicationContext(), MainActivity.class);
//                previousScreen.putExtra("Title", title.getText().toString());
//                previousScreen.putExtra("Artist", artist.getText().toString());
//                previousScreen.putExtra("Album", album.getText().toString());
//                previousScreen.putExtra("Genre", genre.getText().toString());
//                previousScreen.putExtra("Composer", composer.getText().toString());
//                previousScreen.putExtra("Writer", writer.getText().toString());


//                previousScreen.putExtra("Track Number", Long.parseLong(tNum));


                File src = new File(trackUri.getPath());
                MusicMetadataSet src_set = null;
                try {
                    src_set = new MyID3().read(src);
                }catch (IOException e1) {
                    e1.printStackTrace();
                }
                MusicMetadata metaToWrite;
                if (src_set != null)
                {
                    // The source file DID have an ID3v1 or ID3v2 tag (or both).
                    // We'll update those values.
                    metaToWrite = src_set.merged;
                    Log.i("SRC SET ALBUM",src_set.merged.getAlbum());
                    Log.i("SRC SET ARTIST",src_set.merged.getArtist());
                    Log.i("SRC SET TITLE",src_set.merged.getSongTitle());
                    Log.i("SRC SET COMPOSER",src_set.merged.getComposer());
                    Log.i("SRC SET GENRE",src_set.merged.getGenre());
                } else
                {
                    Log.wtf("SET","IS FUCKING NULL");
                    // The file did not have an ID3v1 or ID3v2 tag, so
                    // we need to add new tag(s).
                    metaToWrite = MusicMetadata.createEmptyMetadata();
                }

                metaToWrite.setSongTitle(title.getText().toString());
                metaToWrite.setArtist(artist.getText().toString());
                metaToWrite.setAlbum(album.getText().toString());
                metaToWrite.setGenre(genre.getText().toString());
                metaToWrite.setComposer(composer.getText().toString());
                String tNum = trackNumber.getText().
                        toString().replaceAll("[^\\d.]", "");
                if(tNum.length()!=0){
                    metaToWrite.setTrackNumber(Integer.parseInt(tNum));
                }
                if(album_art.getDrawingCache() !=null){
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    album_art.getDrawingCache().compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
//                    previousScreen.putExtra("Album Art", byteArray);
                }


                String name = src.getName();
                if (name.toLowerCase().endsWith(".mp3"))
                    name = name.substring(0, name.length() - 4);
                name += ".1.mp3";
                File dst = new File(src.getParentFile(), name);
                try {
                    new MyID3().write(src,
                            dst,
                            src_set,
                            metaToWrite);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (ID3WriteException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }  // write updated metadata

                finish();


            }
        });

    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void setDefaultValues(){

        //might be redundant because metadata is extracted again
        String trackTitle = getIntent().getStringExtra("trackTitle");
        String trackArtist = getIntent().getStringExtra("trackArtist");
        try {
            art = metaRetriever.getEmbeddedPicture();
            Bitmap songImage = BitmapFactory
                    .decodeByteArray(art, 0, art.length);
            album_art.setImageBitmap(songImage);
        } catch (Exception e) {
            album_art.setBackgroundColor(Color.GRAY);
        }

        if(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)== null){
            title.setText(trackTitle);
        }else{
            title.setText
                    (metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        }
        if(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) != null){
            artist.setText(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
        }else{
            artist.setText
                    (trackArtist);
        }

        if(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) != null){
            album.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM));
        } else {
            album.setText("Unknown Album");
        }
        if(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE) != null){
            genre.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE));
        }else{
            genre.setText("Unknown Genre");
        }
        if(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER) != null) {
            trackNumber.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER));
        } else {
            trackNumber.setText("Unknown Track Number");
        }
        if(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER) != null) {
            composer.setText(metaRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER));
        } else{
            composer.setText("Unknown Composer");
        }
//        if(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER) != null) {
//            writer.setText(metaRetriever
//                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER));
//        } else {
//            writer.setText("Unknown Writer");
//        }
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
        //writer = (EditText) findViewById(R.id.editWriter);

        saveButton = findViewById(R.id.buttonSave);
        getMetaButton = findViewById(R.id.buttonGetMeta);
        resetButton = findViewById(R.id.buttonReset);
    }

    public void getMetadata(){//gets metadata from last.fm
        ///DONT EVER FUCKING DO THIS IN A REAL APP
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
        //EVER

       GetResult ass = new GetResult();
        ass.execute();
    }

    private class GetResult extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] params) {
            Caller.getInstance().setUserAgent("VirsagoBreak");
            Caller.getInstance().setDebugMode(true);

            String key = "13f5feb595f4b02698b17bb23b82c139";
            String user = "VirsagoBreak";
            String password = "Death's13";
            String secret = "6c2dffa86c3ef43b5a2138df8bcc2ac6";

            Session session = Authenticator.getMobileSession(user,password,key,secret);
            if(artist.getText().toString().equals("Unknown Artist")
                    || artist.getText().toString().equals("<unknown>")){//if artist name is unknown
                Collection<Track> matchingTracks = Track.search(title.getText().toString(),key);//Search based on title only
                if(matchingTracks.isEmpty())Log.i("Matching Tracks","No Results");
                for (Track track : matchingTracks){
                    //print
                    Log.i("Result",track.getArtist());
                }
            }
            else{//artist name is known
                Collection<Track> matchingTracks = Track.search(
                        artist.getText().toString(), title.getText().toString(),
                        25, key);//search based on title, string, and limit results to 25
                if(matchingTracks.isEmpty())Log.i("Matching Tracks","No Results");
                for (Track track : matchingTracks){
                    //print
                    Log.i("Result",track.getArtist());

                }
            }
            return "ass";
        }

        @Override
        protected void onPostExecute(String message) {
            //process message
        }
    }


}