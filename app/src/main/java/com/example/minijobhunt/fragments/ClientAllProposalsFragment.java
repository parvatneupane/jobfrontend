package com.example.minijobhunt.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.minijobhunt.controller.ChatController;
import com.example.minijobhunt.controller.ContractController;
import com.example.minijobhunt.controller.PaymentController;
import com.example.minijobhunt.controller.ProposalController;
import com.example.minijobhunt.controller.ProfileController;
import com.example.minijobhunt.controller.TaskController;
import com.example.minijobhunt.controller.VerificationController;
import com.example.minijobhunt.databinding.FragmentClientAllProposalsBinding;
import com.example.minijobhunt.databinding.ItemClientProposalBinding;
import com.example.minijobhunt.utils.Constants;
import com.example.minijobhunt.utils.UtilsFunctions;
import com.example.minijobhunt.views.PaymentActivity;


import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClientAllProposalsFragment extends Fragment {

    private FragmentClientAllProposalsBinding binding;
    private ProposalController proposalController;
    private VerificationController verificationController;
    private ContractController contractController;
    private ChatController chatController;
    private TaskController taskController;
    private PaymentController paymentController;
    private ProfileController profileController;
    private List<JSONObject> proposalList = new ArrayList<>();
    private ProposalAdapter adapter;
    private int jobId = -1;
    private String jobTitle = "";
    private Map<Integer, Boolean> verificationCache = new HashMap<>();
    private JSONObject selectedProposalForHire;
    private int currentContractId = -1;
    private String jobBudget = "0";
    private String currentTransactionId = "";

    private final ActivityResultLauncher<Intent> paymentLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    boolean success = result.getData().getBooleanExtra("payment_success", false);
                    if (success) {
                        currentTransactionId = result.getData().getStringExtra("payment_message");
                        if (selectedProposalForHire != null) {
                            performHire(selectedProposalForHire);
                        }
                    } else {
                        Toast.makeText(requireContext(), "Payment Failed or Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentClientAllProposalsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        proposalController = new ProposalController();
        verificationController = new VerificationController();
        contractController = new ContractController();
        chatController = new ChatController();
        taskController = new TaskController();
        paymentController = new PaymentController();
        profileController = new ProfileController();

        if (getArguments() != null) {
            jobId = getArguments().getInt("job_id", -1);
            jobTitle = getArguments().getString("job_title", "Applicants");
            jobBudget = getArguments().getString("job_budget", "0");

            binding.txtJobTitleHeader.setText(jobTitle);
        }
        binding.btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        setupRecyclerView();
        loadProposals();
    }





    private void performHire(JSONObject prop) {
        selectedProposalForHire = prop;
        try {
            SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
            String token = "Bearer " + pref.getString("token", "");
            int currentUserId = pref.getInt("userid", 0);

            int freelancerId = prop.getJSONObject("user").getInt("id");
            int proposalId = prop.getInt("id");
            String takesTimeStr = prop.getString("takes_time");

            // Extract numeric part from takes_time (e.g., "5 Days" -> 5)
            int days = 1; // Default
            try {
                days = Integer.parseInt(takesTimeStr.replaceAll("[^0-9]", ""));
            } catch (Exception e) {}

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            String startDate = sdf.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, days);
            String deadline = sdf.format(cal.getTime());

            Map<String, Object> contractBody = new HashMap<>();
            contractBody.put("task_id", jobId);
            contractBody.put("proposal_id", proposalId);
            contractBody.put("client_id", currentUserId);
            contractBody.put("freelancer_id", freelancerId);
            contractBody.put("start_date", startDate);
            contractBody.put("deadline", deadline);
            contractBody.put("status", "pending"); // Set to pending until payment

            contractController.createContract(token, contractBody).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            JSONObject root = new JSONObject(response.body().string());
                            JSONObject contract = root.getJSONObject("data");
                            currentContractId = contract.getInt("id");

                            // Step 2: Store Payment in Backend after Contract is created
                            storePaymentOnBackend(token, currentContractId, freelancerId, currentUserId);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (response.code() == 409) {
                        Toast.makeText(requireContext(), "Contract already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Failed to create contract", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void storePaymentOnBackend(String token, int contractId, int freelancerId, int clientId) {
        Map<String, Object> paymentBody = new HashMap<>();
        paymentBody.put("contract_id", contractId);
        paymentBody.put("client_id", clientId);
        paymentBody.put("freelancer_id", freelancerId);
        
        // Ensure amount is numeric (Double) so Laravel's numeric validator passes
        try {
            String cleanBudget = jobBudget.replaceAll("[^0-9.]", "");
            double amt = Double.parseDouble(cleanBudget);
            paymentBody.put("amount", amt);
        } catch (Exception e) {
            paymentBody.put("amount", 0.0);
        }

        paymentBody.put("payment_method", "esewa");
        
        // Ensure transaction_id is not null
        String txId = (currentTransactionId == null || currentTransactionId.isEmpty()) 
                      ? "ESEWA_" + System.currentTimeMillis() : currentTransactionId;
        paymentBody.put("transaction_id", txId);

        paymentController.storePayment(token, paymentBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    // Step 3: Update Task Status to 'in_progress'
                    updateTaskStatus(token, jobId);

                    // Step 4: Update Proposal Statuses
                    try {
                        rejectOtherProposals(token, selectedProposalForHire.getInt("id"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Step 5 & 6: Create Chat and First Message
                    createChat(token, contractId, freelancerId, clientId);
                } else {
                    try {
                        String error = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        android.util.Log.e("PaymentError", error);
                    } catch (Exception e) {}
                    Toast.makeText(requireContext(), "Payment storage failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Payment Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createChat(String token, int contractId, int freelancerId, int clientId) {
        Map<String, Object> chatBody = new HashMap<>();
        chatBody.put("contract_id", contractId);
        chatBody.put("task_id", jobId);
        chatBody.put("client_id", clientId);
        chatBody.put("freelancer_id", freelancerId);
        chatBody.put("title", jobTitle);

        chatController.createChat(token, chatBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONObject chat = root.getJSONObject("data");
                        int chatId = chat.getInt("id");

                        createFirstMessage(token, chatId, clientId);

                        Toast.makeText(requireContext(), "Hired Successfully! Chat opened.", Toast.LENGTH_SHORT).show();
                        loadProposals(); // Refresh list to show statuses
                    } catch (Exception e) {
                        e.printStackTrace();
                        loadProposals();
                    }
                } else if (response.code() == 409) {
                    Toast.makeText(requireContext(), "Hired! (Chat already exists)", Toast.LENGTH_SHORT).show();
                    loadProposals();
                } else {
                    Toast.makeText(requireContext(), "Contract created, but failed to open chat", Toast.LENGTH_SHORT).show();
                    loadProposals();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(requireContext(), "Contract created, but Chat Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                loadProposals();
            }
        });
    }

    private void createFirstMessage(String token, int chatId, int clientId) {
        Map<String, Object> messageBody = new HashMap<>();
        messageBody.put("chat_id", chatId);
        messageBody.put("sender_id", clientId);
        messageBody.put("message", "Hello! I have hired you for the project: " + jobTitle);

        chatController.sendMessage(token, messageBody).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { }
        });
    }

    private void updateTaskStatus(String token, int jobId) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "in_progress");

        taskController.updateTask(token, jobId, body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { }
        });
    }

    private void rejectOtherProposals(String token, int hiredProposalId) {
        for (JSONObject prop : proposalList) {
            try {
                int pid = prop.getInt("id");
                if (pid != hiredProposalId) {
                    proposalController.updateProposal(token, pid, 
                        RequestBody.create(okhttp3.MediaType.parse("text/plain"), "PUT"),
                        RequestBody.create(okhttp3.MediaType.parse("text/plain"), prop.getString("description")),
                        RequestBody.create(okhttp3.MediaType.parse("text/plain"), prop.getString("takes_time")),
                        RequestBody.create(okhttp3.MediaType.parse("text/plain"), "rejected"),
                        null
                    ).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) { }
                    });
                } else {
                    // Update hired proposal to accepted
                    proposalController.updateProposal(token, pid,
                        RequestBody.create(okhttp3.MediaType.parse("text/plain"), "PUT"),
                        RequestBody.create(okhttp3.MediaType.parse("text/plain"), prop.getString("description")),
                        RequestBody.create(okhttp3.MediaType.parse("text/plain"), prop.getString("takes_time")),
                        RequestBody.create(okhttp3.MediaType.parse("text/plain"), "accepted"),
                        null
                    ).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) { }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) { }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new ProposalAdapter();
        binding.rvProposals.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvProposals.setAdapter(adapter);
    }

    private void loadProposals() {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        proposalController.getProposals(token).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONArray data = root.getJSONArray("data");

                        proposalList.clear();
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject prop = data.getJSONObject(i);
                            if (prop.optInt("task_id") == jobId) {
                                proposalList.add(prop);
                            }
                        }
                        
                        binding.txtApplicantCount.setText(proposalList.size() + " Total");
                        adapter.notifyDataSetChanged();

                        if (proposalList.isEmpty()) {
                            binding.txtEmpty.setVisibility(View.VISIBLE);
                            binding.rvProposals.setVisibility(View.GONE);
                        } else {
                            binding.txtEmpty.setVisibility(View.GONE);
                            binding.rvProposals.setVisibility(View.VISIBLE);
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

    private void fetchVerificationStatus(int userId) {
        if (verificationCache.containsKey(userId)) return;

        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        verificationController.getVerification(token, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    boolean isVerified = false;
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        JSONObject data = root.optJSONObject("data");
                        if (data != null) {
                            isVerified = "approved".equalsIgnoreCase(data.optString("status", ""));
                        }
                    }
                    verificationCache.put(userId, isVerified);
                    if (isVerified && adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    verificationCache.put(userId, false);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                verificationCache.put(userId, false);
            }
        });
    }

    private void fetchFreelancerRating(int userId, android.widget.TextView txtRating) {
        SharedPreferences pref = requireActivity().getSharedPreferences(Constants.cache, Context.MODE_PRIVATE);
        String token = "Bearer " + pref.getString("token", "");

        profileController.getFreelancerProfile(token, userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        JSONObject root = new JSONObject(response.body().string());
                        Object data = root.get("data");
                        JSONObject profile = null;

                        if (data instanceof JSONArray) {
                            JSONArray array = (JSONArray) data;
                            if (array.length() > 0) profile = array.getJSONObject(0);
                        } else if (data instanceof JSONObject) {
                            profile = (JSONObject) data;
                        }

                        if (profile != null) {
                            String rating = profile.optString("rating", "0.0");
                            txtRating.setText(rating);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {}
        });
    }

    private class ProposalAdapter extends RecyclerView.Adapter<ProposalAdapter.ProposalViewHolder> {

        @NonNull
        @Override
        public ProposalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemClientProposalBinding itemBinding = ItemClientProposalBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new ProposalViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ProposalViewHolder holder, int position) {
            JSONObject prop = proposalList.get(position);
            try {
                JSONObject user = prop.getJSONObject("user");
                int userId = user.getInt("id");
                
                holder.itemBinding.txtFreelancerName.setText(user.getString("name"));
                holder.itemBinding.txtDescription.setText(prop.getString("description"));
                holder.itemBinding.txtTime.setText(prop.getString("takes_time"));
                String status = prop.getString("status").toLowerCase();
                holder.itemBinding.txtStatus.setText(status.toUpperCase());
                
                if (status.equals("accepted")) {
                    holder.itemBinding.btnHire.setText("Accepted");
                    holder.itemBinding.btnHire.setEnabled(false);
                    holder.itemBinding.btnHire.setAlpha(0.6f);
                    holder.itemBinding.btnHire.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success)));
                } else if (status.equals("rejected")) {
                    holder.itemBinding.btnHire.setText("Rejected");
                    holder.itemBinding.btnHire.setEnabled(false);
                    holder.itemBinding.btnHire.setAlpha(0.6f);
                    holder.itemBinding.btnHire.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent)));
                } else {
                    holder.itemBinding.btnHire.setText("Hire Now");
                    holder.itemBinding.btnHire.setEnabled(true);
                    holder.itemBinding.btnHire.setAlpha(1.0f);
                    holder.itemBinding.btnHire.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)));
                }
                
                String date = prop.optString("created_at", "");
                holder.itemBinding.txtProposalDate.setText(UtilsFunctions.getTimeAgo(date));

                // Verification Badge
                if (verificationCache.containsKey(userId)) {
                    holder.itemBinding.imgVerifiedBadge.setVisibility(verificationCache.get(userId) ? View.VISIBLE : View.GONE);
                } else {
                    holder.itemBinding.imgVerifiedBadge.setVisibility(View.GONE);
                    fetchVerificationStatus(userId);
                }

                // Fetch and Set Rating
                fetchFreelancerRating(userId, holder.itemBinding.txtRating);

                // Profile Image
                String photo = user.optString("profile_photo", "");


                holder.itemBinding.btnHire.setOnClickListener(v -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Hire Freelancer")
                            .setMessage("To hire " + holder.itemBinding.txtFreelancerName.getText() + ", you need to pay the project amount. Proceed to hire?")
                            .setPositiveButton("Pay & Hire", (dialog, which) -> {
                                selectedProposalForHire = prop;
                                Intent intent = new Intent(requireContext(), PaymentActivity.class);

                                // Reverting to original amount and ID format
                                String cleanBudget = jobBudget.replaceAll("[^0-9.]", "");
                                intent.putExtra("amount", cleanBudget);
                                intent.putExtra("productName", "HireFreelancer");
                                
                                // Reverting to your original productId format
                                String uniqueId = jobId + "_" + System.currentTimeMillis();
                                intent.putExtra("productId", uniqueId);

                                paymentLauncher.launch(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });

                holder.itemBinding.btnViewDetails.setOnClickListener(v -> {
                    FreeLancerPortfolioFragment fragment = new FreeLancerPortfolioFragment();
                    Bundle args = new Bundle();
                    args.putInt("profile_id", userId);
                    try {
                        args.putString("user_name", user.getString("name"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    fragment.setArguments(args);

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
                });

                holder.itemView.setOnClickListener(v -> {
                    String ratingStr = holder.itemBinding.txtRating.getText().toString();
                    ProposalDetailsBottomSheet sheet = ProposalDetailsBottomSheet.newInstance(prop.toString(), ratingStr);
                    sheet.show(getChildFragmentManager(), "ProposalDetails");
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }





        @Override
        public int getItemCount() {
            return proposalList.size();
        }

        class ProposalViewHolder extends RecyclerView.ViewHolder {
            ItemClientProposalBinding itemBinding;
            ProposalViewHolder(ItemClientProposalBinding itemBinding) {
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
