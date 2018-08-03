package sapphire.appexamples.hankstodo.glue;

import com.example.hankstodo.R;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import sapphire.appexamples.hankstodo.app.AppGrpcClient;
import sapphire.appexamples.hankstodo.app.GlobalGrpcClientRef;
/*
import sapphire.appexamples.hankstodo.app.TodoList;
import sapphire.appexamples.hankstodo.app.TodoListManager;
*/
import sapphire.appexamples.hankstodo.app.grpcStubs.TodoListManager_Stub;
import sapphire.appexamples.hankstodo.app.grpcStubs.TodoList_Stub;
import sapphire.oms.OMSServer;

public class MainActivity extends Activity {
	public final static String EXTRA_MESSAGE = "com.example.hankstodo.MESSAGE";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new AccessRemoteObject().execute();
        setContentView(R.layout.activity_main);
    }
    
    private class AccessRemoteObject extends AsyncTask<String, Void, String>{
    	protected String doInBackground(String... params) {
    		String response = null;

            String [] args = { "192.168.42.171", "20005", "192.168.42.171", "20003", "192.168.42.171", "10003" };
            AppGrpcClient grpcClient = new AppGrpcClient(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]), args[4], Integer.parseInt(args[5]));
            GlobalGrpcClientRef.grpcClient = grpcClient;
            TodoListManager_Stub tlm;
            try {
                tlm = new TodoListManager_Stub();
                System.out.println("Finished");

            } catch (Exception e) {
                e.printStackTrace();
                return response;
            }
/*
            TodoList_Stub tl = tlm.newTodoList("Hanks");
            System.out.println("Received tl1: " + tl);
            System.out.println(tl.addToDo("First todo"));
            System.out.println(tl.addToDo("Second todo"));
            System.out.println(tl.addToDo("Third todo"));

            TodoList_Stub tl2 = tlm.newTodoList("AAA");
            System.out.println("Received tl2: " + tl2);
            System.out.println(tl2.addToDo("First todo"));
            System.out.println(tl2.addToDo("Second todo"));
            System.out.println(tl2.addToDo("Third todo"));

            TodoList_Stub tl3 = tlm.newTodoList("HHH");
            System.out.println("Received tl3: " + tl3);
            System.out.println(tl3.addToDo("First todo"));
            System.out.println(tl3.addToDo("Second todo"));
            System.out.println(tl3.addToDo("Third todo"));
*/
    		/*Registry registry;
    		try {
    			registry = LocateRegistry.getRegistry("128.208.4.114");
    			OMSServer server = (OMSServer) registry.lookup("SapphireOMS");
    			System.out.println(server);
    			TodoListManager tlm = (TodoListManager) server.getAppEntryPoint();
                System.out.println("Received tlm: " + tlm);
                
                //KernelServer nodeServer = new KernelServerImpl("10.0.2.15");
                
                runOnUiThread(new Runnable() {
                    public void run() {
                    	EditText txt = (EditText) findViewById(R.id.edit_message);
                        txt.setText("Executed");
                    }
                });
                
    			TodoList tl = tlm.newTodoList("Hanks");		
    			System.out.println("Received tl: " + tl);
    			System.out.println(tl.addToDo("First todo"));
    		} catch (Exception e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}*/
    		return response;
    	}
    }
    
    /** Called when the user clicks the Send button */
    public void sendMessage(View view) {
        Intent intent = new Intent(this, DisplayMessageActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_message);
        String message = editText.getText().toString();
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
}
