package paypulse.service;

import paypulse.exception.DailyLimitExceededException;
import paypulse.exception.InsufficientBalanceException;
import paypulse.exception.InvalidAmountException;
import paypulse.model.Transaction;
import paypulse.model.User;

import java.util.List;


public class TransactionWorker extends Thread {

    private final TransactionQueue queue;
    private final UserService service;
    private final List<Transaction> finished;
    private int processed = 0;
    public TransactionWorker(String name, TransactionQueue queue, UserService service,
                             List<Transaction> finished) {
        super(name);
        this.queue = queue;
        this.service = service;
        this.finished = finished;
    }

    @Override
    public void run() {
        try {
            Transaction t;
            while ((t = queue.take()) != null) {
                handle(t);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handle(Transaction t) {
        User sender = service.getUser(t.getSenderUpi());
        User receiver = service.getUser(t.getReceiverUpi());
        try {
            t.process(sender, receiver);
        } catch (InsufficientBalanceException | DailyLimitExceededException | InvalidAmountException e) {

        }
        finished.add(t);
        processed++;
    }

    public int getProcessed() {
        return processed;
    }
}