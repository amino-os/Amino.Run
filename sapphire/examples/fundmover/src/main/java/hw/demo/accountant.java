package hw.demo;

public class accountant {
    public static void main(String[] args) {
        System.out.println("Hello transaction");

        wallet wallet = new wallet();
        bankaccount bankaccount = new bankaccount();
        finance finance = new finance(wallet, bankaccount);
        finance.transferFromWallet(5);

        System.out.printf("wallet : %d, bank : %d \r\n", wallet.getBalance(), bankaccount.getBalance());
    }
}
