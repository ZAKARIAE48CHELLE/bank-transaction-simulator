package org.example.bank.dao;

import org.example.bank.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public void logDeposit(String toRef, double amount) {
        log("DEPOSIT", null, toRef, amount);
    }

    public void logWithdraw(String fromRef, double amount) {
        log("WITHDRAW", fromRef, null, amount);
    }

    public void logTransfer(String fromRef, String toRef, double amount) {
        log("TRANSFER", fromRef, toRef, amount);
    }

    public int log(String type,
                   String fromRef,
                   String toRef,
                   double amount) {

        String sql = """
        INSERT INTO transactions
        (type, from_account_ref, to_account_ref, amount, status)
        VALUES (?, ?, ?, ?, 'PENDING')
    """;

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt =
                    conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);

            stmt.setString(1, type);
            stmt.setString(2, fromRef);
            stmt.setString(3, toRef);
            stmt.setDouble(4, amount);

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1); // transaction ID
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    public List<String> findHistoryByAccountRef(String accountRef) {

        List<String> history = new ArrayList<>();

        String sql = """
        SELECT type, from_account_ref, to_account_ref, amount, status, created_at
        FROM transactions
        WHERE from_account_ref = ? OR to_account_ref = ?
        ORDER BY created_at DESC
    """;

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, accountRef);
            stmt.setString(2, accountRef);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                history.add(
                        rs.getString("created_at") + " | "
                                + rs.getString("type") + " | "
                                + rs.getString("from_account_ref") + " -> "
                                + rs.getString("to_account_ref") + " | "
                                + rs.getDouble("amount") + " | "
                                + rs.getString("status")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return history;
    }
    public void markDone(int transactionId) {

        String sql = "UPDATE transactions SET status = 'DONE' WHERE id = ?";

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, transactionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void markFailed(int transactionId) {

        String sql = "UPDATE transactions SET status = 'FAILED' WHERE id = ?";

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, transactionId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
