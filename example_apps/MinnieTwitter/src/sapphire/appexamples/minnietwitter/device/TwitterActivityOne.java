package sapphire.appexamples.minnietwitter.device;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import sapphire.appexamples.minnietwitter.app.TagManager;
import sapphire.appexamples.minnietwitter.app.Timeline;
import sapphire.appexamples.minnietwitter.app.Tweet;
import sapphire.appexamples.minnietwitter.app.TwitterManager;
import sapphire.appexamples.minnietwitter.app.User;
import sapphire.appexamples.minnietwitter.app.UserManager;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class TwitterActivityOne {
	
	public static void main(String[] args) {
		Registry registry;
		try {
			registry = LocateRegistry.getRegistry(args[0],Integer.parseInt(args[1]));
			OMSServer server = (OMSServer) registry.lookup("SapphireOMS");

			System.out.println("Connected to the OMS: " + server);
			
            KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress("10.0.2.15", Integer.parseInt(args[1])), new InetSocketAddress(args[2], Integer.parseInt(args[3])));
            
			TwitterManager tm = (TwitterManager) server.getAppEntryPoint();
            System.out.println("Received Twitter Manager Stub: " + tm);
            
            UserManager userManger = tm.getUserManager();
            User u = userManger.getUser("user12");
            Timeline timeline = u.getTimeline();
            
            List<Tweet> peerTweets = timeline.getTweets(0, 100);
            
            System.out.println("User user10 has " + peerTweets.size() + " tweets");
            
            for (Tweet t : peerTweets) {
            	System.out.println("Tweet: " + t.getText());
            	System.out.println("No. retweets: " + t.getRetweetes());
            	System.out.println("No. favorites: " + t.getFavorites());
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

//Feed feed = me.getFeed();		
// Obs. Better to get Feed if used often than call me.getFeed().tweet => two RMIs becomes more expensive