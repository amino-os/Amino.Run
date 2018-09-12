Let's do a POC to demonstrate how a java application uses a javascript SO.

Key points we would like to demo are:
1. JS object creation in Java.
   In order to call `addStudent` method on `College` class, our Java application 
   first needs to construct a `Student` instance in Java. This POC will demo
   how to create javascript `Student` instance in Java. 
2. Get reference to College\_Stub.
   This java application talks to remote javascript College SO through College\_Stub.
   In this POC, we will handscraft a College\_Stub java class and load it with 
   GraalVM API. Eventually, this stub class will be generated automatically and Java
   applications get the stub from OMS.
3. Serialization, Deserialization and Method Invocation.
   Our handcrafted College\_Stub.java should mimic the behaviors of stub class and 
   kernel server. It will serialize parameters into bytes, deserialize bytes into 
   polyglot Value, and and invoke methods on javascript College class via polyglot Value API. 
