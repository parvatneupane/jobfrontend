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
import com.example.minijobhunt.databinding.FragmentChatMessageBinding;
import com.example.minijobhunt.databinding.ItemMessageReceivedBinding;
import com.example.minijobhunt.databinding.ItemMessageSentBinding;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.utils.UtilsFunctions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatMessageFragment extends Fragment {

    private FragmentChatMessageBinding binding;
    private ChatController chatController;
    private List<JSONObject> messageList = new ArrayList<>();
    private MessageAdapter adapter;
    private int chatId = -1;
    private String chatTitle = "";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatMessageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chatController = new ChatController();

        if (getArguments() != null) {
            chatId = getArguments().getInt("chat_id", -1);
            chatTitle = getArguments().getString("chat_title", "Chat");
            binding.txtChatTitle.setText(chatTitle);
        }

        binding.btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        binding.btnSend.setOnClickListener(v -> sendMessage());

        setupRecyclerView();
        loadMessages();
        markAsRead();
    }

    private void markAsRead() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        
        chatController.markChatAsRead(token, chatId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // Silently handle
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { }
        });
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);
    }

    private void loadMessages() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        chatController.getChatMessages(token, chatId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        messageList.clear();
                        for (int i = 0; i < data.length(); i++) {
                            messageList.add(data.getJSONObject(i));
                        }
                        adapter.notifyDataSetChanged();
                        if (messageList.size() > 0) {
                            binding.rvMessages.smoothScrollToPosition(messageList.size() - 1);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage() {
        String msg = binding.etMessage.getText().toString().trim();
        if (msg.isEmpty()) return;

        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int userId = pref.getInt("userid", 0);

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("sender_id", userId);
        body.put("message", msg);

        binding.etMessage.setText("");

        chatController.sendMessage(token, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    loadMessages();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Failed to send", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TYPE_SENT = 1;
        private static final int TYPE_RECEIVED = 2;

        @Override
        public int getItemViewType(int position) {
            SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
            int currentUserId = pref.getInt("userid", 0);
            
            JSONObject msg = messageList.get(position);
            return msg.optInt("sender_id") == currentUserId ? TYPE_SENT : TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_SENT) {
                ItemMessageSentBinding itemBinding = ItemMessageSentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new SentViewHolder(itemBinding);
            } else {
                ItemMessageReceivedBinding itemBinding = ItemMessageReceivedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                return new ReceivedViewHolder(itemBinding);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            JSONObject msg = messageList.get(position);
            try {
                String text = msg.getString("message");
                String time = UtilsFunctions.getTimeAgo(msg.optString("created_at", ""));

                if (holder instanceof SentViewHolder) {
                    SentViewHolder sentHolder = (SentViewHolder) holder;
                    sentHolder.itemBinding.txtMessage.setText(text);
                    sentHolder.itemBinding.txtTime.setText(time);
                    
                    // Read Receipt Feature
                    boolean isSeen = msg.optBoolean("is_seen", false);
                    sentHolder.itemBinding.txtStatus.setVisibility(View.VISIBLE);
                    if (isSeen) {
                        sentHolder.itemBinding.txtStatus.setText("Seen");
                        sentHolder.itemBinding.txtStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success));
                    } else {
                        sentHolder.itemBinding.txtStatus.setText("Sent");
                        sentHolder.itemBinding.txtStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.orange_primary));
                    }
                } else {
                    ((ReceivedViewHolder) holder).itemBinding.txtMessage.setText(text);
                    ((ReceivedViewHolder) holder).itemBinding.txtTime.setText(time);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return messageList.size();
        }

        class SentViewHolder extends RecyclerView.ViewHolder {
            ItemMessageSentBinding itemBinding;
            SentViewHolder(ItemMessageSentBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }

        class ReceivedViewHolder extends RecyclerView.ViewHolder {
            ItemMessageReceivedBinding itemBinding;
            ReceivedViewHolder(ItemMessageReceivedBinding itemBinding) {
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
