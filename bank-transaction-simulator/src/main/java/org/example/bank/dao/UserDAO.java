package org.example.bank.dao;

import org.example.bank.DBConnection;
import org.example.bank.auth.Role;
import org.example.bank.auth.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findByCredentials(String username, String password) {
        String sql = """
                SELECT id, username, role
                FROM users
                WHERE username = ? AND password = ?
                """;
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        Role.valueOf(rs.getString("role"))
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public long countUsers() {
        String sql = "SELECT COUNT(*) AS total FROM users";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getLong("total");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, username, role FROM users ORDER BY id DESC";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                list.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        Role.valueOf(rs.getString("role"))
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // ========= CREATE (return new user id) =========
    public int createClientReturnId(String username, String password) {
        return createUserReturnId(username, password, Role.CLIENT);
    }

    public int createAdminReturnId(String username, String password) {
        return createUserReturnId(username, password, Role.ADMIN);
    }

    private int createUserReturnId(String username, String password, Role role) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try {
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role.name());

            int rows = stmt.executeUpdate();
            if (rows <= 0) return -1;

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Delete user + accounts + transactions (safe even without FK cascade)
    public boolean deleteUserCascade(int userId) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1) delete transactions linked to user accounts
            // (Assumption: transactions table has account_from / account_to as account_ref, adjust if needed)
            String delTx = """
                    DELETE t FROM transactions t
                    JOIN accounts a ON (t.account_from = a.account_ref OR t.account_to = a.account_ref)
                    WHERE a.user_id = ?
                    """;
            try (PreparedStatement st = conn.prepareStatement(delTx)) {
                st.setInt(1, userId);
                st.executeUpdate();
            } catch (Exception ignored) {
                // if your schema doesn't match, we ignore and continue
            }

            // 2) delete accounts
            String delAcc = "DELETE FROM accounts WHERE user_id = ?";
            try (PreparedStatement st = conn.prepareStatement(delAcc)) {
                st.setInt(1, userId);
                st.executeUpdate();
            }

            // 3) delete user
            String delUser = "DELETE FROM users WHERE id = ?";
            int rows;
            try (PreparedStatement st = conn.prepareStatement(delUser)) {
                st.setInt(1, userId);
                rows = st.executeUpdate();
            }

            conn.commit();
            conn.setAutoCommit(true);
            return rows > 0;

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (conn != null) conn.rollback();
            } catch (Exception ignored) {}
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (Exception ignored) {}
        }
    }
}
