package com.example.minijobhunt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minijobhunt.adapter.NotificationAdapter;
import com.example.minijobhunt.utils.App;
import com.example.minijobhunt.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationFragment extends Fragment {

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private TextView txtEmpty;
    private TextView txtClearAll;
    private NotificationAdapter adapter;
    private List<JSONObject> notificationList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        progressBar = view.findViewById(R.id.progressBar);
        txtEmpty = view.findViewById(R.id.txtEmpty);
        txtClearAll = view.findViewById(R.id.txtClearAll);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        txtClearAll.setOnClickListener(v -> clearAllNotifications());

        setupSwipeToDismiss();
        fetchNotifications();
    }

    private void setupSwipeToDismiss() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                JSONObject notification = adapter.getItem(position);
                int id = notification.optInt("id");
                
                deleteNotificationFromServer(id, position);
            }
        }).attachToRecyclerView(rvNotifications);
    }

    private void deleteNotificationFromServer(int id, int position) {
        SharedPreferences sp = getActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = sp.getString("token", "");
        
        App.api.deleteNotification("Bearer " + token, id).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    adapter.removeItem(position);
                    if (notificationList.isEmpty()) {
                        txtEmpty.setVisibility(View.VISIBLE);
                        txtClearAll.setVisibility(View.GONE);
                    }
                } else {
                    adapter.notifyItemChanged(position);
                    Toast.makeText(getContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                adapter.notifyItemChanged(position);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearAllNotifications() {
        SharedPreferences sp = getActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = sp.getString("token", "");
        int userId = sp.getInt("userid", 0);

        progressBar.setVisibility(View.VISIBLE);
        App.api.clearNotifications("Bearer " + token, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    notificationList.clear();
                    adapter.notifyDataSetChanged();
                    txtEmpty.setVisibility(View.VISIBLE);
                    txtClearAll.setVisibility(View.GONE);
                } else {
                    Toast.makeText(getContext(), "Failed to clear", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchNotifications() {
        SharedPreferences sp = getActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = sp.getString("token", "");
        int userId = sp.getInt("userid", 0);

        if (token.isEmpty() || userId == 0) {
            txtEmpty.setVisibility(View.VISIBLE);
            txtEmpty.setText("Please login to see notifications");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);

        App.api.getNotifications("Bearer " + token, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                progressBar.setVisibility(View.GONE);
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String raw = response.body().string();
                        JSONObject json = new JSONObject(raw);
                        JSONArray data = json.getJSONArray("data");

                        notificationList.clear();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject obj = data.getJSONObject(i);
                            // Filter out chat notifications
                            if (!"New Message".equalsIgnoreCase(obj.optString("title"))) {
                                notificationList.add(obj);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (notificationList.isEmpty()) {
                            txtEmpty.setVisibility(View.VISIBLE);
                            txtClearAll.setVisibility(View.GONE);
                        } else {
                            txtClearAll.setVisibility(View.VISIBLE);
                        }
                    } else {
                        txtEmpty.setVisibility(View.VISIBLE);
                        txtEmpty.setText("No notifications found");
                    }
                } catch (Exception e) {
                    Log.e("NOTIF", "Error", e);
                    txtEmpty.setVisibility(View.VISIBLE);
                    txtEmpty.setText("Failed to load notifications");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                txtEmpty.setVisibility(View.VISIBLE);
                txtEmpty.setText("Connection error");
            }
        });
    }
}