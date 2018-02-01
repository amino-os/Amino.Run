/*
 * Copyright (C) 2015 Federico Iosue (federico.iosue@gmail.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.feio.android.omninotes.async.notes;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;
import de.greenrobot.event.EventBus;
import it.feio.android.checklistview.App;
import it.feio.android.omninotes.async.bus.NotesLoadedEvent;
import it.feio.android.omninotes.cloud.AppManager;
import it.feio.android.omninotes.db.DbHelper;
import it.feio.android.omninotes.models.Note;
import it.feio.android.omninotes.utils.Constants;
import sapphire.common.AppObjectStub;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;


public class NoteLoaderTask extends AsyncTask<Object, Void, ArrayList<Note>> {

	private static NoteLoaderTask instance;

	private NoteLoaderTask() {}


	public static NoteLoaderTask getInstance() {

		if (instance != null) {
			if (instance.getStatus() == Status.RUNNING && !instance.isCancelled()) {
				instance.cancel(true);
			} else if (instance.getStatus() == Status.PENDING) {
				return instance;
			}
		}

		instance = new NoteLoaderTask();
		return instance;
	}


	@Override
	protected ArrayList<Note> doInBackground(Object... params) {
		if (KernelServerImpl.oms == null) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
			/*
			// start KS in emulator - ensure oms w/ 10.0.2.2
			String hardcodedArgs[] = {
					"127.0.0.1",  //unable to start KS in emulator right now due to NAT issue
					"22345",
					"10.0.2.2",
					"22343"
			};
			//*/
			///*
			// start KS in device (ensure oms starts w/ proper public ip address
			String hardcodedArgs[] = {
					"192.168.10.154", //"192.168.10.109", //device ip
					"22345",
					"192.168.10.22",  //laptop ip
					"22343"
			};
			//*/
			// KernelServerImpl.main(hardcodedArgs);
			try {
				//System.setProperty("java.rmi.server.hostname", "192.168.10.109");

				KernelServerImpl server = new KernelServerImpl(
						new InetSocketAddress(hardcodedArgs[0], 22345),
						new InetSocketAddress(hardcodedArgs[2], 22343));

				KernelServer stub = (KernelServer) java.rmi.server.UnicastRemoteObject.exportObject(server, 0);
				java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.createRegistry(Integer.parseInt(hardcodedArgs[1]));
				registry.rebind("SapphireKernelServer", stub);
				KernelServerImpl.oms.registerKernelServer(
						// 10.0.2.15?
						new InetSocketAddress(hardcodedArgs[0], 22345));

				System.out.println("Server ready!");
				server.getMemoryStatThread().start();

				ArrayList<InetSocketAddress> kss = KernelServerImpl.oms.getServers();

				// hwc: to work around oms startAE blocking issue
				InetSocketAddress ks = kss.get(0);

				Registry ksRegistry = LocateRegistry.getRegistry(ks.getHostString(), ks.getPort());
				KernelServer rks = (KernelServer) ksRegistry.lookup("SapphireKernelServer");
				AppObjectStub appEP = rks.startApp("it.feio.android.omninotes.cloud.OmniNotesApp");

				// get hold of root SO
				// via oms - oms starts w/ right class name
				//AppObjectStub appEP = KernelServerImpl.oms.getAppEntryPoint();
				/*
				// alternatively
				Registry localRegistry = LocateRegistry.getRegistry(22345);
				KernelServer lks = (KernelServer) localRegistry.lookup("SapphireKernelServer");
				AppObjectStub appEP = lks.startApp("it.feio.android.omninotes.cloud.OmniNotesApp");
				// AppObjectStub appEP = server.startApp("it.feio.android.omninotes.cloud.OmniNotesApp");
				//*/

				AppManager mgr = (AppManager)appEP;
				mgr.setMemo("SO - no extends!");
				String memo = mgr.getMemo();
				it.feio.android.omninotes.db.DbHelper dbHelper= mgr.getDbHelper();
				AppManager.setStaticDbHelper(dbHelper);
				//DbHelper.setInstance(dbHelper);

				ArrayList<InetSocketAddress> kss2 = KernelServerImpl.oms.getServers();
			} catch (Exception e) {
				System.out.println(e);
			}
		}

		ArrayList<Note> notes = new ArrayList<>();
		String methodName = params[0].toString();
		Object methodArgs = params[1];
		//DbHelper db = DbHelper.getInstance();
		//DbHelper db = AppManager.getInstance().getDbHelper();
		//AppManager.setStaticDbHelper(db);
		DbHelper db = AppManager.getStaticDbHelper();

		// If null argument an empty list will be returned
		if (methodArgs == null) {
			return notes;
		}

		// Checks the argument class with reflection
		Class[] paramClass = new Class[]{methodArgs.getClass()};

		// Retrieves and calls the right method
		try {
			Method method = db.getClass().getDeclaredMethod(methodName, paramClass);
			notes = (ArrayList<Note>) method.invoke(db, paramClass[0].cast(methodArgs));
		} catch (Exception e) {
			Log.e(Constants.TAG, "Error retrieving notes", e);
		}

		return notes;
	}


	@Override
	protected void onPostExecute(ArrayList<Note> notes) {

		super.onPostExecute(notes);
		EventBus.getDefault().post(new NotesLoadedEvent(notes));
	}
}