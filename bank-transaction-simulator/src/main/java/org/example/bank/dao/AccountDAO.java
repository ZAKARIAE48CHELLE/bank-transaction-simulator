package org.example.bank.dao;

import org.example.bank.DBConnection;
import org.example.bank.model.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AccountDAO {

    public List<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT id, user_id, account_ref, balance FROM accounts";

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                accounts.add(
                        new Account(
                                rs.getInt("id"),
                                rs.getInt("user_id"),
                                rs.getString("account_ref"),
                                rs.getDouble("balance")
                        )
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return accounts;
    }

    public List<Account> findByUserId(int userId) {
        List<Account> accounts = new ArrayList<>();

        String sql = """
            SELECT id, user_id, account_ref, balance
            FROM accounts
            WHERE user_id = ?
        """;

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                accounts.add(
                        new Account(
                                rs.getInt("id"),
                                rs.getInt("user_id"),
                                rs.getString("account_ref"),
                                rs.getDouble("balance")
                        )
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return accounts;
    }

    public void updateBalance(int accountId, double newBalance) {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);

            stmt.setDouble(1, newBalance);
            stmt.setInt(2, accountId);

            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Account findByAccountRef(String accountRef) {
        String sql = """
        SELECT id, user_id, account_ref, balance
        FROM accounts
        WHERE account_ref = ?
    """;

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, accountRef);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Account(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("account_ref"),
                        rs.getDouble("balance")
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ✅ Create default account for a new user
    public boolean createDefaultAccountForUser(int userId) {
        String ref = "ACC-" + userId + "-" + UUID.randomUUID().toString().substring(0, 5);

        String sql = "INSERT INTO accounts (user_id, account_ref, balance) VALUES (?, ?, 0)";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setString(2, ref);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
