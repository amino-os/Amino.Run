package main

import (
	"crypto/rand"
	"errors"
	"reflect"
	"fmt"
	"io"
	"os"
	"net"
	"plugin"
	api "sapphire_grpc/api"
	"sapphire_grpc/sapphire_process/tlsutil"
	"golang.org/x/net/context"
	"google.golang.org/grpc/health"
	healthpb "google.golang.org/grpc/health/grpc_health_v1"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
	"google.golang.org/grpc/keepalive"
	"flag"
	"log"
	"path/filepath"
	"time"
	"crypto/tls"
)


const (
	// tlsEnableEnvVar names the environment variable that enables TLS.
	tlsEnableEnvVar = "TILLER_TLS_ENABLE"
	// tlsVerifyEnvVar names the environment variable that enables
	// TLS, as well as certificate verification of the remote.
	tlsVerifyEnvVar = "TILLER_TLS_VERIFY"
	// tlsCertsEnvVar names the environment variable that points to
	// the directory where Tiller's TLS certificates are located.
	tlsCertsEnvVar = "TILLER_TLS_CERTS"

)

var (
	grpcAddr             = flag.String("listen", ":7000", "address:port to listen on")
	tlsEnable            = flag.Bool("tls", tlsEnableEnvVarDefault(), "enable TLS")
	tlsVerify            = flag.Bool("tls-verify", tlsVerifyEnvVarDefault(), "enable TLS and verify remote certificate")
	keyFile              = flag.String("tls-key", tlsDefaultsFromEnv("tls-key"), "path to TLS private key file")
	certFile             = flag.String("tls-cert", tlsDefaultsFromEnv("tls-cert"), "path to TLS certificate file")
	caCertFile           = flag.String("tls-ca-cert", tlsDefaultsFromEnv("tls-ca-cert"), "trust certificates signed by this CA")
	SharedLibsPath       = flag.String("SharedLibsPath", "../sapphire_objects/", "path of the shared libs")

	// rootServer is the root gRPC server.
	// Each gRPC service registers itself to this server during start().
	rootServer *grpc.Server

	logger *log.Logger
)
//==============================Start UUID related Code ====================================================

type UUID []byte

var rander = rand.Reader

func (uuid UUID) String() string {
	if uuid == nil || len(uuid) != 16 {
		return ""
	}
	b := []byte(uuid)

	return fmt.Sprintf("%08x-%04x-%04x-%04x-%012x", b[:4], b[4:6], b[6:8], b[8:10], b[10:])

}

func randomBits(b []byte) {
	if _, err := io.ReadFull(rander, b); err != nil {
		fmt.Println("Error in randomBits func", err)
		panic(err.Error())
	}
}

func NewRandom() UUID {

	uuid := make([]byte, 16)
	randomBits(uuid)

	uuid[6] = (uuid[6] & 0x0f) | 0x40 //version 4
	uuid[8] = (uuid[8] & 0x3f) | 0x80 //version 10
	return uuid
}

func GetRandomString() string {

	return NewRandom().String()

}

// maxMsgSize use 20MB as the default message size limit.
// grpc library default is 4MB
const maxMsgSize = 1024 * 1024 * 20

// DefaultServerOpts returns the set of default grpc ServerOption's that Tiller requires.
func DefaultServerOpts() []grpc.ServerOption {
	return []grpc.ServerOption{
		grpc.MaxRecvMsgSize(maxMsgSize),
		grpc.MaxSendMsgSize(maxMsgSize),
		//grpc.UnaryInterceptor(newUnaryInterceptor()),
		//grpc.StreamInterceptor(newStreamInterceptor()),
	}
}

//==============================End UUID related Code ====================================================

type PlugInInfo struct {
	ObjectAddress *plugin.Plugin
}

//PlugInName & PlugIn Map
var SapphireNameMap map[string]PlugInInfo

//UUID & SapphireObjectMap
var SapphireIDMap map[string]interface{}

type Server struct{}

// Create the Sapphire Object and Load the Dynamic lib First time
func (s *Server) CreateSapphireObject(c context.Context, in *api.CreateRequest) (*api.CreateReply, error) {

	var plug *plugin.Plugin
	var err error
	sapphireObjName := in.GetName()

	fmt.Println("sapphireObjName:", sapphireObjName)

	plugInfo, ok := SapphireNameMap[sapphireObjName]

	//if the plug in is not loaded then load it other wise just create the UUID &Create the SapphireObject
	if !ok {

		sharedlibPath := fmt.Sprintf("%s%s.so",*SharedLibsPath,sapphireObjName)

		_,err = os.Stat(sharedlibPath)

		if err != nil {
			fmt.Println("Shared library is not there is the path", sharedlibPath)
			return nil, err
		}
		plug, err = plugin.Open(sharedlibPath)
		if err != nil {
			fmt.Println("plugin.Open Failed",err)
			return nil, err
		}

		SapphireNameMap[sapphireObjName] = PlugInInfo{ObjectAddress: plug}
		fmt.Println("Successfully Loaded  the Dynamic lib", SapphireNameMap)
	} else {
		plug = plugInfo.ObjectAddress
	}

	uuid := GetRandomString()

	symGreeter, err := plug.Lookup("GenericCreate")
	if err != nil {
		fmt.Println("Error in plug.Lookup for GenericCreate")
		fmt.Println(err)
		return nil, err
	}
	method, ok := symGreeter.(func() interface{})

	if !ok {
		err = errors.New("GenericCreate function is not there in dynamic lib")
		fmt.Println("Error in symGreeter.(func(string) interface{} )")
		return nil, err
	}

	output := method()

	//store the Sapphire Object in the map
	SapphireIDMap[uuid] = output
	fmt.Println("Successfully created the Sapphire Object", SapphireIDMap)
	return &api.CreateReply{ObjId: uuid}, nil
}

// Delete the Sapphire Object
func (s *Server) DeleteSapphireObject(c context.Context, in *api.DeleteRequest) (*api.DeleteReply, error) {

	_, flag := SapphireIDMap[in.ObjId]

	if ! flag  {
		err := errors.New("SapphireObject ID is Invalid")
		fmt.Println("SapphireObject ID is Invalid")
		return &api.DeleteReply{Flag: false},err
	}

	delete(SapphireIDMap, in.ObjId)
	fmt.Println("Successfully&api.DeleteReply{Flag: true} DeleteSapphireObject the Sapphire Object", SapphireIDMap)
	return &api.DeleteReply{Flag: true}, nil
}
func (s *Server) GenericMethodInvoke(c context.Context, in *api.GenericMethodRequest) (*api.GenericMethodReply, error) {

	_, ok := SapphireNameMap[in.SapphireObjName]

	if ! ok  {
		err := errors.New("SapphireObject Name is wrong")
		fmt.Println("SapphireObject Name is wrong")
		return nil,err
	}

	Obj, flag := SapphireIDMap[in.ObjId]

	if ! flag  {
		err := errors.New("SapphireObject ID is Invalid")
		fmt.Println("SapphireObject ID is Invalid")
		return nil,err
	}

	actualMethod := reflect.ValueOf(Obj).MethodByName(in.FuncName)

	if ! actualMethod.IsValid() {
		err := errors.New("Method Name is Inavlid")
		fmt.Println("Method Name is Inavlid :", actualMethod)
		return nil,err
	}
	wrapperMethod := reflect.ValueOf(Obj).MethodByName(in.FuncName + "_Wrap")
	
	if ! wrapperMethod.IsValid() {
		err := errors.New("wrappermethod is wrong some issue in stub generation")
		fmt.Println("Method Name is Inavlid :", wrapperMethod ,err)
		return nil, err
	}
	
	objects := make(map[reflect.Type]interface{})
	reqin := make([]reflect.Value, wrapperMethod.Type().NumIn())

	paramtype := wrapperMethod.Type().In(0).Elem()
	objects[paramtype] = in.Params
	reqin[0] = reflect.ValueOf(objects[paramtype])
	

	methodReslut := wrapperMethod.Call(reqin)

	ret := methodReslut[0].Interface().([]byte)

	return &api.GenericMethodReply{Ret: ret}, nil
}

func tlsOptions() tlsutil.Options {
	opts := tlsutil.Options{CertFile: *certFile, KeyFile: *keyFile}
	if *tlsVerify {
		opts.CaCertFile = *caCertFile
		opts.ClientAuth = tls.RequireAndVerifyClientCert
	}
	return opts
}

func newLogger(prefix string) *log.Logger {
	if len(prefix) > 0 {
		prefix = fmt.Sprintf("[%s] ", prefix)
	}
	return log.New(os.Stderr, prefix, log.Flags())
}

// NewServer creates a new grpc server.
func NewgrpcServer(opts ...grpc.ServerOption) *grpc.Server {
	return grpc.NewServer(append(DefaultServerOpts(), opts...)...)
}

func main() {

	srvErrCh := make(chan error)

	flag.Parse()

	SapphireNameMap = make(map[string]PlugInInfo)
	SapphireIDMap = make(map[string]interface{})

	logger = newLogger("main")


	healthSrv := health.NewServer()
	healthSrv.SetServingStatus("go-runtime", healthpb.HealthCheckResponse_NOT_SERVING)


	if *tlsEnable || *tlsVerify {
		opts := tlsutil.Options{CertFile: *certFile, KeyFile: *keyFile}
		if *tlsVerify {
			opts.CaCertFile = *caCertFile
		}
	}

	var opts []grpc.ServerOption

	if *tlsEnable || *tlsVerify {
		cfg, err := tlsutil.ServerConfig(tlsOptions())
		if err != nil {
			logger.Fatalf("Could not create server TLS configuration: %v", err)
		}
		opts = append(opts, grpc.Creds(credentials.NewTLS(cfg)))
	}

	opts = append(opts, grpc.KeepaliveParams(keepalive.ServerParameters{
		MaxConnectionIdle: 10 * time.Minute,
		// If needed, we can configure the max connection age
	}))
	opts = append(opts, grpc.KeepaliveEnforcementPolicy(keepalive.EnforcementPolicy{
		MinTime: time.Duration(20) * time.Second, // For compatibility with the client keepalive.ClientParameters
	}))

	rootServer = NewgrpcServer(opts...)
	healthpb.RegisterHealthServer(rootServer, healthSrv)

	lis, err := net.Listen("tcp", *grpcAddr)
	if err != nil {
		fmt.Println("Unable to listen ")
		logger.Fatalf("Server died: %s", err)
		return
	}


	api.RegisterMgmtgrpcServiceServer(rootServer, &Server{})
	rootServer.Serve(lis)



	defer lis.Close()

	healthSrv.SetServingStatus("Tiller", healthpb.HealthCheckResponse_SERVING)

	select {
	case err := <-srvErrCh:
		logger.Fatalf("Server died: %s", err)
	}

}

func tlsDefaultsFromEnv(name string) (value string) {
	switch certsDir := os.Getenv(tlsCertsEnvVar); name {
		case "tls-key":
			return filepath.Join(certsDir, "tls.key")
		case "tls-cert":
			return filepath.Join(certsDir, "tls.crt")
		case "tls-ca-cert":
			return filepath.Join(certsDir, "ca.crt")
	}
	return ""
}

func tlsEnableEnvVarDefault() bool { return os.Getenv(tlsEnableEnvVar) != "" }
func tlsVerifyEnvVarDefault() bool { return os.Getenv(tlsVerifyEnvVar) != "" }
