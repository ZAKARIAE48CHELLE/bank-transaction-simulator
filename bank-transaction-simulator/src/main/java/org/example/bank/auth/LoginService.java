package org.example.bank.auth;

import org.example.bank.dao.UserDAO;

public class LoginService {

    private final UserDAO userDAO = new UserDAO();

    public User login(String username, String password) {
        return userDAO.findByCredentials(username, password);
    }

}
