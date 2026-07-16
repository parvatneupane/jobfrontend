package com.example.minijobhunt.fragments;

import android.content.Context;
import android.content.SharedPreferences;
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

import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.ChatController;
import com.example.minijobhunt.databinding.FragmentFreelancerChatBinding;
import com.example.minijobhunt.databinding.ItemChatBinding;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.utils.UtilsFunctions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FreelancerChatFragment extends Fragment {

    private FragmentFreelancerChatBinding binding;
    private ChatController chatController;
    private List<JSONObject> chatList = new ArrayList<>();
    private ChatAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFreelancerChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chatController = new ChatController();

        setupRecyclerView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadChats();
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter();
        binding.rvChats.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChats.setAdapter(adapter);
    }

    private void loadChats() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int currentUserId = pref.getInt("userid", 0);

        chatController.getChats(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String jsonResponse = response.body().string();
                        JSONObject root = new JSONObject(jsonResponse);
                        JSONArray data = root.getJSONArray("data");

                        chatList.clear();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject chat = data.getJSONObject(i);
                            
                            int freelancerId = -1;
                            if (chat.has("contract") && !chat.isNull("contract")) {
                                freelancerId = chat.getJSONObject("contract").optInt("freelancer_id", -1);
                            } else {
                                // Direct freelancer_id in chat object fallback
                                freelancerId = chat.optInt("freelancer_id", -1);
                            }

                            if (freelancerId == currentUserId) {
                                chatList.add(chat);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (chatList.isEmpty()) {
                            Toast.makeText(requireContext(), "No active chats found for your account", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to fetch chats: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (getContext() != null) {
                    Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemChatBinding itemBinding = ItemChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ChatViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            JSONObject chat = chatList.get(position);
            try {
                String name = "Client";
                if (chat.has("contract") && !chat.isNull("contract")) {
                    JSONObject contract = chat.getJSONObject("contract");
                    if (contract.has("client") && !contract.isNull("client")) {
                        name = contract.getJSONObject("client").optString("name", "Client");
                    }
                }
                
                holder.itemBinding.txtChatTitle.setText(name);
                holder.itemBinding.txtChatAvatar.setText(String.valueOf(name.charAt(0)));
                
                holder.itemBinding.txtLastMessage.setText(chat.optString("last_message", "No messages yet"));
                
                String date = chat.optString("last_message_time", chat.optString("updated_at", ""));
                holder.itemBinding.txtChatTime.setText(UtilsFunctions.getTimeAgo(date));

                final String finalName = name;
                holder.itemView.setOnClickListener(v -> {
                    ChatMessageFragment fragment = new ChatMessageFragment();
                    Bundle args = new Bundle();
                    args.putInt("chat_id", chat.optInt("id"));
                    args.putString("chat_title", finalName);
                    fragment.setArguments(args);

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return chatList.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            ItemChatBinding itemBinding;
            ChatViewHolder(ItemChatBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
