package com.example.android.musicplayer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import de.umass.lastfm.Track;

/**
 * Created by user on 05-Dec-17.
 */



public class ResultMetadataAdapter extends BaseAdapter{

    private Context c;
    private ArrayList<Track> tList;
    private LayoutInflater tInf;

    public ResultMetadataAdapter(Context context, ArrayList<Track> resultData){
        c = context;
        tList = resultData;
    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final RelativeLayout songLay = (RelativeLayout) tInf.inflate
                (R.layout.resultmetadata, parent, false);
        Track metadata= tList.get(position);

        TextView textTitle= convertView.findViewById(R.id.textTitle);
        TextView textArtist= convertView.findViewById(R.id.textArtist);
        TextView textAlbum= convertView.findViewById(R.id.textAlbum);

        //this supposed to get data from log, but i dont know how to make it happen, also i'm blind :( alv
        textTitle.setText(metadata.getName());
        textArtist.setText("- "+ metadata.getArtist());
        textAlbum.setText("- "+ metadata.getAlbum());

        return convertView;
    }
}
