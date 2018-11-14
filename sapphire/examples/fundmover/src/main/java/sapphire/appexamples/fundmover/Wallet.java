package sapphire.appexamples.fundmover;

import sapphire.app.SapphireObject;
import sapphire.policy.serializability.TransactionAlreadyStartedException;
import sapphire.policy.transaction.TransactionExecutionException;
import sapphire.policy.transaction.TransactionManager;

import java.util.UUID;

import java.sql.*;

public class Wallet implements SapphireObject, TransactionManager {
    private int balance;
    private Boolean isStart = false;
    private Boolean isCommit = false;

    private static final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
    private static final String DB_URL="jdbc:mariadb://172.17.0.2/fundmover";
    private static final String USER = "root";
    private static final String PASS = "mysecretpw";

    transient private Connection conn;

    public Wallet() {
        try {
            this.conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ensureConnection() {
        if (this.conn == null) {
            try {
                this.conn = DriverManager.getConnection(DB_URL, USER, PASS);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void credit(int amount) {
        this.balance += amount;
        this.isStart = true;
        System.out.print("new balance "+this.balance);
    }

    public void debit(int amount) throws Exception {
        if (this.balance < amount) {
            this.isStart = true;
            throw new Exception("insufficient fund");

        }

        this.balance -= amount;
        this.isStart = true;
         }

    public int getBalance() {
        return this.balance;
    }

    @Override
    public void join(UUID transactionId) throws TransactionAlreadyStartedException {
        System.out.println("[wallet] xa join");

        try {
            this.ensureConnection();
            Statement statement = this.conn.createStatement();
            /* Additional character added  to make transaction id as unique @ Database */
            String sql = "xa start '" + transactionId.toString() + "2'";
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void leave(UUID transactionId) { }

    @Override
    public Vote vote(UUID transactionId) throws TransactionExecutionException {
        System.out.println("[wallet] xa vote");
         Vote vote = Vote.YES;

        try {
            Statement statement = this.conn.createStatement();

             try {
                 if (this.isStart) {
                     String updateSql = String.format("update Wallets set balance=%d where name='test'", this.balance);
                     statement.executeUpdate(updateSql);
                     this.isStart = false;
                 }
             } catch (Exception e){
                 System.out.println(e.getMessage());
                 vote=Vote.NO;
             }
            /* Additional character added  to make transaction id as unique @ Databases */
            String sql = "xa end '" + transactionId.toString() +"2'";
            statement.execute(sql);
            String sql2 = "xa prepare '" + transactionId.toString() + "2'";
            this.isCommit=true;
            statement.execute(sql2);
        } catch (SQLException e) {
            e.printStackTrace();
            vote=Vote.NO;
        }


        return vote;
    }

    @Override
    public void commit(UUID transactionId) {
        System.out.println("[wallet] xa commit");

        try {
            Statement statement = this.conn.createStatement();
            /* Additional character added  to make transaction id as unique @ Databases */
            String sql = "xa commit '" + transactionId.toString() + "2'";
            statement.execute(sql);
            this.isCommit=false;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void abort(UUID transactionId) {
        System.out.println("[wallet] xa abort");
        try {
            Statement statement = this.conn.createStatement();
            /* Additional character added  to make transaction id as unique @ Databases */
            if (this.isStart){
                String sql = "xa end '" + transactionId.toString() +"2'";
                statement.execute(sql);
                this.isStart=false;
            }
            if (this.isCommit){
            String sql = "xa rollback '" + transactionId.toString()+"2'";
            statement.execute(sql);
            this.isCommit=false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}