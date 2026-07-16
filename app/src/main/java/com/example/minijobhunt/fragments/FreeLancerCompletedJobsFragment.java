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
import com.example.minijobhunt.databinding.FragmentFreeLancerCompletedJobsBinding;
import com.example.minijobhunt.databinding.ItemFreelancerJobBinding;
import com.example.minijobhunt.utils.Constants;

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

public class FreeLancerCompletedJobsFragment extends Fragment {

    private FragmentFreeLancerCompletedJobsBinding binding;
    private ContractController contractController;
    private SubmissionController submissionController;
    private List<JSONObject> contractList = new ArrayList<>();
    private Map<Integer, List<JSONObject>> submissionCache = new HashMap<>();
    private CompletedWorkAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFreeLancerCompletedJobsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contractController = new ContractController();
        submissionController = new SubmissionController();
        setupRecyclerView();
        loadCompletedWork();
    }

    private void setupRecyclerView() {
        adapter = new CompletedWorkAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    private void loadCompletedWork() {
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
                            if (sub.optInt("freelancer_id") == currentUserId) {
                                int contractId = sub.optInt("contract_id");
                                if (!submissionCache.containsKey(contractId)) {
                                    submissionCache.put(contractId, new ArrayList<>());
                                }
                                submissionCache.get(contractId).add(sub);
                            }
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
                            if (contract.optInt("freelancer_id") == currentUserId && 
                                contract.optString("status").equalsIgnoreCase("completed")) {
                                contractList.add(contract);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class CompletedWorkAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<CompletedWorkAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemFreelancerJobBinding itemBinding = ItemFreelancerJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JSONObject contract = contractList.get(position);
            try {
                JSONObject job = contract.getJSONObject("task");
                JSONObject client = contract.optJSONObject("client");

                holder.itemBinding.txtJobTitle.setText(job.getString("title"));
                holder.itemBinding.txtClientName.setText(client != null ? client.optString("name", "Client") : "Client");
                holder.itemBinding.txtJobBudget.setText("Earned: Rs. " + job.getString("budget"));
                holder.itemBinding.txtJobDetails.setText("Completed Project");
                
                holder.itemBinding.btnApply.setVisibility(View.GONE);
                holder.itemBinding.layoutWork.setVisibility(View.VISIBLE);
                holder.itemBinding.btnSubmitWork.setVisibility(View.GONE);
                holder.itemBinding.btnManageWork.setText("View My Work");

                int contractId = contract.optInt("id");
                List<JSONObject> submissions = submissionCache.get(contractId);
                
                holder.itemBinding.btnManageWork.setOnClickListener(v -> showViewWorkDialog(submissions));
                
                if (submissions != null && !submissions.isEmpty()) {
                    holder.itemBinding.txtWorkFile.setVisibility(View.VISIBLE);
                    holder.itemBinding.txtWorkFile.setText("Final File: " + submissions.get(submissions.size()-1).optString("attachment"));
                    holder.itemBinding.txtWorkFile.setOnClickListener(v -> openAttachment(submissions.get(submissions.size()-1).optString("attachment")));
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
            ItemFreelancerJobBinding itemBinding;
            ViewHolder(ItemFreelancerJobBinding itemBinding) { super(itemBinding.getRoot()); this.itemBinding = itemBinding; }
        }
    }

    private void openAttachment(String attachment) {
        String url = Constants.URL + "storage/" + attachment;
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void showViewWorkDialog(List<JSONObject> submissions) {
        if (submissions == null || submissions.isEmpty()) return;

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
                holder.txtFileName.setText("Work: " + attachment);
                holder.btnView.setOnClickListener(v -> openAttachment(attachment));
                holder.btnDelete.setVisibility(View.GONE); // No deletion after completion
            }
            @Override
            public int getItemCount() { return submissions.size(); }
        });
        new android.app.AlertDialog.Builder(requireContext()).setTitle("Your Completed Work").setView(rv).setPositiveButton("Close", null).show();
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
