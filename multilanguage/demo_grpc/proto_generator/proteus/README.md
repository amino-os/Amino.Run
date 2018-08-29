# proteus
# Usage 
1. Clone the code and place it in the $GOPATH/src folder 
2. Switch to the proteus folder 
   cd $GOPATH/src/proteus
3. Build the binary as mentioned below 
    go build 
4. To generate Proto for user code execute  the mentioned command from $GOPATH/src 
    root1@lenovo:~/Work/src$ ./proteus/proteus -p hankstodo/app/user_code/ -f hankstodo/
    -p : Path of Application Code  (package)
     -f: Path where proto file needs to be stored   

# Limitation 
1. If any interface or error is used as request or response params in method those fileds 
   will be ignored in proto
2. Proto defination of entire   package (Application Code ) will be generated in single      file 