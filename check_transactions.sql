-- Query lịch sử giao dịch của tất cả user
-- Chạy trong MySQL Workbench hoặc CLI

-- 1. Xem tất cả transactions (mới nhất trước)
SELECT
    wt.id AS transaction_id,
    u.user_id,
    u.username,
    u.email,
    wt.transaction_type,
    wt.amount,
    wt.balance_before,
    wt.balance_after,
    wt.description,
    wt.reference_number,
    wt.created_at
FROM wallet_transaction wt
JOIN user_wallet uw ON wt.wallet_id = uw.id
JOIN users u ON uw.user_id = u.user_id
ORDER BY wt.created_at DESC
LIMIT 50;

-- 2. Tổng hợp theo user
SELECT
    u.user_id,
    u.username,
    u.email,
    COUNT(wt.id) AS total_transactions,
    SUM(CASE WHEN wt.transaction_type = 'DEPOSIT' THEN wt.amount ELSE 0 END) AS total_deposit,
    SUM(CASE WHEN wt.transaction_type = 'ORDER_PAYMENT' THEN wt.amount ELSE 0 END) AS total_spent,
    uw.balance AS current_balance
FROM users u
LEFT JOIN user_wallet uw ON u.user_id = uw.user_id
LEFT JOIN wallet_transaction wt ON uw.id = wt.wallet_id
GROUP BY u.user_id, u.username, u.email, uw.balance
HAVING total_transactions > 0
ORDER BY u.user_id;

-- 3. Chi tiết transactions của USER1
SELECT
    wt.id,
    wt.transaction_type,
    wt.amount,
    wt.balance_before,
    wt.balance_after,
    wt.description,
    wt.reference_number,
    wt.created_at
FROM wallet_transaction wt
JOIN user_wallet uw ON wt.wallet_id = uw.id
WHERE uw.user_id = 1
ORDER BY wt.created_at DESC;

-- 4. Chi tiết transactions của USER3
SELECT
    wt.id,
    wt.transaction_type,
    wt.amount,
    wt.balance_before,
    wt.balance_after,
    wt.description,
    wt.reference_number,
    wt.created_at
FROM wallet_transaction wt
JOIN user_wallet uw ON wt.wallet_id = uw.id
WHERE uw.user_id = 3
ORDER BY wt.created_at DESC;

-- 5. Xem SMS đã xử lý và tạo deposit
SELECT
    bs.id AS sms_id,
    bs.sender,
    bs.message,
    bs.parsed_amount,
    bs.extracted_user_id,
    bs.extracted_username,
    bs.deposit_created,
    bs.wallet_transaction_id,
    bs.error_message,
    bs.received_at,
    wt.amount AS actual_deposit_amount,
    wt.balance_after AS new_balance
FROM bank_sms bs
LEFT JOIN wallet_transaction wt ON bs.wallet_transaction_id = wt.id
WHERE bs.deposit_created = TRUE
ORDER BY bs.received_at DESC;
