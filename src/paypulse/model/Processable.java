package paypulse.model;

import paypulse.exception.InsufficientBalanceException;

public interface Processable {
    void validate();
    void process(User sender, User receiver) throws InsufficientBalanceException;
}