package org.example.bank.dao;

import org.example.bank.DBConnection;
import org.example.bank.model.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AccountDAO {

    public List<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT id, user_id, account_ref, balance FROM accounts ORDER BY id ASC";

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                accounts.add(new Account(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("account_ref"),
                        rs.getDouble("balance")
                ));
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
            ORDER BY id ASC
        """;

        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                accounts.add(new Account(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("account_ref"),
                        rs.getDouble("balance")
                ));
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

    // ✅ Création compte par défaut : account_ref = "ACC-<ID_COMPTE>"
    // Comme dans ta DB (ACC-1, ACC-2, ACC-3...)
    public boolean createDefaultAccountForUser(int userId) {
        return createDefaultAccountForUserReturnAccountId(userId) > 0;
    }

    // ✅ Renvoie l'ID du compte créé (utile si tu veux afficher / log)
    public int createDefaultAccountForUserReturnAccountId(int userId) {
        String insert = "INSERT INTO accounts (user_id, account_ref, balance) VALUES (?, ?, 0)";
        String updateRef = "UPDATE accounts SET account_ref = ? WHERE id = ?";

        try {
            Connection conn = DBConnection.getConnection();

            // 1) Insert avec une ref temporaire (obligatoire si NOT NULL)
            PreparedStatement stInsert = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            stInsert.setInt(1, userId);
            stInsert.setString(2, "TMP"); // temporaire
            int rows = stInsert.executeUpdate();
            if (rows <= 0) return -1;

            // 2) Récupérer ID auto-généré
            ResultSet keys = stInsert.getGeneratedKeys();
            if (!keys.next()) return -1;

            int accountId = keys.getInt(1);

            // 3) Mettre account_ref = ACC-<accountId>
            String ref = "ACC-" + accountId;
            PreparedStatement stUpdate = conn.prepareStatement(updateRef);
            stUpdate.setString(1, ref);
            stUpdate.setInt(2, accountId);
            stUpdate.executeUpdate();

            return accountId;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ✅ Supprimer tous les comptes d'un user (transactions restent)
    public boolean deleteAccountsByUserId(int userId) {
        String sql = "DELETE FROM accounts WHERE user_id = ?";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ✅ Optionnel : supprimer un compte précis
    public boolean deleteAccountById(int accountId) {
        String sql = "DELETE FROM accounts WHERE id = ?";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, accountId);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
