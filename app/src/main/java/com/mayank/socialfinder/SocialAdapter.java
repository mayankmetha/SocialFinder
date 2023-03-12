package com.mayank.socialfinder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class SocialAdapter extends ArrayAdapter<SocialModel> {

    final ArrayList<SocialModel> socialModels;
    final Context context;

    public SocialAdapter(ArrayList<SocialModel> socialModels, Context context) {
        super(context, -1, socialModels);
        this.socialModels = socialModels;
        this.context = context;
    }

    @Override
    public int getCount() {
        return socialModels.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @NonNull
    @Override
    public View getView(int pos, View convertedView, @NonNull ViewGroup parent) {
        SocialModel socialViewModel = socialModels.get(pos);
        if(convertedView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);
            convertedView = layoutInflater.inflate(R.layout.account_item, parent, false);
        }
        TextView name = convertedView.findViewById(R.id.social_name);
        name.setText(socialViewModel.getPlatform());
        TextView details = convertedView.findViewById(R.id.social_details);
        details.setText(socialViewModel.getDetails().isEmpty()?R.string.no_metadata:R.string.metadata_present);
        TextView icon = convertedView.findViewById(R.id.social_icon);
        icon.setText(String.valueOf(socialViewModel.getStatus()));

        return convertedView;
    }

}

