package amino.run.appexamples.fundmover;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import amino.run.app.SapphireObject;
import amino.run.policy.serializability.TransactionAlreadyStartedException;
import amino.run.policy.transaction.TransactionExecutionException;
import amino.run.policy.transaction.TransactionManager;

public class Wallet implements SapphireObject, TransactionManager {
    private int balance;
    private Boolean isStart = false;

    private static final String JDBC_DRIVER = "org.mariadb.jdbc.Driver";
    private static final String DB_URL="jdbc:mariadb://172.17.0.2/fundmover";
    private static final String USER = "root";
    private static final String PASS = "mysecretpw";

    transient private Connection conn;

    public Wallet() {}

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
        }

    public void debit(int amount) throws Exception {
        if (this.balance < amount) {
            throw new Exception("insufficient fund");
        }
        this.balance -= amount;
    }

    public int getBalance() {
        return this.balance;
    }

    @Override
    public void join(UUID transactionId) throws TransactionAlreadyStartedException {
        System.out.println("[wallet] xa join");
        Statement statement=null;
        try {
            this.ensureConnection();
            statement = this.conn.createStatement();
            String sql = "xa start '" + transactionId.toString()+"'";
            statement.execute(sql);
            this.isStart=true;
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                if (statement!=null){
                    statement.close();
                }
            } catch (SQLException e) {}
        }

    }

    @Override
    public void leave(UUID transactionId) {}

    @Override
    public Vote vote(UUID transactionId) throws TransactionExecutionException {
        System.out.println("[wallet] xa vote");
        Vote vote = Vote.YES;
         Statement statement=null;
        try {
             this.ensureConnection();
             statement = this.conn.createStatement();
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
            String sql = "xa end '" + transactionId.toString()+"'";
            statement.execute(sql);
            String sql2 = "xa prepare '" + transactionId.toString()+"'" ;
            statement.execute(sql2);
        } catch (SQLException e) {
            e.printStackTrace();
            vote=Vote.NO;
        }finally {
            try {
                if (statement !=null){
                    statement.close();
                }
            }catch (SQLException e) {}
        }
        return vote;
    }

    @Override
    public void commit(UUID transactionId) {
        System.out.println("[wallet] xa commit");
        Statement statement=null;
        try {
            this.ensureConnection();
            statement = this.conn.createStatement();
            String sql = "xa commit '" + transactionId.toString() +"'";
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement !=null){
                    statement.close();
                }
            }catch (SQLException e)  {}
            try {
                this.conn.close();
            } catch (SQLException e) {}
        }
    }

    @Override
    public void abort(UUID transactionId) {
        System.out.println("[wallet] xa abort");
        Statement statement=null;
        try {
            this.ensureConnection();
            statement = this.conn.createStatement();
            if (this.isStart){
                String sql = "xa end '" + transactionId.toString()+"'" ;
                statement.execute(sql);
                this.isStart=false;
            }
            String sql = "xa rollback '" + transactionId.toString()+"'";
            statement.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            try {
                if (statement !=null){
                    statement.close();
                }
            }catch (SQLException e) {}
            try {
                this.conn.close();
            } catch (SQLException e) {}
        }
    }
}
