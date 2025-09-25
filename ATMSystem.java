import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * ATMSystem.java
 * Single-file core-Java ATM / Banking Simulation with persistence (serialization).
 *
 * Compile:
 *   javac ATMSystem.java
 * Run:
 *   java ATMSystem
 *
 * Requires Java 8+ (uses java.time).
 */

// Account class: represents a bank account
class Account implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long accountNumber;
    private String name;
    private int pin; // 4-digit pin (store as int for simplicity)
    private double balance;
    private final List<String> transactions;

    public Account(long accountNumber, String name, int pin) {
        this.accountNumber = accountNumber;
        this.name = name;
        this.pin = pin;
        this.balance = 0.0;
        this.transactions = new ArrayList<>();
        record("Account created.");
    }

    public long getAccountNumber() { return accountNumber; }
    public String getName() { return name; }
    public double getBalance() { return balance; }

    public boolean checkPin(int inputPin) {
        return this.pin == inputPin;
    }

    public void changePin(int newPin) {
        this.pin = newPin;
        record("PIN changed.");
    }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit must be positive.");
        balance += amount;
        record(String.format("Deposited: ₹%.2f | Balance: ₹%.2f", amount, balance));
    }

    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdraw must be positive.");
        if (amount > balance) throw new IllegalArgumentException("Insufficient balance.");
        balance -= amount;
        record(String.format("Withdrawn: ₹%.2f | Balance: ₹%.2f", amount, balance));
    }

    public void transferOut(double amount, long toAccount) {
        if (amount <= 0) throw new IllegalArgumentException("Transfer must be positive.");
        if (amount > balance) throw new IllegalArgumentException("Insufficient balance.");
        balance -= amount;
        record(String.format("Transferred ₹%.2f to A/C %d | Balance: ₹%.2f", amount, toAccount, balance));
    }

    public void transferIn(double amount, long fromAccount) {
        if (amount <= 0) throw new IllegalArgumentException("Transfer must be positive.");
        balance += amount;
        record(String.format("Received ₹%.2f from A/C %d | Balance: ₹%.2f", amount, fromAccount, balance));
    }

    private void record(String note) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        transactions.add(ts + " - " + note);
    }

    public List<String> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    @Override
    public String toString() {
        return String.format("A/C %d | %s | Balance: ₹%.2f", accountNumber, name, balance);
    }
}

// Bank class: manages accounts and persistence
class Bank implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<Long, Account> accounts = new HashMap<>();
    private long nextAccountNumber = 1001001001L; // starting account number

    public synchronized Account createAccount(String name, int pin) {
        long accNo = nextAccountNumber++;
        Account a = new Account(accNo, name, pin);
        accounts.put(accNo, a);
        return a;
    }

    public Account getAccount(long accNo) {
        return accounts.get(accNo);
    }

    public Collection<Account> getAllAccounts() {
        return accounts.values();
    }

    public boolean accountExists(long accNo) {
        return accounts.containsKey(accNo);
    }

    // Save bank to file
    public void saveToFile(String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(this);
        }
    }

    // Load bank from file
    public static Bank loadFromFile(String filename) throws IOException, ClassNotFoundException {
        File f = new File(filename);
        if (!f.exists()) {
            return new Bank(); // return empty bank
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            Object obj = ois.readObject();
            if (obj instanceof Bank) {
                return (Bank) obj;
            } else {
                throw new ClassNotFoundException("File does not contain a Bank object");
            }
        }
    }
}

// ATM UI class with main()
public class ATMSystem {
    private static final String DATA_FILE = "bank.dat";
    private final Bank bank;
    private final Scanner sc;

    private ATMSystem(Bank bank) {
        this.bank = bank;
        this.sc = new Scanner(System.in);
    }

    public static void main(String[] args) {
        Bank bank;
        try {
            bank = Bank.loadFromFile(DATA_FILE);
            System.out.println("Loaded bank data.");
        } catch (Exception e) {
            bank = new Bank();
            System.out.println("Starting new bank (no saved data found).");
        }

        ATMSystem app = new ATMSystem(bank);
        app.run();
        // on exit, save bank
        try {
            bank.saveToFile(DATA_FILE);
            System.out.println("Bank data saved to " + DATA_FILE);
        } catch (IOException e) {
            System.err.println("Failed to save bank data: " + e.getMessage());
        }
    }

    private void run() {
        boolean running = true;
        while (running) {
            showMainMenu();
            int choice = readInt("Choice: ");
            switch (choice) {
                case 1:
                    createAccountFlow();
                    break;
                case 2:
                    loginFlow();
                    break;
                case 3:
                    listAccountsBrief();
                    break;
                case 0:
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
        System.out.println("Goodbye!");
    }

    private void showMainMenu() {
        System.out.println("\n==== Welcome to SimpleATM ====");
        System.out.println("1) Create new account");
        System.out.println("2) Login to account");
        System.out.println("3) List accounts (brief)"); // for demo/testing - remove in production
        System.out.println("0) Exit");
    }

    private void createAccountFlow() {
        System.out.println("\n--- Create Account ---");
        String name = readLine("Full name: ").trim();
        int pin;
        while (true) {
            pin = readInt("Choose a 4-digit PIN: ");
            if (pin >= 1000 && pin <= 9999) break;
            System.out.println("PIN must be 4 digits.");
        }
        Account a = bank.createAccount(name, pin);
        System.out.println("Account created successfully!");
        System.out.println("Your account number: " + a.getAccountNumber());
        saveBank();
    }

    private void loginFlow() {
        System.out.println("\n--- Login ---");
        long accNo = readLong("Account number: ");
        if (!bank.accountExists(accNo)) {
            System.out.println("Account not found.");
            return;
        }
        Account acc = bank.getAccount(accNo);
        int attempts = 0;
        while (attempts < 3) {
            int pin = readInt("Enter PIN: ");
            if (acc.checkPin(pin)) {
                System.out.println("Login successful. Welcome, " + acc.getName() + "!");
                accountMenu(acc);
                return;
            } else {
                attempts++;
                System.out.println("Incorrect PIN. Attempts left: " + (3 - attempts));
            }
        }
        System.out.println("Too many incorrect attempts. Returning to main menu.");
    }

    private void accountMenu(Account acc) {
        boolean inAccount = true;
        while (inAccount) {
            System.out.println("\n--- Account Menu for A/C " + acc.getAccountNumber() + " ---");
            System.out.println("1) Check balance");
            System.out.println("2) Deposit");
            System.out.println("3) Withdraw");
            System.out.println("4) Transfer");
            System.out.println("5) Mini-statement");
            System.out.println("6) Change PIN");
            System.out.println("0) Logout");
            int c = readInt("Choice: ");
            try {
                switch (c) {
                    case 1:
                        System.out.printf("Current balance: ₹%.2f%n", acc.getBalance());
                        break;
                    case 2:
                        double dep = readDouble("Amount to deposit: ₹");
                        acc.deposit(dep);
                        System.out.printf("Deposited ₹%.2f. New balance: ₹%.2f%n", dep, acc.getBalance());
                        saveBank();
                        break;
                    case 3:
                        double w = readDouble("Amount to withdraw: ₹");
                        acc.withdraw(w);
                        System.out.printf("Withdrawn ₹%.2f. New balance: ₹%.2f%n", w, acc.getBalance());
                        saveBank();
                        break;
                    case 4:
                        long toAcc = readLong("Transfer to account number: ");
                        if (!bank.accountExists(toAcc)) {
                            System.out.println("Destination account not found.");
                            break;
                        }
                        double amt = readDouble("Amount to transfer: ₹");
                        Account dest = bank.getAccount(toAcc);
                        acc.transferOut(amt, toAcc);
                        dest.transferIn(amt, acc.getAccountNumber());
                        System.out.printf("Transferred ₹%.2f to %d. Your balance: ₹%.2f%n", amt, toAcc, acc.getBalance());
                        saveBank();
                        break;
                    case 5:
                        showMiniStatement(acc);
                        break;
                    case 6:
                        int newPin;
                        while (true) {
                            newPin = readInt("Enter new 4-digit PIN: ");
                            if (newPin >= 1000 && newPin <= 9999) break;
                            System.out.println("PIN must be 4 digits.");
                        }
                        acc.changePin(newPin);
                        System.out.println("PIN changed successfully.");
                        saveBank();
                        break;
                    case 0:
                        inAccount = false;
                        System.out.println("Logged out.");
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (IllegalArgumentException ex) {
                System.out.println("Operation failed: " + ex.getMessage());
            }
        }
    }

    private void showMiniStatement(Account acc) {
        System.out.println("\n--- Mini Statement ---");
        List<String> tx = acc.getTransactions();
        if (tx.isEmpty()) {
            System.out.println("No transactions.");
            return;
        }
        int lines = Math.min(tx.size(), 10); // show last 10
        for (int i = tx.size() - lines; i < tx.size(); i++) {
            System.out.println(tx.get(i));
        }
    }

    private void listAccountsBrief() {
        System.out.println("\n--- All Accounts (brief) ---");
        Collection<Account> all = bank.getAllAccounts();
        if (all.isEmpty()) {
            System.out.println("No accounts created yet.");
            return;
        }
        for (Account a : all) {
            System.out.println(a);
        }
    }

    private void saveBank() {
        try {
            bank.saveToFile(DATA_FILE);
        } catch (IOException e) {
            System.err.println("Warning: Failed to save bank data: " + e.getMessage());
        }
    }

    // ---------- Simple input helpers ----------
    private String readLine(String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private long readLong(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid amount (e.g., 2500.50).");
            }
        }
    }
}
