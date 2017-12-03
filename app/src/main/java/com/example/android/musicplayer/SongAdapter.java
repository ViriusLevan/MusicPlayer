package com.example.android.musicplayer;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;


/**
 * Created by ViB on 08/10/2017.
 */

public class SongAdapter extends BaseAdapter {

    private ArrayList<Audio> songs;
    private LayoutInflater songInf;
    private Context mContext;

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
        final RelativeLayout songLay = (RelativeLayout) songInf.inflate
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

        Button editMeta = (Button)songLay.findViewById(R.id.editMeta);
        Button addToPlaylist = (Button)songLay.findViewById(R.id.addPlaylist);
        editMeta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mContext instanceof MainActivity){
                    ((MainActivity)mContext).goToMetadataEdit(songLay);
                }
            }
        });

        addToPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return songLay;
    }

    public SongAdapter(Context c, ArrayList<Audio> theSongs){
        songs=theSongs;
        songInf=LayoutInflater.from(c);
        this.mContext=c;
    }


}

