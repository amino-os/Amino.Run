//
// This file corresponds to the DCAP-Sapphire core module.
// Have created a demo Sapphire class to demonstrate,
// how LLVM can be used in DCAP.
//

public class sapphire {

  private static int sapphire_obj_count = 0;
  private static String str = "Hello World from DCAP-Sapphire.!!!!!";

  public void HelloWorld(String app_string) {
    System.out.println(str);
    
    // Lets print the string passed by the user application.
    System.out.println(app_string);
  }
  
  public void incrementSapphireObjCount() {
    sapphire_obj_count++;
  }
  
  public int getSapphireObjCount() {
    System.out.println("Sapphire object count is: " + sapphire_obj_count);
    return sapphire_obj_count;
  }
}

