package com.example.minijobhunt.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.minijobhunt.R;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.utils.UtilsFunctions;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONObject;

public class ProposalDetailsBottomSheet extends BottomSheetDialogFragment {

    private JSONObject proposal;
    private String rating = "0.0";

    public static ProposalDetailsBottomSheet newInstance(String proposalJson, String rating) {
        ProposalDetailsBottomSheet fragment = new ProposalDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString("proposal_data", proposalJson);
        args.putString("rating", rating);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_proposal_details_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView imgProfile = view.findViewById(R.id.imgProfile);
        TextView txtName = view.findViewById(R.id.txtName);
        TextView txtRating = view.findViewById(R.id.txtRating);
        TextView txtStatus = view.findViewById(R.id.txtStatus);
        TextView txtTime = view.findViewById(R.id.txtTime);
        TextView txtDate = view.findViewById(R.id.txtDate);
        TextView txtDescription = view.findViewById(R.id.txtDescription);
        View layoutAchievement = view.findViewById(R.id.layoutAchievement);
        TextView txtAchievement = view.findViewById(R.id.txtAchievement);
        Button btnClose = view.findViewById(R.id.btnClose);

        if (getArguments() != null) {
            try {
                proposal = new JSONObject(getArguments().getString("proposal_data"));
                rating = getArguments().getString("rating", "0.0");
                
                JSONObject user = proposal.getJSONObject("user");
                txtName.setText(user.optString("name", "Freelancer"));
                txtRating.setText(rating);
                
                String status = proposal.optString("status", "PENDING").toUpperCase();
                txtStatus.setText(status);
                
                txtTime.setText(proposal.optString("takes_time", "N/A"));
                
                String date = proposal.optString("created_at", "");
                txtDate.setText(UtilsFunctions.getTimeAgo(date));
                
                txtDescription.setText(proposal.optString("description", "No description."));
                
                String achievement = proposal.optString("achievement", "");
                if (!achievement.isEmpty() && !achievement.equals("null")) {
                    layoutAchievement.setVisibility(View.VISIBLE);
                    txtAchievement.setOnClickListener(v -> {
                        String url = Constants.URL + "storage/" + achievement;
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    });
                }

                String photo = user.optString("profile_photo", "");
                if (!photo.isEmpty()) {
                    String imageUrl = Constants.BASE_URL.replace("/api/", "") + "/storage/" + photo;
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_profile).into(imgProfile);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        btnClose.setOnClickListener(v -> dismiss());
    }
}
