package sapphire.appexamples.minnietwitter.glue;

import com.example.minnietwitter.R;
import sapphire.appexamples.minnietwitter.device.generator.TwitterWorldGenerator;



import android.os.Bundle;
import android.os.AsyncTask;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		new GenerateWorld().execute(Configuration.hostAddress);
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
				TwitterWorldGenerator.main(params);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return response;
		}

	}
}
