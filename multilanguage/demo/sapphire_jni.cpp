#include <iostream>
#include <string>
#include <stdlib.h>

#include <jni.h>
#include <liblltap.h>

#include <thread>

using namespace std;

typedef const char* uuid_t;
  
// CPP wrapper class for Java Class sapphire
class sapphire
{
  private:
    uuid_t uid;            // UUID of Sapphire object
    
    JavaVM *jvm;           // Pointer to the JVM (Java Virtual Machine)
    JNIEnv *env;           // Pointer to native interface

    jclass    cls;         // Pointer to Java class
    jmethodID ctor;        // Java class constructor
    jobject   myobj;       // Java class object
    
    void loadJVM();
    void deleteJVM();
    void createSapphireInstance();
    void deleteSapphireInstance();
    
  public:
    // Constructor
    sapphire() {
      loadJVM();
      createSapphireInstance();
    }
    
    // Destructor
    ~sapphire() {
      deleteSapphireInstance();
      deleteJVM();
    }
    
    //void helloWorld();
    void helloWorld(char*);
};


void sapphire::loadJVM() {
  //================== prepare loading of Java VM ============================
  JavaVMInitArgs vm_args;                             // Initialization arguments
  vm_args.version = JNI_VERSION_1_6;                  // minimum Java version
  vm_args.options = NULL;
  vm_args.nOptions = 0;                               // number of options
  vm_args.ignoreUnrecognized = JNI_FALSE;             // invalid options make the JVM init fail
  
  //=============== load and initialize Java VM and JNI interface =============
  jint rc = JNI_CreateJavaVM(&jvm, (void**)&env, (void*)&vm_args);
  if (rc != JNI_OK) 
  {
    cin.get();
    exit(EXIT_FAILURE);
  }

  //=============== Display JVM version =======================================
  jint ver = env->GetVersion();
  cout << "JVM load succeeded: Version ";
  cout << ((ver>>16)&0x0f) << "."<<(ver&0x0f) << endl;
  
  cls = env->FindClass("sapphireJni");
  if(cls == 0)
    cerr << "ERROR: Class sapphireJni not found. !!!" << endl;
    
  
  // search for constructor  
  ctor = env->GetMethodID(cls, "<init>", "()V");  // FIND AN OBJECT CONSTRUCTOR
  if(!ctor) {
    cerr << "ERROR: constructor not found !" << endl;
  }
  else {
    myobj = env->NewObject(cls, ctor);
    if (myobj)
      cout << "Object succesfully constructed." << endl;
    else
      cerr << "ERROR: Object creation failed. !!!" << endl;
  }
}

void sapphire::deleteJVM() {
  // Cleanup the created JVM.
  jvm->DestroyJavaVM();
}

void sapphire::createSapphireInstance() {
  
  jmethodID mid = env->GetMethodID(cls,"createSapphireInstance","()Ljava/lang/String;");
  if (mid != NULL)
  {
    jstring result = (jstring)env->CallObjectMethod(myobj, mid);
	uid = (char*)env->GetStringUTFChars(result, 0);
  }
  else
    cerr << "ERROR: Method createSapphireInstance not found. !!!" << endl;
}

void sapphire::deleteSapphireInstance() {
    
    jmethodID mid = env->GetMethodID(cls, "deleteSapphireInstance", "(Ljava/lang/String;)V");
    if(mid != NULL)
    {
      env->CallObjectMethod(myobj, mid, env->NewStringUTF(uid));
    }
    else
      cerr << "ERROR: Method deleteSapphireInstance not found. !!!" << endl;
}

void sapphire::helloWorld(char* app_string) {
     
    jmethodID mid = env->GetMethodID(cls, "sapphire_HelloWorld", "(Ljava/lang/String;Ljava/lang/String;)V");
    if(mid != NULL)
    {
      env->CallObjectMethod(myobj, mid, env->NewStringUTF(uid), env->NewStringUTF(app_string));
    }
    else
      cerr << "ERROR: Method sapphire_HelloWorld not found. !!!" << endl;
}


// Interface function for replacing CPP function with Java function.golang function.
// The fist parameter for taking the this pointer of C++.
void sapphire_HelloWorld(void* addr, char* app_string) {
 
   sapphire *sobj = new sapphire;
   sobj->helloWorld(app_string);
   delete sobj;
}



// Interface function for creating the new thread which will load the JVM.
// The fist parameter for taking the this pointer of C++.
void thread_HelloWorld(void* addr, char* app_string) {

  std::thread thr(&sapphire_HelloWorld, addr, app_string);
  thr.join();

  cout << "sapphire_jni.cpp main thread exiting. !!!" << endl;
}


LLTapHookInfo sapphire_hooks[] = {
    // C++ LLTAP Hook details.
    {(char*)"_ZN8Sapphire19sapphire_helloWorldEPc", (LLTapHook)thread_HelloWorld, LLTAP_REPLACE_HOOK},
    
    // Golang LLTAP Hook details.
    {(char*)"main.sapphire_helloWorld.N13_main.Sapphire", (LLTapHook)thread_HelloWorld, LLTAP_REPLACE_HOOK},
    
    // Indicates the end of list.
    {(char*)NULL, (LLTapHook)NULL, (LLTapHookType)0},
    };
LLTAP_REGISTER_HOOKS(sapphire_hooks)

