package sapphire.appexamples.minnietwitter.device;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;

import sapphire.appexamples.minnietwitter.app.Timeline;
import sapphire.appexamples.minnietwitter.app.TagManager;
import sapphire.appexamples.minnietwitter.app.Tweet;
import sapphire.appexamples.minnietwitter.app.TwitterManager;
import sapphire.appexamples.minnietwitter.app.User;
import sapphire.appexamples.minnietwitter.app.UserManager;
import sapphire.common.SapphireObjectID;
import sapphire.kernel.server.KernelServer;
import sapphire.kernel.server.KernelServerImpl;
import sapphire.oms.OMSServer;

public class TwitterActivityTwo {
    /**
     * To execute this application, please pass in three parameters: <OMS-IP> <OMS-Port> <KernelServer-Port>
     *
     * @param args <ul>
     *             <li><code>args[0]</code>:</li> OMS server IP address
     *             <li><code>args[1]</code>:</li> OMS server Port number
     *             <li><code>args[2]</code>:</li> Kernel server Port number
     *             </ul>
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Registry registry = LocateRegistry.getRegistry(args[0], Integer.parseInt(args[1]));
        OMSServer server = (OMSServer) registry.lookup("SapphireOMS");

        // This kernel server is a fake kernel server. It is _not_ registered in OMS. Therefore
        // there will be no Sapphire object on this server. The purpose of creating such a fake
        // kernel server is to construct a KernelClient (inside the KernelServer object) and to
        // configure GlobalKernelReferences.nodeServer properly.
        //
        // Since this is a fake kernel server, we do not have to use the real IP of this host.
        // We can use any IP address as long as it does not conflict with OMS IP and the IPs of
        // other kernel servers. To keep things simple, I hard coded it as "127.0.0.2".
        KernelServer nodeServer = new KernelServerImpl(new InetSocketAddress("127.0.0.2", Integer.parseInt(args[2])), new InetSocketAddress(args[0], Integer.parseInt(args[1])));

        SapphireObjectID sapphireObjId = server.createSapphireObject("sapphire.appexamples.minnietwitter.app.TwitterManager");
        TwitterManager tm = (TwitterManager)server.acquireSapphireObjectStub(sapphireObjId);
        System.out.println("Received Twitter Manager Stub: " + tm);

        UserManager userManger = tm.getUserManager();
        TagManager tagManager = tm.getTagManager();

        User me = userManger.addUser("user1", "user1_password");
        Timeline myTimeline = me.getTimeline();
        User peer = userManger.getUser("user1");

        peer.addFollowing(me);
        me.addFollower(peer);

        Timeline peerTimeline = peer.getTimeline();
        peerTimeline.tweet("peer hello");
        List<Tweet> peerTweets = peerTimeline.getTweets(0, 1);
        myTimeline.retweet(peerTweets.get(0));

        List<Tweet> myTweets = myTimeline.getTweets(0, 1);
        System.out.println("My tweet:" + myTweets.get(0).getText());

        tagManager.addTag("#goodlife", myTweets.get(0));
        List<Tweet> tweetsForTag = tagManager.getTweetsForTag("#goodlife", 0, 1);
        System.out.println(tweetsForTag.get(0).getText());

        System.out.println("Retweets: " + peerTimeline.getRetweets(peerTweets.get(0).getId(), 0, 1).get(0));
        System.exit(0);
    }
}
