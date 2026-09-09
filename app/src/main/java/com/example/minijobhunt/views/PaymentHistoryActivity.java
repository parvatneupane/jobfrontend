package com.example.minijobhunt.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.minijobhunt.R;
import com.example.minijobhunt.utils.App;
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

public class PaymentHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private SwipeRefreshLayout swipeRefresh;
    private TextView txtEmpty;
    private List<JSONObject> historyList = new ArrayList<>();
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        rvHistory = findViewById(R.id.rvHistory);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        txtEmpty = findViewById(R.id.txtEmpty);

        adapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadHistory);
        loadHistory();
    }

    private void loadHistory() {
        swipeRefresh.setRefreshing(true);
        SharedPreferences pref = getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        App.api.getPaymentHistory(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        historyList.clear();
                        for (int i = 0; i < data.length(); i++) {
                            historyList.add(data.getJSONObject(i));
                        }
                        adapter.notifyDataSetChanged();

                        if (historyList.isEmpty()) {
                            txtEmpty.setVisibility(View.VISIBLE);
                            rvHistory.setVisibility(View.GONE);
                        } else {
                            txtEmpty.setVisibility(View.GONE);
                            rvHistory.setVisibility(View.VISIBLE);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PaymentHistoryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payment_history, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject item = historyList.get(position);
            try {
                String type = item.getString("type");
                String title = item.getString("title");
                String description = item.getString("description");
                String date = item.getString("date");
                double amount = item.getDouble("amount");
                String status = item.getString("status");

                holder.txtTitle.setText(title);
                holder.txtDescription.setText(description);
                holder.txtDate.setText(UtilsFunctions.getTimeAgo(date));

                if ("earning".equals(type)) {
                    holder.txtIcon.setText("E");
                    holder.txtIcon.setBackgroundResource(R.drawable.bg_circle_light_purple);
                    holder.txtIcon.setTextColor(ContextCompat.getColor(PaymentHistoryActivity.this, R.color.primary));
                    holder.txtAmount.setText("+ Rs. " + amount);
                    holder.txtAmount.setTextColor(ContextCompat.getColor(PaymentHistoryActivity.this, R.color.success));
                } else {
                    holder.txtIcon.setText("W");
                    holder.txtIcon.setBackgroundResource(R.drawable.bg_circle_light_blue);
                    holder.txtIcon.setTextColor(ContextCompat.getColor(PaymentHistoryActivity.this, R.color.info));
                    holder.txtAmount.setText("- Rs. " + amount);
                    holder.txtAmount.setTextColor(ContextCompat.getColor(PaymentHistoryActivity.this, R.color.accent));
                }

                holder.txtStatus.setText(status.toUpperCase());
                if ("completed".equalsIgnoreCase(status) || "released".equalsIgnoreCase(status)) {
                    holder.txtStatus.setTextColor(Color.GREEN);
                } else if ("pending".equalsIgnoreCase(status)) {
                    holder.txtStatus.setTextColor(Color.parseColor("#FFA500"));
                } else {
                    holder.txtStatus.setTextColor(Color.RED);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtIcon, txtTitle, txtDescription, txtDate, txtAmount, txtStatus;
            ViewHolder(View itemView) {
                super(itemView);
                txtIcon = itemView.findViewById(R.id.txtIcon);
                txtTitle = itemView.findViewById(R.id.txtTitle);
                txtDescription = itemView.findViewById(R.id.txtDescription);
                txtDate = itemView.findViewById(R.id.txtDate);
                txtAmount = itemView.findViewById(R.id.txtAmount);
                txtStatus = itemView.findViewById(R.id.txtStatus);
            }
        }
    }
}
