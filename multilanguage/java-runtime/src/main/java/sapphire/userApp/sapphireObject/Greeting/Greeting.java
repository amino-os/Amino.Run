/**
 * Created by Jithu Thomas on 21/6/18.
 */

package sapphire.userApp.sapphireObject.Greeting;

public class Greeting {

    public static String HelloWorld (String reqName, int reqNum) {
        System.out.printf("Inside Greeting HelloWorld(), Received params: \"%s\" and \"%d\"\n", reqName, reqNum);
        return "HelloWorld invoked in Java by DCAP platform.!!!";
    }

    public static String Fibbonaci (int reqNum) {
        System.out.println("Inside Greeting Fibbonaci(), Received param: " + reqNum);

        int n1 = 0, n2 = 1;
        for (int i = 0; i < reqNum; i++) {
            int temp = n1;
            n1 = n2;
            n2 = n1 + temp;
        }
        System.out.printf("Fibbonaci of number: %d is: %d\n", reqNum, n2);
        return Integer.toString(n2);
    }

    /*public static void main(String[] args) {
        System.out.println("Inside greeting_app main()");

        Greeting object = new Greeting();

        String resp = object.HelloWorld("Hello from DCAP", 2018);
        System.out.println("HelloWorld() response: " + resp);

        String resp_fib = object.Fibbonaci(10);
        System.out.println("Fibbonaci() response: " + resp_fib);
    }*/
}
