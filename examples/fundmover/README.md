# Fundmover Demo App

  App demonstrates the usage of TwoPC DM with Maria Database ,
  BankAccount and Wallet class extends the TwoPCExtResourceCohortPolicy DM and 
  Finance class extends the TwoPCCoordinatorPolicy DM. 

#Setup  
Follow the below steps to build and configure the MariaDB

1. docker pull mariadb/server:latest
2. docker run -p 127.0.0.1:3306:3306 --name mariadbamino -e MYSQL_ROOT_PASSWORD=mysecretpw -d mariadb/server:latest  
In place of mysecretpw specify the password to be used 
3. docker start mariadbamino
4. sudo docker exec -it mariadbamino bash 
5. mysql -uroot -p  
When asked for password, enter the password specified in Step 2 
6. Execute all the queries present in sql-scripts/setup.sql file 
7. Exit from the container

# Usage 
1. Check the state of DB container:
    ```
    docker ps
    ```  
    If container is not up, start the container using:
    ```
    docker start mariadbamino
    ```
2. Replace the ```PASS``` field in ```Wallet.java``` with the password specified in step 2 of setup
3. Build App
    ```
    ./gradlew examples:fundmover:build
    ``` 
4. Run OMS
    ```
    ./gradlew examples:fundmover:runoms
    ```
5. Run Kernel Server
    ```
    ./gradlew examples:fundmover:runks
    ```
6. Run app
    ```
    ./gradlew examples:fundmover:runapp
    ```
7.  Alternatively, you can run the app using
    ```
    ./gradlew examples:fundmover:run
    ```
    It starts the oms and two kernelservers in background and runs the app on top of it.  
   
#Todo 

Docker file will updated to handle DB specific operation such as setting password ,
creating the Database and executing ddl and dml queries.
    