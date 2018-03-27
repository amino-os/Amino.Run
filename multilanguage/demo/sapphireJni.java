//
// This file corresponds to the DCAP-Sapphire JNI interface module.
// Have created the sapphireJni class to include the sapphire Java
// class in C++ class, for demonstarting how LLVM can be used in DCAP.
//

import java.util.UUID;
import java.util.HashMap;

public class sapphireJni {

  private static HashMap<String, sapphire> instances = new HashMap<String, sapphire>();
  
  public sapphireJni(){
  }
  
  public String createSapphireInstance() {
    System.out.println("Inside createSapphireInstance()");
    
    UUID uid = UUID.randomUUID();
    String uid_string = uid.toString();
    
    System.out.println("Creating Sapphire instance with UUID: " + uid_string);
    instances.put(uid_string, new sapphire());
    
    return uid_string; 
  }
  
  public void deleteSapphireInstance(String uid) {
    System.out.println("Deleting Sapphire instance with UUID: " + uid);
    instances.remove(uid);
  }
  
  // sapphire class interfaces.
  public void sapphire_HelloWorld(String uid, String str) {
  
    sapphire instance = instances.get(uid);
	if (instance != null) {	
      System.out.println("Calling Sapphire HelloWorld() with UUID: " + uid);
      // Call the corresponding call function.
      instance.HelloWorld(str);
	}
	else{
      System.out.println("Sapphire instance not found for the UUID: " + uid);
	}
  }
  
  public void sapphire_incrementSapphireObjCount(String uid) {
    // Call the corresponding call function.
    instances.get(uid).incrementSapphireObjCount();
  }
  
  public int sapphire_getSapphireObjCount(String uid) {
    // Call the corresponding call function.
    return instances.get(uid).getSapphireObjCount();
  }
  
}

