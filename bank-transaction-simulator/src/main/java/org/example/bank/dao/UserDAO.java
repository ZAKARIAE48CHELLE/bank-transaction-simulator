package org.example.bank.dao;

import org.example.bank.auth.Role;
import org.example.bank.auth.User;
import org.example.bank.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
}
