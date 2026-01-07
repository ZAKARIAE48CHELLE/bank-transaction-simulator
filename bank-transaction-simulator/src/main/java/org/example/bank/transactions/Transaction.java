package org.example.bank.transactions;

import java.util.List;

public interface Transaction {
    void execute();
    List<String> lockKeys();

}
