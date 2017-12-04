package com.example.android.musicplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

/**
 * Created by ViB on 04/12/2017.
 */

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION=1;
    private static final String DATABASE_NAME = "ContactDB";

    private static final String CREATE_TABLE_AUDIO =
            "CREATE TABLE Audio (" +
                    "ID INTEGER PRIMARY KEY, " +
                    "Title VARCHAR(100), " +
                    "Album VARCHAR(100), " +
                    "Artist VARCHAR(100), " +
                    "Data VARCHAR(100)," +
                    "MID VARCHAR(100))";//MediaStore.Audio.Media._ID
    private static final String CREATE_TABLE_PLAYLIST =
            "CREATE TABLE Playlist (" +
                    "ID INTEGER PRIMARY KEY, " +
                    "Name VARCHAR(100))";
    private static final String CREATE_TABLE_PLAYLIST_HAS_AUDIO =
            "CREATE TABLE Playlist_Has_Audio " +
                    "(PID INTEGER, " +
                    "AID INTEGER, " +
                    "FOREIGN KEY (PID) REFERENCES Playlist(ID)," +
                    "FOREIGN KEY (AID) REFERENCES Audio(ID),"+
                    "PRIMARY KEY (PID,AID))";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_AUDIO);
        db.execSQL(CREATE_TABLE_PLAYLIST);
        db.execSQL(CREATE_TABLE_PLAYLIST_HAS_AUDIO);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {

    }

    public void addToPlaylist(Audio newAudio, String playlistName)
    {

        SQLiteDatabase dbr = this.getReadableDatabase();
        SQLiteDatabase dbw = this.getWritableDatabase();
        //query the playlist ID
        String[] column ={"ID"};
        String[] wArgs = {playlistName};
        Cursor cursor = dbr.query("Playlist",
                column,"Name = ? ",
                wArgs,null, null, null, "1");
        Integer pID = cursor.getInt(cursor.getColumnIndex("ID"));
        cursor.close();

        //if query for audio is empty, add audio first, then add audio to playlist_has_audio
        String[]aCol = {"ID"};
        String[]awArgs = {newAudio.getTitle(), newAudio.getArtist(), newAudio.getData()};
        Cursor aCur = dbr.query("Audio",
                column, "Title = ? AND Artist = ? AND Data = ?",
                awArgs, null, null, null, "1");
        if(aCur.moveToFirst()){//query is empty
            ContentValues values = new ContentValues();
            values.put("Title", newAudio.getTitle());
            values.put("Album", newAudio.getAlbum());
            values.put("Artist", newAudio.getArtist());
            values.put("Data", newAudio.getData());
            values.put("MID", newAudio.getId());
            dbw.insert("Audio", null, values);
        }
        aCur.close();
        Cursor bCur = dbr.query("Audio",
                column, "Title = ? AND Artist = ?",
                awArgs, null, null, null, "1");
        Integer aID = bCur.getInt(bCur.getColumnIndex("ID"));
        bCur.close();

        dbr.close();
        String sql = "INSERT INTO Playlist_Has_Audio(PID,AID) VALUES ("+ pID +" , "+ aID +") ";
        dbw.execSQL(sql);
    }

    public ArrayList<String> getAllPlaylist(){

        SQLiteDatabase db = this.getReadableDatabase();
        String[] column = {"Name"};
        Cursor cursor = db.query("Playlist", column, null,
                null, null, null, null);
        ArrayList<String> plList = new ArrayList<String>();
        while(cursor.moveToNext())
        {
            plList.add(cursor.getString(cursor.getColumnIndex("Name")));
        }
        cursor.close();
        db.close();

        return plList;
    }

    public ArrayList<Audio> getAllAudioFromPlaylist(String playlistName)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] column = {"Title", "Album", "Artist", "Data", "MID"};
        String[] wArgs = {playlistName};
        Cursor cursor = db.query("Audio, Playlist_Has_Audio, Playlist",
                column,
                "Playlist.Name = ? " +
                        "AND Playlist_Has_Audio.PID = Playlist.ID" +
                        "AND Playlist_Has_Audio.AID = Audio.ID",
                wArgs,
                null, null, null);
        ArrayList<Audio> audioList = new ArrayList<Audio>();
        while(cursor.moveToNext())
        {
            Audio audio = new Audio(
                        cursor.getString(cursor.getColumnIndex("Data")),
                        cursor.getString(cursor.getColumnIndex("Title")),
                        cursor.getString(cursor.getColumnIndex("Album")),
                        cursor.getString(cursor.getColumnIndex("Artist")),
                        cursor.getString(cursor.getColumnIndex("MID"))
                    );
            audioList.add(audio);
        }
        cursor.close();
        db.close();
        return audioList;
    }

    public void deleteFromPlaylist(Audio audio, String playlistName)
    {
        SQLiteDatabase dbr = this.getReadableDatabase();
        //query the playlist ID
        String[] column ={"ID"};
        String[] wArgs = {playlistName};
        Cursor cursor = dbr.query("Playlist",
                column,"Name = ? ",
                wArgs,null, null, null, "1");
        Integer pID = cursor.getInt(cursor.getColumnIndex("ID"));
        cursor.close();

        //query the audio ID
        String[]aCol = {"ID"};
        String[]awArgs = {audio.getTitle(), audio.getArtist()};
        Cursor aCur = dbr.query("Audio",
                column, "Title = ? AND Artist = ?",
                awArgs, null, null, null, "1");
        Integer aID = aCur.getInt(aCur.getColumnIndex("ID"));
        aCur.close();

        SQLiteDatabase db = this.getWritableDatabase();
        String whereField = "AID = ? AND PID = ?";
        String[] whereValues = {String.valueOf(aID), String.valueOf(pID)};
        db.delete("Playlist_Has_Audio", whereField, whereValues);
        db.close();
    }
}