package com.example.android.musicplayer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


/**
 * Created by ViB on 08/10/2017.
 */

public class SongAdapter extends BaseAdapter {

    private ArrayList<Audio> songs;
    private LayoutInflater songInf;

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int arg0) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //map to song layout
        RelativeLayout songLay = (RelativeLayout) songInf.inflate
                (R.layout.song, parent, false);
        //get title and artist views
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);
        //get song using position
        Audio currSong = songs.get(position);
        //get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());
        //set position as tag
        songLay.setTag(position);

        //spinner tag example, dunno if this works
        /*settingsSpinner.setTag("bg_color_spinner");
        settingsSpinner.setAdapter(new BackgroundColorAdapter());*/

        return songLay;
    }

    public SongAdapter(Context c, ArrayList<Audio> theSongs){
        songs=theSongs;
        songInf=LayoutInflater.from(c);
    }


}
