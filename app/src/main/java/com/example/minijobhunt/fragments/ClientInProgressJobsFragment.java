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

import com.bumptech.glide.Glide;
import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.ContractController;
import com.example.minijobhunt.controller.PaymentController;
import com.example.minijobhunt.controller.SubmissionController;
import com.example.minijobhunt.controller.TaskController;
import com.example.minijobhunt.databinding.FragmentClientInProgressJobsBinding;
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

public class ClientInProgressJobsFragment extends Fragment {

    private FragmentClientInProgressJobsBinding binding;
    private TaskController taskController;
    private ContractController contractController;
    private SubmissionController submissionController;
    private PaymentController paymentController;
    private List<JSONObject> contractList = new ArrayList<>();
    private Map<Integer, List<JSONObject>> submissionCache = new HashMap<>(); // contract_id -> list of submissions
    private JobAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClientInProgressJobsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        taskController = new TaskController();
        contractController = new ContractController();
        submissionController = new SubmissionController();
        paymentController = new PaymentController();
        setupRecyclerView();
        loadActiveContracts();
    }

    private void setupRecyclerView() {
        adapter = new JobAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    private void loadActiveContracts() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int currentUserId = pref.getInt("userid", 0);

        // Fetch submissions first to see what's uploaded
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // Then load contracts
                fetchContracts(token, currentUserId);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                fetchContracts(token, currentUserId);
            }
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
                            // Filter by client ID and "active" status
                            if (contract.optInt("client_id") == currentUserId && 
                                contract.optString("status").equalsIgnoreCase("active")) {
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

    private void completeJob(int contractId, int jobId, int freelancerId, int clientId, String freelancerName) {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        // Show a progress indicator or toast
        Toast.makeText(requireContext(), "Processing completion...", Toast.LENGTH_SHORT).show();

        // 1. Update Contract to completed
        Map<String, Object> contractBody = new HashMap<>();
        contractBody.put("status", "completed");

        contractController.updateContractStatus(token, contractId, contractBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // 2. Update Task to completed
                    Map<String, Object> taskBody = new HashMap<>();
                    taskBody.put("status", "completed");
                    taskController.updateTask(token, jobId, taskBody).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                // 3. Release Payment from Escrow
                                releaseEscrowPayment(token, contractId, jobId, freelancerId, clientId, freelancerName);
                            } else {
                                Toast.makeText(requireContext(), "Contract updated, but failed to update task status", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(requireContext(), "Task Update Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(requireContext(), "Failed to update contract status", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Contract Update Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void releaseEscrowPayment(String token, int contractId, int taskId, int freelancerId, int clientId, String freelancerName) {
        paymentController.getPayments(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");
                        
                        int paymentId = -1;
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject p = data.getJSONObject(i);
                            if (p.optInt("contract_id") == contractId && 
                                p.optString("status").equalsIgnoreCase("escrow")) {
                                paymentId = p.getInt("id");
                                break;
                            }
                        }

                        if (paymentId != -1) {
                            paymentController.releasePayment(token, paymentId).enqueue(new Callback<ResponseBody>() {
                                @Override
                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                    Toast.makeText(requireContext(), "Job completed and payment released!", Toast.LENGTH_SHORT).show();
                                    openReviewFragment(contractId, taskId, freelancerId, clientId, freelancerName);
                                }
                                @Override
                                public void onFailure(Call<ResponseBody> call, Throwable t) {
                                    Toast.makeText(requireContext(), "Job completed, but payment release failed", Toast.LENGTH_SHORT).show();
                                    openReviewFragment(contractId, taskId, freelancerId, clientId, freelancerName);
                                }
                            });
                        } else {
                            Toast.makeText(requireContext(), "Job completed! (No escrow payment found)", Toast.LENGTH_SHORT).show();
                            openReviewFragment(contractId, taskId, freelancerId, clientId, freelancerName);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    openReviewFragment(contractId, taskId, freelancerId, clientId, freelancerName);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                openReviewFragment(contractId, taskId, freelancerId, clientId, freelancerName);
            }
        });
    }

    private void openReviewFragment(int contractId, int taskId, int freelancerId, int clientId, String freelancerName) {
        ReviewAndRatingBoxFragment fragment = new ReviewAndRatingBoxFragment();
        Bundle args = new Bundle();
        args.putInt("contract_id", contractId);
        args.putInt("task_id", taskId);
        args.putInt("freelancer_id", freelancerId);
        args.putInt("client_id", clientId);
        args.putString("freelancer_name", freelancerName);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openConflictDetails(int contractId) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ConflictDetailsFragment.newInstance(contractId))
                .addToBackStack(null)
                .commit();
    }

    private class JobAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<JobAdapter.JobViewHolder> {
        @NonNull
        @Override
        public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemClientJobBinding itemBinding = ItemClientJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new JobViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
            JSONObject contract = contractList.get(position);
            try {
                JSONObject job = contract.getJSONObject("task");
                JSONObject freelancer = contract.optJSONObject("freelancer");

                holder.itemBinding.txtTitle.setText(job.getString("title"));
                
                String freelancerName = freelancer != null ? freelancer.optString("name", "Freelancer") : "Freelancer";
                holder.itemBinding.txtClientName.setText(freelancerName);
                
                holder.itemBinding.txtCategory.setText(job.optJSONObject("category") != null ? 
                        job.getJSONObject("category").getString("name") : "General");
                holder.itemBinding.txtBudget.setText("Rs. " + job.getString("budget"));
                holder.itemBinding.txtDeadline.setText("Due: " + UtilsFunctions.formatDate(contract.optString("deadline")));
                holder.itemBinding.txtStatus.setText("HIRED: " + freelancerName.toUpperCase());
                
                holder.itemBinding.layoutClientActions.setVisibility(View.GONE);
                holder.itemBinding.layoutCompletion.setVisibility(View.VISIBLE);

                int contractId = contract.optInt("id");
                List<JSONObject> submissions = submissionCache.get(contractId);

                if (submissions != null && !submissions.isEmpty()) {
                    holder.itemBinding.btnViewWork.setVisibility(View.VISIBLE);
                    holder.itemBinding.btnMarkCompleted.setEnabled(true);
                    holder.itemBinding.btnMarkCompleted.setAlpha(1.0f);
                    
                    holder.itemBinding.btnViewWork.setOnClickListener(v -> {
                        showViewSubmissionsDialog(submissions);
                    });
                } else {
                    holder.itemBinding.btnViewWork.setVisibility(View.GONE);
                    holder.itemBinding.btnMarkCompleted.setEnabled(false);
                    holder.itemBinding.btnMarkCompleted.setAlpha(0.5f);
                }

                String status = contract.optString("status");
                String taskStatus = job.optString("status");

                if (taskStatus.equalsIgnoreCase("under_review")) {
                    holder.itemBinding.txtStatus.setText("UNDER REVIEW");
                    holder.itemBinding.txtStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    
                    holder.itemBinding.btnMarkCompleted.setEnabled(false);
                    holder.itemBinding.btnMarkCompleted.setAlpha(0.5f);
                    holder.itemBinding.btnMarkCompleted.setText("Under Review");
                    
                    holder.itemBinding.btnRaiseConflict.setText("View Conflict Details");
                    holder.itemBinding.btnRaiseConflict.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
                } else {
                    holder.itemBinding.btnMarkCompleted.setText("Mark as Completed");
                    holder.itemBinding.btnRaiseConflict.setText("Problem? Raise a Conflict");
                    holder.itemBinding.btnRaiseConflict.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent));
                }

                holder.itemBinding.btnMarkCompleted.setOnClickListener(v -> {
                    if (taskStatus.equalsIgnoreCase("under_review")) {
                        Toast.makeText(requireContext(), "Action disabled while under review", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Complete Job")
                            .setMessage("Are you sure you want to mark this job as completed? This will release payment and close the project.")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                try {
                                    SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
                                    int clientId = pref.getInt("userid", 0);
                                    int freelancerId = freelancer != null ? freelancer.optInt("id", 0) : 0;
                                    completeJob(contractId, job.getInt("id"), freelancerId, clientId, freelancerName);
                                } catch (Exception e) { e.printStackTrace(); }
                            })
                            .setNegativeButton("No", null)
                            .show();
                });

                holder.itemBinding.btnRaiseConflict.setOnClickListener(v -> {
                    if (taskStatus.equalsIgnoreCase("under_review")) {
                        // Open Conflict Details
                        openConflictDetails(contractId);
                    } else {
                        requireActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, RaiseConflictFragment.newInstance(contractId))
                                .addToBackStack(null)
                                .commit();
                    }
                });

                holder.itemView.setOnClickListener(v -> {
                    TaskDetailsBottomSheet sheet = TaskDetailsBottomSheet.newInstance(job.toString());
                    sheet.show(getChildFragmentManager(), "TaskDetails");
                });

            } catch (Exception e) { e.printStackTrace(); }
        }

        @Override
        public int getItemCount() { return contractList.size(); }

        class JobViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemClientJobBinding itemBinding;
            JobViewHolder(ItemClientJobBinding itemBinding) {
                super(itemBinding.getRoot());
                this.itemBinding = itemBinding;
            }
        }
    }

    private void openAttachment(String attachment) {
        String url;
        if (attachment.startsWith("[")) {
            try {
                JSONArray array = new JSONArray(attachment);
                url = Constants.URL + "storage/" + array.optString(0);
            } catch (Exception e) {
                url = Constants.URL + "storage/" + attachment;
            }
        } else {
            url = Constants.URL + "storage/" + attachment;
        }
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private void showViewSubmissionsDialog(List<JSONObject> submissions) {
        if (submissions == null || submissions.isEmpty()) return;

        androidx.recyclerview.widget.RecyclerView rv = new androidx.recyclerview.widget.RecyclerView(requireContext());
        rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setPadding(20, 20, 20, 20);

        ViewSubmissionsAdapter viewAdapter = new ViewSubmissionsAdapter(submissions);
        rv.setAdapter(viewAdapter);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Submitted Work")
                .setView(rv)
                .setPositiveButton("Close", null)
                .show();
    }

    private class ViewSubmissionsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ViewSubmissionsAdapter.ViewViewHolder> {
        private final List<JSONObject> list;

        ViewSubmissionsAdapter(List<JSONObject> list) { this.list = list; }

        @NonNull
        @Override
        public ViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_submission_manage, parent, false);
            return new ViewViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewViewHolder holder, int position) {
            JSONObject sub = list.get(position);
            String attachment = sub.optString("attachment", "");
            holder.txtFileName.setText("Submission #" + sub.optInt("id") + " - " + attachment);
            
            holder.btnView.setOnClickListener(v -> openAttachment(attachment));
            holder.btnDelete.setVisibility(View.GONE); // Clients cannot delete freelancer work
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ViewViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.TextView txtFileName;
            android.widget.ImageButton btnView, btnDelete;
            ViewViewHolder(View itemView) {
                super(itemView);
                txtFileName = itemView.findViewById(R.id.txtFileName);
                btnView = itemView.findViewById(R.id.btnView);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
