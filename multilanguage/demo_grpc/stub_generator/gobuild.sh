
#build
mkdir -p bin
g++ -Isrc src/generator_base.cc src/gosrv/generator.cc src/gosrv/plugin.cc -std=c++11 `pkg-config --cflags --libs protobuf` -lprotoc -o bin/protoc-gen-go_sapphire_server && \

#install
chmod +x bin/protoc-gen-go_sapphire_server && \
sudo cp bin/protoc-gen-go_sapphire_server /usr/local/bin/ && \



#run code generator
mkdir -p gooutputserver
protoc --go_out=gooutputserver proto/test.proto && \
protoc --go_sapphire_server_out=gooutputserver proto/test.proto && \

#place in gopath so we can run
mkdir -p $GOPATH/src/CodeGenTestServer/test && \
mkdir -p $GOPATH/src/CodeGenTestServer/main && \
cp gooutputserver/test.pb.go $GOPATH/src/CodeGenTestServer/test && \
cp gooutputserver/SapphireServerStubtest.pb.go $GOPATH/src/CodeGenTestServer/test && \
cp gooutputserver/test.go $GOPATH/src/CodeGenTestServer/main && \
cp gooutputserver/sapphireobject.go $GOPATH/src/CodeGenTestServer/test && \

#build and run go application
go install CodeGenTestServer/main && \
$GOPATH/bin/main

echo "done"


