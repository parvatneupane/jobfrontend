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

import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.ContractController;
import com.example.minijobhunt.controller.SubmissionController;
import com.example.minijobhunt.databinding.FragmentClientCompletedJobsBinding;
import com.example.minijobhunt.databinding.ItemClientJobBinding;
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

public class ClientCompletedJobsFragment extends Fragment {

    private FragmentClientCompletedJobsBinding binding;
    private ContractController contractController;
    private SubmissionController submissionController;
    private List<JSONObject> contractList = new ArrayList<>();
    private Map<Integer, List<JSONObject>> submissionCache = new HashMap<>();
    private CompletedJobAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClientCompletedJobsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contractController = new ContractController();
        submissionController = new SubmissionController();
        setupRecyclerView();
        loadCompletedContracts();
    }

    private void setupRecyclerView() {
        adapter = new CompletedJobAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    private void loadCompletedContracts() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int currentUserId = pref.getInt("userid", 0);

        submissionController.getSubmissions(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        submissionCache.clear();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject sub = data.getJSONObject(i);
                            int contractId = sub.optInt("contract_id");
                            if (!submissionCache.containsKey(contractId)) {
                                submissionCache.put(contractId, new ArrayList<>());
                            }
                            submissionCache.get(contractId).add(sub);
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
                fetchContracts(token, currentUserId);
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { fetchContracts(token, currentUserId); }
        });
    }

    private void fetchContracts(String token, int currentUserId) {
        contractController.getContracts(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        contractList.clear();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject contract = data.getJSONObject(i);
                            if (contract.optInt("client_id") == currentUserId && 
                                contract.optString("status").equalsIgnoreCase("completed")) {
                                contractList.add(contract);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (contractList.isEmpty()) {
                            binding.txtEmpty.setVisibility(View.VISIBLE);
                            binding.recyclerView.setVisibility(View.GONE);
                        } else {
                            binding.txtEmpty.setVisibility(View.GONE);
                            binding.recyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class CompletedJobAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CompletedJobAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemClientJobBinding itemBinding = ItemClientJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject contract = contractList.get(position);
            try {
                JSONObject job = contract.getJSONObject("task");
                JSONObject freelancer = contract.optJSONObject("freelancer");

                holder.itemBinding.txtTitle.setText(job.getString("title"));
                holder.itemBinding.txtClientName.setText(freelancer != null ? freelancer.optString("name", "Freelancer") : "Freelancer");
                holder.itemBinding.txtCategory.setText(job.optJSONObject("category") != null ? job.getJSONObject("category").getString("name") : "General");
                holder.itemBinding.txtBudget.setText("Rs. " + job.getString("budget"));
                holder.itemBinding.txtDeadline.setText("Completed");
                holder.itemBinding.txtStatus.setText("COMPLETED");
                holder.itemBinding.txtStatus.setTextColor(requireContext().getColor(R.color.success));
                
                holder.itemBinding.layoutClientActions.setVisibility(View.GONE);
                holder.itemBinding.layoutCompletion.setVisibility(View.VISIBLE);
                holder.itemBinding.btnMarkCompleted.setVisibility(View.GONE);

                int contractId = contract.optInt("id");
                List<JSONObject> submissions = submissionCache.get(contractId);
                if (submissions != null && !submissions.isEmpty()) {
                    holder.itemBinding.btnViewWork.setVisibility(View.VISIBLE);
                    holder.itemBinding.btnViewWork.setText("View Final Work");
                    holder.itemBinding.btnViewWork.setOnClickListener(v -> showViewSubmissionsDialog(submissions));
                } else {
                    holder.itemBinding.btnViewWork.setVisibility(View.GONE);
                }

                holder.itemView.setOnClickListener(v -> {
                    TaskDetailsBottomSheet sheet = TaskDetailsBottomSheet.newInstance(job.toString());
                    sheet.show(getChildFragmentManager(), "TaskDetails");
                });

            } catch (Exception e) { e.printStackTrace(); }
        }
        @Override
        public int getItemCount() { return contractList.size(); }
        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemClientJobBinding itemBinding;
            ViewHolder(ItemClientJobBinding itemBinding) { super(itemBinding.getRoot()); this.itemBinding = itemBinding; }
        }
    }

    private void showViewSubmissionsDialog(List<JSONObject> submissions) {
        androidx.recyclerview.widget.RecyclerView rv = new androidx.recyclerview.widget.RecyclerView(requireContext());
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setAdapter(new androidx.recyclerview.widget.RecyclerView.Adapter<SubViewHolder>() {
            @NonNull
            @Override
            public SubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_submission_manage, parent, false);
                return new SubViewHolder(view);
            }
            @Override
            public void onBindViewHolder(@NonNull SubViewHolder holder, int position) {
                JSONObject sub = submissions.get(position);
                String attachment = sub.optString("attachment", "");
                holder.txtFileName.setText("Work File: " + attachment);
                holder.btnView.setOnClickListener(v -> {
                    String url = Constants.URL + "storage/" + attachment;
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                });
                holder.btnDelete.setVisibility(View.GONE);
            }
            @Override
            public int getItemCount() { return submissions.size(); }
        });
        new android.app.AlertDialog.Builder(requireContext()).setTitle("Completed Work Files").setView(rv).setPositiveButton("Close", null).show();
    }
    class SubViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        android.widget.TextView txtFileName;
        android.widget.ImageButton btnView, btnDelete;
        SubViewHolder(View itemView) {
            super(itemView);
            txtFileName = itemView.findViewById(R.id.txtFileName);
            btnView = itemView.findViewById(R.id.btnView);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
