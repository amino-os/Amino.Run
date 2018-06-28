/**
 * Created by Jithu Thomas on 25/6/18.
 */

package sapphire.userApp.sapphireObject.Algo;

import java.util.Arrays;

public class Algo {

    public static int[] Sort(int[] arr) {
        System.out.println("Sort method invocked with input: " + Arrays.toString(arr));

        int temp = 0;
        for (int i=0; i < (arr.length-1); i++) {
            for (int j=0; j < (arr.length-i-1); j++){
                if (arr[j] >= arr[j+1]) {
                    temp = arr[j];
                    arr[j] = arr[j+1];
                    arr[j+1] = temp;
                }
            }
        }
        return arr;
    }

    public static int Search (Integer[] arr, int key) {
        System.out.printf("Search method invocked with input: %d and key: %d \n", arr, key);

        for (int i=0; i <arr.length; i++) {
            if(arr[i] == key) {
                System.out.printf("Search element :%d found at position: %d\n", key, i);
                return (i+1);
            }
        }
        System.out.printf("Search element: %d not found.\n", key);
        return -1;
    }

    public static String Fibbonaci (int reqNum) {
        System.out.println("Fibbonaci method invocked with param: " + reqNum);

        int n1 = 0, n2 = 1;
        for (int i = 0; i < reqNum; i++) {
            int temp = n1;
            n1 = n2;
            n2 = n1 + temp;
        }
        System.out.printf("Fibbonaci of number: %d is: \n", reqNum, n2);
        return Integer.toString(n2);
    }

}
