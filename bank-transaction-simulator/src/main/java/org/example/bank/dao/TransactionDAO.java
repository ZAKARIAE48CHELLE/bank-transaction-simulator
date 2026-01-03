package org.example.bank.dao;

import org.example.bank.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

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

    private void log(String type,
                     String fromRef,
                     String toRef,
                     double amount) {

        String sql = """
            INSERT INTO transactions
            (type, from_account_ref, to_account_ref, amount)
            VALUES (?, ?, ?, ?)
        """;

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setString(1, type);
            stmt.setString(2, fromRef);
            stmt.setString(3, toRef);
            stmt.setDouble(4, amount);

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
