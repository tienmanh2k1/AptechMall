# Bank Transfer SMS Integration

## Overview

This document describes the SMS-based bank transfer integration for automatic wallet deposits. The system receives SMS notifications from banks when users make transfers, parses the SMS content, and automatically credits the user's wallet.

**⚠️ IMPORTANT:** This is a development/testing feature. For production, use official payment gateways (VNPay, MoMo, ZaloPay).

## Architecture

### Components

1. **BankSms Entity** - Stores SMS messages from banks
2. **BankSmsRepository** - Database queries for SMS records
3. **BankSmsParserService** - Parses SMS content to extract transaction info
4. **BankTransferService** - Processes SMS and creates wallet deposits
5. **BankTransferController** - Webhook endpoint for receiving SMS

### Flow Diagram

```
[Bank] → [SMS] → [Smartphone App] → [Webhook API] → [Parser] → [WalletService] → [User Wallet]
```

## How It Works

### 1. User Makes Bank Transfer

User transfers money to your bank account with specific format in transfer note:
```
Nap tien USER123
```

Where `123` is the user ID in the system.

### 2. Bank Sends SMS

Bank sends SMS notification:
```
TK 1234567890 +500,000 VND. GD: 987654321. ND: Nap tien USER123
```

### 3. SMS Forwarding

Use a smartphone app (e.g., SMS Forwarder) to forward bank SMS to your webhook:
```
POST http://your-domain.com/api/bank-transfer/sms-webhook?sender=VIETCOMBANK&message=...
```

### 4. Automatic Processing

- System parses SMS to extract:
  - Amount: 500,000 VND
  - Transaction Reference: GD987654321
  - User ID: 123
- Creates deposit transaction in user's wallet
- Marks SMS as processed

## Database Schema

### bank_sms Table

```sql
CREATE TABLE bank_sms (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender VARCHAR(50),                    -- Bank identifier
    message TEXT,                          -- SMS content
    raw LONGTEXT,                          -- Full SMS body
    received_at DATETIME,                  -- When SMS received
    processed BOOLEAN DEFAULT FALSE,       -- Processing status
    processed_at DATETIME,                 -- When processed
    parsed_amount DECIMAL(12,2),          -- Extracted amount
    transaction_reference VARCHAR(100),    -- GD number
    extracted_user_id BIGINT,             -- User ID from content
    deposit_created BOOLEAN DEFAULT FALSE, -- Deposit status
    wallet_transaction_id BIGINT,         -- Created transaction ID
    error_message TEXT,                    -- Error if any
    created_at DATETIME,
    updated_at DATETIME,
    INDEX idx_sms_processed (processed),
    INDEX idx_sms_received_at (received_at)
);
```

## API Endpoints

### Base URL: `/api/bank-transfer`

All endpoints are public (no authentication required) to allow webhook access.

### 1. SMS Webhook (POST/GET)

Receive bank SMS notifications.

```
POST/GET /api/bank-transfer/sms-webhook
```

**Query Parameters:**
- `sender` (string) - Bank identifier (e.g., "VIETCOMBANK")
- `message` (string) - SMS content
- `raw` (string, optional) - Full SMS body

**Example:**
```bash
curl "http://localhost:8080/api/bank-transfer/sms-webhook?sender=VIETCOMBANK&message=TK+1234567890+%2B500000+VND+GD+123456+ND+NAPTIEN+USER123"
```

**Response:**
```json
{
  "success": true,
  "message": "SMS received and processed",
  "data": {
    "status": "success",
    "smsId": 1,
    "processed": true,
    "depositCreated": true,
    "error": ""
  }
}
```

### 2. Process Pending SMS (GET)

Manually trigger processing of unprocessed SMS.

```
GET /api/bank-transfer/process-pending
```

**Response:**
```json
{
  "success": true,
  "message": "Processed 5 SMS",
  "data": {
    "status": "success",
    "unprocessedCount": 5,
    "processedCount": 5,
    "data": [...]
  }
}
```

### 3. Get All SMS (GET)

Retrieve all SMS records.

```
GET /api/bank-transfer/sms
```

### 4. Get SMS by ID (GET)

Get specific SMS details.

```
GET /api/bank-transfer/sms/{id}
```

### 5. Get SMS with Errors (GET)

Get SMS that failed to process.

```
GET /api/bank-transfer/sms/errors
```

### 6. Test Webhook (GET)

Verify webhook is accessible.

```
GET /api/bank-transfer/test
```

## SMS Format Patterns

### Supported Bank SMS Formats

The parser supports multiple SMS formats from Vietnamese banks:

**Format 1: Standard bank format**
```
TK 1234567890 +500,000 VND. GD: 987654. ND: Nap tien USER123
```

**Format 2: Compact format**
```
+500000d GD:987654 ND:NAPTIEN USER123
```

**Format 3: K suffix**
```
Tai khoan +500k GD 987654
```

**Format 4: Simple test format**
```
GD 100k
```

### Extraction Patterns

**Amount Extraction:**
- `+500,000 VND` → 500000
- `+500k` → 500000
- `GD 100` → 100000 (assumes thousands)

**Transaction Reference:**
- `GD: 123456` → "GD123456"
- `GD:123456` → "GD123456"
- No GD found → "SMS{timestamp}"

**User ID Extraction:**
- `ND: Nap tien USER123` → 123
- `ND:NAPTIEN USER123` → 123
- `NAPTIEN 123` → 123
- `USER123` → 123

## Setup Guide

### Step 1: Configure Smartphone SMS Forwarder

Install an SMS forwarding app on your smartphone (e.g., "SMS Forwarder" app).

Configure it to forward SMS from your bank to:
```
URL: http://your-server.com/api/bank-transfer/sms-webhook
Method: GET
Parameters:
  - sender: {sender}
  - message: {body}
```

### Step 2: Test SMS Reception

Send test SMS to your phone or use the API directly:

```bash
curl "http://localhost:8080/api/bank-transfer/sms-webhook?sender=TEST&message=GD+100k"
```

### Step 3: Instruct Users

Tell users to include their user ID in transfer notes:

**Format:**
```
Nap tien USER{userId}
```

**Example for user ID 123:**
```
Nap tien USER123
```

### Step 4: Monitor SMS Processing

Check processing status:
```bash
curl http://localhost:8080/api/bank-transfer/sms
```

View errors:
```bash
curl http://localhost:8080/api/bank-transfer/sms/errors
```

## Security Considerations

### ⚠️ Limitations

1. **No Authentication** - Webhook is public, anyone can send fake SMS
2. **No Signature Verification** - Cannot verify SMS authenticity
3. **Depends on Smartphone** - Single point of failure
4. **SMS Delay** - Processing time depends on SMS delivery
5. **No Guaranteed Delivery** - SMS can be lost or delayed

### Recommendations

1. **For Testing Only** - Do not use in production
2. **Use Official Payment Gateways** - VNPay, MoMo, ZaloPay for production
3. **Monitor Regularly** - Check for errors and anomalies
4. **Limit Transfer Amounts** - Set maximum deposit limits
5. **Manual Verification** - Verify large transfers manually

### Preventing Abuse

1. **Duplicate Detection** - Transaction references are checked for duplicates
2. **Amount Validation** - Minimum deposit: 1000 VND (configured in DepositRequest)
3. **Error Logging** - All parsing errors are logged
4. **Manual Review** - Check SMS with errors regularly

## Troubleshooting

### SMS Not Processed

**Problem:** SMS received but not creating deposit

**Solutions:**
1. Check SMS format matches supported patterns
2. Verify user ID is included in transfer note
3. Check error logs: `GET /api/bank-transfer/sms/errors`
4. Manually trigger processing: `GET /api/bank-transfer/process-pending`

### Amount Parsing Failed

**Problem:** Cannot extract amount from SMS

**Solutions:**
1. Ensure amount includes `+` prefix or `k` suffix
2. Check for comma separators: `500,000` or `500k`
3. Review `BankSmsParserService` patterns

### User ID Not Found

**Problem:** Deposit not created because user ID missing

**Solutions:**
1. Instruct user to include `USER{id}` in transfer note
2. Example: "Nap tien USER123" for user ID 123
3. Check extracted_user_id field in database

### Duplicate Transaction

**Problem:** SMS rejected as duplicate

**Solutions:**
1. Each transaction reference (GD number) can only be used once
2. This prevents double-processing same bank transfer
3. Check if deposit was already created in first SMS

## Testing

### Manual Test with curl

**1. Simple test (100k deposit):**
```bash
curl -X POST "http://localhost:8080/api/bank-transfer/sms-webhook?sender=TEST&message=GD+100k"
```

**2. With user ID (user 1, 500k deposit):**
```bash
curl -X POST "http://localhost:8080/api/bank-transfer/sms-webhook?sender=VIETCOMBANK&message=TK+1234567890+%2B500000+VND+GD+123456+ND+NAPTIEN+USER1"
```

**3. Check processing:**
```bash
curl http://localhost:8080/api/bank-transfer/sms
```

**4. Process pending:**
```bash
curl http://localhost:8080/api/bank-transfer/process-pending
```

### Expected Results

After successful SMS processing:
1. SMS record created in `bank_sms` table
2. `processed` = true
3. `deposit_created` = true
4. User wallet balance increased
5. Transaction record in `wallet_transaction` table

## Integration with Wallet Feature

### Automatic Deposit Creation

When SMS is successfully parsed and user ID is found:

1. **WalletService.processDeposit()** is called automatically
2. **PaymentGateway.BANK_TRANSFER** is used
3. **Transaction reference** from SMS is stored
4. **User wallet** is credited with amount
5. **Transaction history** records the deposit

### Related Documentation

- `WALLET_FEATURE_IMPLEMENTATION.md` - Main wallet documentation
- `Backend/CLAUDE.md` - Backend architecture
- `CLAUDE.md` - Project overview

## Conclusion

SMS-based bank transfer is a simple but limited solution for wallet deposits. It's suitable for:

✅ Development and testing
✅ Low-volume personal projects
✅ Proof of concept demos

❌ NOT suitable for production
❌ NOT suitable for high-volume
❌ NOT suitable for mission-critical applications

**For production, use official payment gateways: VNPay, MoMo, or ZaloPay.**
