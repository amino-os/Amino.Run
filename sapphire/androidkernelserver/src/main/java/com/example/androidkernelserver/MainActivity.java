package com.example.androidkernelserver;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;

import com.example.androidkernelserver.R;

import sapphire.kernel.server.KernelServerImpl;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new GenerateWorld().execute("192.168.10.143", "22345", "192.168.10.130", "22346");
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private class GenerateWorld extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... params) {
            String response = null;
            try {
                KernelServerImpl.main(params);
                System.out.println("Done!");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }
    }
}
