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
import com.example.minijobhunt.databinding.FragmentClientChatBinding;
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

public class ClientChatFragment extends Fragment {

    private FragmentClientChatBinding binding;
    private ChatController chatController;
    private List<JSONObject> chatList = new ArrayList<>();
    private List<JSONObject> filteredList = new ArrayList<>();
    private ChatAdapter adapter;
    private String currentTab = "all";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClientChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        chatController = new ChatController();

        setupRecyclerView();
        setupTabs();
    }

    private void setupTabs() {
        binding.tabAll.setOnClickListener(v -> {
            currentTab = "all";
            updateTabUI();
            filterChats();
        });

        binding.tabUnread.setOnClickListener(v -> {
            currentTab = "unread";
            updateTabUI();
            filterChats();
        });
    }

    private void updateTabUI() {
        if (currentTab.equals("all")) {
            binding.txtTabAll.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
            binding.indicatorAll.setVisibility(View.VISIBLE);
            binding.txtTabUnread.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
            binding.indicatorUnread.setVisibility(View.INVISIBLE);
        } else {
            binding.txtTabUnread.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
            binding.indicatorUnread.setVisibility(View.VISIBLE);
            binding.txtTabAll.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
            binding.indicatorAll.setVisibility(View.INVISIBLE);
        }
    }

    private void filterChats() {
        filteredList.clear();
        if (currentTab.equals("all")) {
            filteredList.addAll(chatList);
        } else {
            for (JSONObject chat : chatList) {
                if (chat.optInt("calculated_unread_count", 0) > 0) {
                    filteredList.add(chat);
                }
            }
        }
        adapter.notifyDataSetChanged();
        
        if (filteredList.isEmpty()) {
            binding.txtEmpty.setVisibility(View.VISIBLE);
            binding.rvChats.setVisibility(View.GONE);
        } else {
            binding.txtEmpty.setVisibility(View.GONE);
            binding.rvChats.setVisibility(View.VISIBLE);
        }
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
                            
                            int clientId = -1;
                            if (chat.has("contract") && !chat.isNull("contract")) {
                                clientId = chat.getJSONObject("contract").optInt("client_id", -1);
                            } else {
                                clientId = chat.optInt("client_id", -1);
                            }

                            if (clientId == currentUserId) {
                                // Calculate unread count locally from messages array
                                int unreadCount = 0;
                                if (chat.has("messages")) {
                                    JSONArray messages = chat.getJSONArray("messages");
                                    for (int j = 0; j < messages.length(); j++) {
                                        JSONObject msg = messages.getJSONObject(j);
                                        if (!msg.optBoolean("is_seen", true) && 
                                            msg.optInt("sender_id") != currentUserId) {
                                            unreadCount++;
                                        }
                                    }
                                }
                                chat.put("calculated_unread_count", unreadCount);
                                chatList.add(chat);
                            }
                        }
                        filterChats();
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
            JSONObject chat = filteredList.get(position);
            try {
                String name = "Freelancer";
                if (chat.has("contract") && !chat.isNull("contract")) {
                    JSONObject contract = chat.getJSONObject("contract");
                    if (contract.has("freelancer") && !contract.isNull("freelancer")) {
                        name = contract.getJSONObject("freelancer").optString("name", "Freelancer");
                    }
                }
                
                holder.itemBinding.txtChatTitle.setText(name);
                holder.itemBinding.txtChatAvatar.setText(String.valueOf(name.charAt(0)));
                holder.itemBinding.txtLastMessage.setText(chat.optString("last_message", "No messages yet"));
                
                String date = chat.optString("last_message_time", chat.optString("updated_at", ""));
                holder.itemBinding.txtChatTime.setText(UtilsFunctions.getTimeAgo(date));

                // Unread Feature
                int unreadCount = chat.optInt("calculated_unread_count", 0);
                if (unreadCount > 0) {
                    holder.itemBinding.txtUnreadCount.setVisibility(View.VISIBLE);
                    holder.itemBinding.txtUnreadCount.setText(String.valueOf(unreadCount));
                    holder.itemBinding.txtChatTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                    holder.itemBinding.txtLastMessage.setTypeface(null, android.graphics.Typeface.BOLD);
                    holder.itemBinding.txtLastMessage.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
                } else {
                    holder.itemBinding.txtUnreadCount.setVisibility(View.GONE);
                    holder.itemBinding.txtChatTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
                    holder.itemBinding.txtLastMessage.setTypeface(null, android.graphics.Typeface.NORMAL);
                    holder.itemBinding.txtLastMessage.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_secondary));
                }

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
            return filteredList.size();
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
