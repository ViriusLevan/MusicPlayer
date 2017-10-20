package com.example.android.musicplayer;

/**
 * Created by ViB on 08/10/2017.
 */

public class Song {

    private long id;
    private String title;
    private String artist;

    public long getID(){return id;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}

    public Song(long songID, String songTitle, String songArtist) {
        id=songID;
        title=songTitle;
        artist=songArtist;
    }

}
