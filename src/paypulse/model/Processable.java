package paypulse.model;

import paypulse.exception.InsufficientBalanceException;
import paypulse.exception.DailyLimitExceededException;

public interface Processable {
    void validate();
    void process(User sender, User receiver) throws InsufficientBalanceException, DailyLimitExceededException;
}