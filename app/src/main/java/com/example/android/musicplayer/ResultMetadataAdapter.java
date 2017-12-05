package com.example.android.musicplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.umass.lastfm.Track;

/**
 * Created by user on 05-Dec-17.
 */

public class ResultMetadataAdapter extends ArrayAdapter<Track> {
    public ResultMetadataAdapter(Context context, ArrayList<Track> resultData){
        super(context,R.layout.resultmetadata, resultData);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Track metadata= getItem(position);
        if(convertView==null){
            convertView= LayoutInflater.from(getContext()).inflate(R.layout.resultmetadata,parent,false);
        }

        TextView textTitle= convertView.findViewById(R.id.textTitle);
        TextView textArtist= convertView.findViewById(R.id.textArtist);
        TextView textAlbum= convertView.findViewById(R.id.textAlbum);


        //this supposed to get data from log, but i dont know how to make it happen, also i'm blind :( alv
        textTitle.setText("Title");
        textArtist.setText("- "+"ArtistName");
        textAlbum.setText("- "+"Album");

        return convertView;
    }
}
