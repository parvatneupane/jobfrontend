# eSewa Integration — Implementation Reference

## 1. Overview
The codebase currently implements the **Method A: Deprecated Android SDK** integration path for eSewa. This integration relies on a pre-compiled `.aar` library (`eSewaSdk.aar`) to provide the payment interface within the Android app. Payments are initiated client-side using test credentials hardcoded in the source code, and once a transaction is completed, a confirmation record is sent to a custom backend for storage. This method is officially deprecated by eSewa in favor of ePay v2.

## 2. Files Involved

### Backend controllers / routes / services
*Not implemented* (Backend source code not found in this repository; only API endpoints are visible from Android code).

### Backend .env / config files
*Not implemented* (Source not found).

### Android Activities / Fragments
- `app/src/main/java/com/example/minijobhunt/views/PaymentActivity.java`: Main entry point for initializing the eSewa SDK and launching the payment screen.
- `app/src/main/java/com/example/minijobhunt/fragments/ClientAllProposalsFragment.java`: Triggers the `PaymentActivity` when a client hires a freelancer.
- `app/src/main/java/com/example/minijobhunt/fragments/ClientInProgressJobsFragment.java`: Calls the release payment endpoint when a job is completed.
- `app/src/main/java/com/example/minijobhunt/controller/PaymentController.java`: Android controller managing API calls to backend payment endpoints.

### Android layout XML files related to payment UI
- `app/src/main/res/layout/activity_payment.xml`: Layout for the intermediate `PaymentActivity`.

### Database migration files or schema files
*Not implemented* (Source not found).

## 3. Environment Configuration

### Android Source hardcoding (**CRITICAL SECURITY ISSUE**)
- **Merchant Code (CLIENT_ID):** Hardcoded as `"JB0BBQ4aD0UqIThFJwAKBgAXEUkEGQUBBAwdOgABHD4DChwUAB0R"` in `PaymentActivity.java`.
- **Secret Key:** Hardcoded as `"BhwIWQQADhIYSxILExMcAgFXFhcOBwAKBgAXEQ=="` in `PaymentActivity.java`.

### Status
- **Environment:** Currently set to `EsewaConfiguration.ENVIRONMENT_TEST`.
- **URLs:** SDK internally uses `rc-epay` sandbox endpoints.
- **Callback URL:** `https://yourcallbackurl.com` (Dummy placeholder found in `PaymentActivity.java`).

## 4. Signature Generation
The HMAC-SHA256 signature generation required by modern eSewa integrations is **not implemented** in the visible codebase. Because this project uses the deprecated SDK, the signature is likely handled internally by the library using the hardcoded `SECRET_KEY`.

**CRITICAL:** Signature generation should never happen on the Android side. If migrated to ePay v2, this logic must be moved to the backend.

## 5. Full Payment Flow (as implemented)

1. Customer taps the "Pay & Hire" button in `ClientAllProposalsFragment.java`.
2. Android launches `PaymentActivity.java` via an `ActivityResultLauncher`.
3. `PaymentActivity` initializes `EsewaConfiguration` with hardcoded test keys and launches the SDK's `EsewaPaymentActivity`.
4. The customer completes the payment within the SDK UI.
5. eSewa SDK returns the result to `PaymentActivity.onActivityResult`.
6. `PaymentActivity` returns the success status and message (transaction ID) back to `ClientAllProposalsFragment`.
7. `ClientAllProposalsFragment` calls `performHire()`, which first creates a contract record on the backend.
8. Upon successful contract creation, `storePaymentOnBackend()` is called, sending a `POST` request to `/api/payments`.
9. **SECURITY GAP:** The backend relies on the client (Android app) to report the payment success and transaction ID. There is no evidence of a server-to-server verification step to validate the transaction with eSewa's status endpoint before updating the database.

## 6. Database Schema
Based on the `storePaymentOnBackend` payload, the following columns are expected to exist in the backend `payments` table:

| Column | Type | Description |
| :--- | :--- | :--- |
| `contract_id` | Integer | Foreign key to contracts table |
| `client_id` | Integer | ID of the paying user |
| `freelancer_id` | Integer | ID of the recipient |
| `amount` | Double | The payment amount |
| `payment_method` | String | Hardcoded as `"esewa"` |
| `transaction_id` | String | The ID returned by eSewa SDK |

**SECURITY GAP:** It is unknown if `transaction_id` (or `transaction_uuid` equivalent) has a `UNIQUE` constraint in the database, which is necessary to prevent duplicate storage of the same payment.

## 7. Security Review

| Requirement | Status | Evidence / Note |
| :--- | :--- | :--- |
| Server-side re-verification | **FAIL** | Code trusts the Android app to report success; no backend verification logic found. |
| Amount comparison | **FAIL** | No evidence of server-side comparison. |
| Unique Transaction ID | **UNKNOWN** | Database schema not visible, but no check found in Android code. |
| Conditional status updates | **FAIL** | Backend just "stores" the record based on Android's request. |
| Secret key protection | **CRITICAL FAIL** | `SECRET_KEY` is hardcoded in `PaymentActivity.java`. |
| HTTPS Usage | **PASS** | `Constants.BASE_URL` uses an IP but `RestApi` calls are intended for HTTPS in production. |
| Callback Signature Validation | **FAIL** | Not implemented. |

**Recommended fix:** Migrate to ePay v2, move all credentials to backend `.env`, and implement server-side verification using eSewa's status API.

## 8. Refund Handling
Partial refund logic exists in the Android `RestApi.java` and `PaymentController.java` (`PUT /api/payments/{id}/refund`), but it is not triggered anywhere in the visible fragment code.
**Note:** eSewa does not have a public automated refund API; this endpoint likely marks a payment as "refunded" in the local database for manual processing.

## 9. Test Credentials Currently Used
The implementation uses standard eSewa sandbox credentials:
- **Environment:** TEST
- **Merchant ID/Client ID:** JB0BBQ4a...
- **Secret Key:** BhwIWQQA...

## 10. Switching to Production — Checklist
- [ ] Replace `libs/eSewaSdk.aar` with ePay v2 web-based integration (Recommended).
- [ ] Move `SECRET_KEY` and `CLIENT_ID` from `PaymentActivity.java` to backend environment variables.
- [ ] Change `EsewaConfiguration.ENVIRONMENT_TEST` to `ENVIRONMENT_LIVE`.
- [ ] Update `CALLBACK_URL` to a valid listener endpoint.
- [ ] Implement a backend transaction status verification job.
- [ ] Update all payment-related strings and identifiers to production values provided by eSewa.
