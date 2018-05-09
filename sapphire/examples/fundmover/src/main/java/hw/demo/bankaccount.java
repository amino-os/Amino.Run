package hw.demo;

import sapphire.app.SapphireObject;
import sapphire.policy.serializability.TransactionAlreadyStartedException;
import sapphire.policy.transaction.TransactionExecutionException;
import sapphire.policy.transaction.TransactionManager;
import sapphire.policy.transaction.TwoPCCohortPolicy;
import sapphire.policy.transaction.TwoPCExtResourceCohortPolicy;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class bankaccount implements SapphireObject<TwoPCExtResourceCohortPolicy>, TransactionManager {
    private int balance;
    private Boolean isDirty = false;

    private static final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
    private static final String DB_URL="jdbc:mariadb://172.17.0.4/bank";
    private static final String USER = "root";
    private static final String PASS = "my_secret_pw";

    transient private Connection conn;

    private void ensureConnection() {
        if (this.conn == null) {
            try {
                this.conn = DriverManager.getConnection(DB_URL, USER, PASS);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public bankaccount() {
        this.ensureConnection();
    }

    public void credit(int amount) {
        this.balance += amount;
        this.isDirty = true;
    }

    public void debit(int amount) {
        this.balance -= amount;
        this.isDirty = true;
    }

    public int getBalance() {
        return this.balance;
    }

    @Override
    public void join(UUID transactionId) throws TransactionAlreadyStartedException {
        System.out.println("xa join");

        try {
            this.ensureConnection();
            Statement statement = this.conn.createStatement();
            String sql = "xa start '" + transactionId.toString() + "'";
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void leave(UUID transactionId) {
        System.out.println("xa leave");
    }

    @Override
    public Vote vote(UUID transactionId) throws TransactionExecutionException {
        System.out.println("xa vote");

        try {
            Statement statement = this.conn.createStatement();

            if (this.isDirty) {
                String updateSql = String.format("update accounts set balance=%d where name='seattle'", this.balance);
                statement.executeUpdate(updateSql);
                this.isDirty = false;
            }

            String sql = "xa end '" + transactionId.toString() +"'";
            statement.execute(sql);
            String sql2 = "xa prepare '" + transactionId.toString() + "'";
            statement.execute(sql2);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Vote.YES;
    }

    @Override
    public void commit(UUID transactionId) {
        System.out.println("xa commit");

        try {
            Statement statement = this.conn.createStatement();
            String sql = "xa commit '" + transactionId.toString() + "'";
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void abort(UUID transactionId) {
        System.out.println("xa abort");

        try {
            Statement statement = this.conn.createStatement();
            String sql = "xa rollback " + transactionId.toString();
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
