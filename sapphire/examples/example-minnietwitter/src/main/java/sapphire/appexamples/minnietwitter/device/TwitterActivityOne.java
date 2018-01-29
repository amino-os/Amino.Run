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
import sapphire.kernel.common.GlobalKernelReferences;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class TwitterActivityOne {

    /**
     * To execute this application, please pass in three parameters: <OMS-IP> <OMS-Port> <KernelServer-Port>
     * @param args
     * <ul>
     *     <li><code>args[0]</code>:</li> OMS server IP address
     *     <li><code>args[1]</code>:</li> OMS server Port number
     *     <li><code>args[2]</code>:</li> Kernel server Port number
     * </ul>
     *
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        final String userName = "user12";
        final int tweetCnt = 10;

        Registry registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
        OMSServer server = (OMSServer) registry.lookup("SapphireOMS");
        System.out.println("Connected to the OMS: " + server);

        // This kernel server is a fake kernel server. It is _not_ registered in OMS. Therefore
        // there will be no Sapphire object on this server. The purpose of creating such a fake
        // kernel server is to construct a KernelClient (inside the KernelServer object) and to
        // configure GlobalKernelReferences.nodeServer properly.
        //
        // Since this is a fake kernel server, we do not have to use the real IP of this host.
        // We can use any IP address as long as it does not conflict with OMS IP and the IPs of
        // other kernel servers. To keep things simple, I hard coded it as "127.0.0.2".
        GlobalKernelReferences.nodeServer = new KernelServerImpl(new InetSocketAddress("127.0.0.2", Integer.parseInt(args[2])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

        TwitterManager tm = (TwitterManager) server.getAppEntryPoint();
        System.out.println("Received Twitter Manager Stub: " + tm);

        UserManager userManger = tm.getUserManager();
        userManger.addUser(userName, "pwd");
        User u = userManger.getUser(userName);
        Timeline timeline = u.getTimeline();
        for (int i = 0; i < tweetCnt; i++) {
            timeline.tweet("tweet_" + i);
        }

        List<Tweet> peerTweets = timeline.getTweets(0, tweetCnt);
        System.out.println(String.format("User %s has %d tweets", userName, peerTweets.size()));

        for (Tweet t : peerTweets) {
            System.out.println(String.format("==> Tweet: '%s'; number of retweets: %d; number of favorites: %d", t.getText(), t.getRetweetes(), t.getFavorites()));
        }
        System.exit(0);
    }
}
