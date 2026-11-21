package paypulse.model;

import paypulse.exception.InvalidPinException;

public interface Authenticatable {
    boolean login(String pin) throws InvalidPinException;
    void lock();
}
