# Fundmover Demo App

  App demonstrates the usage of TwoPC DM with Maria Database ,
  BankAccount and Wallet class  extends the TwoPCExtResourceCohortPolicy DM and 
  Finance class extends the TwoPCCoordinatorPolicy DM. 

#Setup 
  Follow the Below step to build and configure the Maria DB

1. docker pull mariadb/server:latest
2. docker run --name mariadbdcap -e MYSQL_ROOT_PASSWORD=mysecretpw -d mariadb/server:latest
   In Place of mysecretpw specify the password to be used 
3. docker start mariadbdcap
4. sudo docker exec -it mariadbdcap bash 
5. mysql -uroot -p
   use the password defined  in Step 2 
6. Execute all  the query  present in sql-scripts/setup.sql file 
7. Exit from the CLi and container 

# Usage 
1. Check the state of DB container 
   docker ps
   If container is not up start the container 
   docker start mariadbdcap
2. Use the password defined in Step 2 in Wallet.java and BackAccount.java files
3. Build App
   ../../gradlew build 
4. Run OMS
   ../../gradlew runoms
5. Run Kernel Server
   ../../gradlew runks
6. Run app
   ../../gradlew runapp
   
#Todo 

Docker file will updated to handle DB specific operation such as setting password ,
creating the Database and executing ddl and dml queries.
    