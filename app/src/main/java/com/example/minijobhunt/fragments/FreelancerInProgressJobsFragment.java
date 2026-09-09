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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.minijobhunt.R;
import com.example.minijobhunt.controller.ContractController;
import com.example.minijobhunt.controller.SubmissionController;
import com.example.minijobhunt.databinding.FragmentFreelancerInProgressJobsBinding;
import com.example.minijobhunt.databinding.ItemFreelancerJobBinding;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.utils.UtilsFunctions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FreelancerInProgressJobsFragment extends Fragment {

    private FragmentFreelancerInProgressJobsBinding binding;
    private ContractController contractController;
    private SubmissionController submissionController;
    private List<JSONObject> contractList = new ArrayList<>();
    private Map<Integer, List<JSONObject>> submissionCache = new HashMap<>(); // contract_id -> submissions
    private ContractAdapter adapter;
    private int selectedContractId = -1;

    private final ActivityResultLauncher<Intent> filePicker = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    List<Uri> uris = new ArrayList<>();
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            uris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        uris.add(result.getData().getData());
                    }

                    if (!uris.isEmpty() && selectedContractId != -1) {
                        uploadWorkFiles(selectedContractId, uris);
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFreelancerInProgressJobsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        contractController = new ContractController();
        submissionController = new SubmissionController();
        setupRecyclerView();
        loadActiveContracts();
    }

    private void setupRecyclerView() {
        adapter = new ContractAdapter();
        binding.recyclerViewJobs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewJobs.setAdapter(adapter);
    }

    private void loadActiveContracts() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int currentUserId = pref.getInt("userid", 0);

        // Fetch submissions first to see what's already uploaded
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
                            if (contract.optInt("freelancer_id") == currentUserId && 
                                contract.optString("status").equalsIgnoreCase("active")) {
                                contractList.add(contract);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (contractList.isEmpty()) {
                            binding.txtEmpty.setVisibility(View.VISIBLE);
                            binding.recyclerViewJobs.setVisibility(View.GONE);
                        } else {
                            binding.txtEmpty.setVisibility(View.GONE);
                            binding.recyclerViewJobs.setVisibility(View.VISIBLE);
                        }
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

    private void uploadWorkFiles(int contractId, List<Uri> fileUris) {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");
        int currentUserId = pref.getInt("userid", 0);

        List<MultipartBody.Part> attachments = new ArrayList<>();
        for (Uri uri : fileUris) {
            File file = UtilsFunctions.uriToFile(requireContext(), uri);
            if (file != null) {
                String mimeType = requireContext().getContentResolver().getType(uri);
                if (mimeType == null) mimeType = "application/octet-stream";
                RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
                attachments.add(MultipartBody.Part.createFormData("attachment", file.getName(), requestFile));
            }
        }

        if (attachments.isEmpty()) return;

        RequestBody rbContractId = RequestBody.create(MultipartBody.FORM, String.valueOf(contractId));
        RequestBody rbFreelancerId = RequestBody.create(MultipartBody.FORM, String.valueOf(currentUserId));
        RequestBody rbMessage = RequestBody.create(MultipartBody.FORM, "Work submission for contract #" + contractId);

        submissionController.submitWork(token, rbContractId, rbFreelancerId, rbMessage, attachments).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Work submitted successfully", Toast.LENGTH_SHORT).show();
                    loadActiveContracts();
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "No error body";
                        Toast.makeText(requireContext(), "Failed to submit: " + response.code() + " - " + errorBody, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Failed to submit: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openConflictDetails(int contractId) {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ConflictDetailsFragment.newInstance(contractId))
                .addToBackStack(null)
                .commit();
    }

    private class ContractAdapter extends RecyclerView.Adapter<ContractAdapter.ContractViewHolder> {
        @NonNull
        @Override
        public ContractViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemFreelancerJobBinding itemBinding = ItemFreelancerJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ContractViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ContractViewHolder holder, int position) {
            JSONObject contract = contractList.get(position);
            try {
                JSONObject job = contract.getJSONObject("task");
                JSONObject client = contract.optJSONObject("client");

                holder.itemBinding.txtJobTitle.setText(job.getString("title"));
                
                String clientName = client != null ? client.optString("name", "Client") : "Client";
                holder.itemBinding.txtClientName.setText(clientName);
                
                String deadline = contract.optString("deadline", "N/A");
                holder.itemBinding.txtJobDetails.setText("Hired by " + clientName + " • Due: " + UtilsFunctions.formatDate(deadline));
                holder.itemBinding.txtJobBudget.setText("Budget: Rs. " + job.getString("budget"));
                
                holder.itemBinding.btnApply.setVisibility(View.GONE);
                holder.itemBinding.layoutWork.setVisibility(View.VISIBLE);

                int contractId = contract.optInt("id");
                List<JSONObject> submissions = submissionCache.get(contractId);
                if (submissions != null && !submissions.isEmpty()) {
                    JSONObject latestSubmission = submissions.get(submissions.size() - 1);
                    String attachment = latestSubmission.optString("attachment", "");
                    
                    holder.itemBinding.txtWorkFile.setVisibility(View.VISIBLE);
                    holder.itemBinding.btnManageWork.setVisibility(View.VISIBLE);
                    holder.itemBinding.btnSubmitWork.setText("Add New File");
                    
                    holder.itemBinding.txtWorkFile.setOnClickListener(v -> {
                        openAttachment(attachment);
                    });
                } else {
                    holder.itemBinding.txtWorkFile.setVisibility(View.GONE);
                    holder.itemBinding.btnManageWork.setVisibility(View.GONE);
                    holder.itemBinding.btnSubmitWork.setText("Submit Final Work");
                }

                String taskStatus = job.optString("status");

                if (taskStatus.equalsIgnoreCase("under_review")) {
                    holder.itemBinding.btnSubmitWork.setEnabled(false);
                    holder.itemBinding.btnSubmitWork.setAlpha(0.5f);
                    holder.itemBinding.btnSubmitWork.setText("Locked (Under Review)");
                    
                    holder.itemBinding.btnRaiseConflict.setText("View Conflict Details");
                    holder.itemBinding.btnRaiseConflict.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
                } else {
                    holder.itemBinding.btnSubmitWork.setEnabled(true);
                    holder.itemBinding.btnSubmitWork.setAlpha(1.0f);
                    holder.itemBinding.btnRaiseConflict.setText("Problem? Raise a Conflict");
                    holder.itemBinding.btnRaiseConflict.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent));
                }

                holder.itemBinding.btnSubmitWork.setOnClickListener(v -> {
                    if (taskStatus.equalsIgnoreCase("under_review")) {
                        Toast.makeText(requireContext(), "Uploads locked during conflict review", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    selectedContractId = contractId;
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    filePicker.launch(intent);
                });

                holder.itemBinding.btnManageWork.setOnClickListener(v -> {
                    showManageSubmissionsDialog(submissions);
                });

                holder.itemBinding.btnRaiseConflict.setOnClickListener(v -> {
                    if (taskStatus.equalsIgnoreCase("under_review")) {
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

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() { return contractList.size(); }

        class ContractViewHolder extends RecyclerView.ViewHolder {
            ItemFreelancerJobBinding itemBinding;
            ContractViewHolder(ItemFreelancerJobBinding itemBinding) {
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

    private void showManageSubmissionsDialog(List<JSONObject> submissions) {
        if (submissions == null || submissions.isEmpty()) return;

        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setPadding(20, 20, 20, 20);

        ManageSubmissionsAdapter manageAdapter = new ManageSubmissionsAdapter(submissions);
        rv.setAdapter(manageAdapter);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Manage Submissions")
                .setView(rv)
                .setPositiveButton("Close", null)
                .show();
    }

    private class ManageSubmissionsAdapter extends RecyclerView.Adapter<ManageSubmissionsAdapter.ManageViewHolder> {
        private List<JSONObject> list;

        ManageSubmissionsAdapter(List<JSONObject> list) { this.list = list; }

        @NonNull
        @Override
        public ManageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_submission_manage, parent, false);
            return new ManageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ManageViewHolder holder, int position) {
            JSONObject sub = list.get(position);
            String attachment = sub.optString("attachment", "");
            holder.txtFileName.setText("Submission #" + sub.optInt("id") + " - " + attachment);
            
            holder.btnView.setOnClickListener(v -> openAttachment(attachment));
            holder.btnDelete.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Delete Submission")
                        .setMessage("Are you sure you want to delete this submission?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            deleteSubmission(sub.optInt("id"));
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class ManageViewHolder extends RecyclerView.ViewHolder {
            android.widget.TextView txtFileName;
            android.widget.ImageButton btnView, btnDelete;
            ManageViewHolder(View itemView) {
                super(itemView);
                txtFileName = itemView.findViewById(R.id.txtFileName);
                btnView = itemView.findViewById(R.id.btnView);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }

    private void deleteSubmission(int submissionId) {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        submissionController.deleteSubmission(token, submissionId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Submission deleted", Toast.LENGTH_SHORT).show();
                    loadActiveContracts();
                } else {
                    Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
