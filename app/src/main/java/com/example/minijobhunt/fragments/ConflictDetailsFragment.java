package com.example.minijobhunt.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minijobhunt.controller.ConflictController;
import com.example.minijobhunt.databinding.FragmentConflictDetailsBinding;
import com.example.minijobhunt.databinding.ItemSubmissionManageBinding; // Reusing for now or create new
import com.example.minijobhunt.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConflictDetailsFragment extends Fragment {

    private FragmentConflictDetailsBinding binding;
    private ConflictController controller;
    private int contractId;
    private int conflictId = -1;
    private List<JSONObject> replyList = new ArrayList<>();
    private ReplyAdapter adapter;

    public static ConflictDetailsFragment newInstance(int contractId) {
        ConflictDetailsFragment fragment = new ConflictDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("contract_id", contractId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            contractId = getArguments().getInt("contract_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentConflictDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        controller = new ConflictController();

        setupRecyclerView();
        findConflict();

        binding.btnSendReply.setOnClickListener(v -> submitReply());
    }

    private void setupRecyclerView() {
        adapter = new ReplyAdapter();
        binding.recyclerViewReplies.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewReplies.setAdapter(adapter);
    }

    private void findConflict() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        controller.getConflicts(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject conflict = data.getJSONObject(i);
                            if (conflict.optInt("contract_id") == contractId) {
                                displayConflict(conflict);
                                break;
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void displayConflict(JSONObject conflict) {
        try {
            conflictId = conflict.getInt("id");
            binding.txtConflictTitle.setText(conflict.getString("title"));
            binding.txtConflictReason.setText(conflict.getString("reason"));
            
            String status = conflict.optString("status", "open");
            binding.txtConflictStatus.setText(status.toUpperCase());

            JSONObject raisedBy = conflict.optJSONObject("raised_by_user");
            JSONObject against = conflict.optJSONObject("against_user");
            String raisedByName = raisedBy != null ? raisedBy.optString("name", "User") : "User";
            String againstName = against != null ? against.optString("name", "User") : "User";
            
            binding.txtRaisedBy.setText("Raised By: " + raisedByName);
            binding.txtAgainst.setText("Against: " + againstName);

            String attachment = conflict.optString("attachment", "");
            if (!attachment.isEmpty() && !attachment.equals("null")) {
                binding.btnViewAttachment.setVisibility(View.VISIBLE);
                binding.btnViewAttachment.setOnClickListener(v -> openAttachment(attachment));
            }

            loadReplies();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void loadReplies() {
        if (conflictId == -1) return;

        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        controller.getConflictReplies(token, conflictId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        replyList.clear();
                        for (int i = 0; i < data.length(); i++) {
                            replyList.add(data.getJSONObject(i));
                        }
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private void submitReply() {
        String msg = binding.etReplyMessage.getText().toString().trim();
        
        if (msg.isEmpty()) {
            Toast.makeText(requireContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        if (conflictId == -1) {
            Toast.makeText(requireContext(), "Conflict data still loading...", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", 0);

        binding.btnSendReply.setEnabled(false);

        controller.submitReply(token, conflictId, userId, msg, null).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                binding.btnSendReply.setEnabled(true);
                if (response.isSuccessful()) {
                    binding.etReplyMessage.setText("");
                    loadReplies();
                } else {
                    Toast.makeText(requireContext(), "Failed to send: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                binding.btnSendReply.setEnabled(true);
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openAttachment(String attachment) {
        String url = Constants.URL + "storage/" + attachment;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {
        @NonNull
        @Override
        public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemSubmissionManageBinding itemBinding = ItemSubmissionManageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ReplyViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
            JSONObject reply = replyList.get(position);
            try {
                JSONObject user = reply.getJSONObject("user");
                holder.itemBinding.txtFileName.setText(user.optString("name") + ": " + reply.getString("message"));
                holder.itemBinding.btnDelete.setVisibility(View.GONE);
                
                String attachment = reply.optString("attachment", "");
                if (!attachment.isEmpty() && !attachment.equals("null")) {
                    holder.itemBinding.btnView.setVisibility(View.VISIBLE);
                    holder.itemBinding.btnView.setOnClickListener(v -> openAttachment(attachment));
                } else {
                    holder.itemBinding.btnView.setVisibility(View.GONE);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }

        @Override
        public int getItemCount() { return replyList.size(); }

        class ReplyViewHolder extends RecyclerView.ViewHolder {
            ItemSubmissionManageBinding itemBinding;
            ReplyViewHolder(ItemSubmissionManageBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }
    }
}
