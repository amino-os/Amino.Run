package amino.run.appexamples.minnietwitter;

import amino.run.app.DMSpec;
import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.ArgumentParser.AppArgumentParser;
import amino.run.common.MicroServiceCreationException;
import amino.run.common.MicroServiceID;
import amino.run.common.MicroServiceNameModificationException;
import amino.run.common.MicroServiceNotFoundException;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;
import amino.run.policy.atleastoncerpc.AtLeastOnceRPCPolicy;
import com.google.devtools.common.options.OptionsParser;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MinnieTwitterMain {
    static final int EVENTS_PER_USER = 10;
    static final int USER_COUNT = 10;
    static final int TAG_COUNT = 5;

    static final int MAX_TAGS_PER_TWEET = 2;
    static final int MAX_MENTIONS_PER_TWEET = 2;

    private static final Random gen = new Random();
    private static final String[] events = {"TWEET", "TWEET", "TWEET", "RETWEET", "FAVORITE"};

    private static String getTweet(int num) {
        int numTags = gen.nextInt(MAX_TAGS_PER_TWEET);
        int numMentions = gen.nextInt(MAX_MENTIONS_PER_TWEET);

        String tweet = "Tweet " + num + " ";

        for (int i = 0; i < numTags; i++) {
            tweet += getTag() + " ";
        }

        for (int i = 0; i < numMentions; i++) {
            tweet += "@" + getUserName() + " ";
        }

        return tweet;
    }

    private static String getTweet() {
        return getTweet(0);
    }

    private static String getTag() {
        return "#tag" + gen.nextInt(TAG_COUNT);
    }

    private static String getUserName() {
        return "user" + gen.nextInt(USER_COUNT);
    }

    private static int getId(int max) {
        return gen.nextInt(max);
    }

    private static void printStatistics() {}

    /**
     * (Sungwook Moon, 12/5/2017) This is a temporary method for demo to show shift policy. This
     * method adds a single user and send tweet messages for predefined times.
     */
    private static void ExecuteSingleUserDemo(
            InetSocketAddress hostAddr, InetSocketAddress omsAddr) {
        java.rmi.registry.Registry registry;

        try {
            registry = LocateRegistry.getRegistry(omsAddr.getHostName(), omsAddr.getPort());
            Registry server = (Registry) registry.lookup("io.amino.run.oms");

            KernelServer nodeServer = new KernelServerImpl(hostAddr, omsAddr);

            /* Get Twitter and User Manager */
            MicroServiceSpec spec =
                    MicroServiceSpec.newBuilder()
                            .setLang(Language.java)
                            .setJavaClassName("amino.run.appexamples.minnietwitter.TwitterManager")
                            .create();

            MicroServiceID microServiceId = server.create(spec.toString());
            TwitterManager tm = (TwitterManager) server.acquireStub(microServiceId);

            /* To set a name to microservice. It is required to set the name if the object has to be shared */
            server.setName(microServiceId, "MyTwitterManager");

            /* Attach to microservice is to get reference to shared microservice. Generally it
            is not done in the same thread which creates microservice. In this example,
            Twitter manager microservice is created just above in same thread. Below attach call
            has no significance. It is just used to show the usage of API. */
            TwitterManager tmAttached = (TwitterManager) server.attachTo("MyTwitterManager");

            /* Detach from the shared microservice. It is necessary to explicitly call detach to
            un-reference the microservice. This call is not required here if attach call was not
            made above */
            server.detachFrom("MyTwitterManager");

            UserManager userManager = tm.getUserManager();
            TagManager tagManager = tm.getTagManager();

            /* Create the users */
            List<User> users = new ArrayList<User>();
            User u = userManager.addUser("user" + 0, "user" + 0);
            System.out.println("Added user0");

            /* Generate events */
            int cnt = 0;
            u = userManager.getUser("user0");
            Timeline t = u.getTimeline();

            for (int i = 0; i < EVENTS_PER_USER; i++) {
                cnt++;
                int userId = i % USER_COUNT;
                String tweet = getTweet(cnt);

                try {
                    t.tweet(tweet);
                } catch (Exception e) {
                    System.out.print(", Failed ");
                }
                System.out.println("\n@user" + Integer.toString(userId) + " tweeted: " + tweet);
            }

            System.out.println("Done populating!");

            /* Explicit deletion from app */
            userManager.deleteUser("user" + 0);
            tm.deInitialize();
            server.delete(microServiceId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create an instance of the TwitterManager Microservice
     *
     * @param oms
     * @return
     * @throws RemoteException
     * @throws MicroServiceCreationException
     * @throws MicroServiceNotFoundException
     */
    private static TwitterManager createTwitterManager(Registry oms, String microServiceName)
            throws RemoteException, MicroServiceCreationException, MicroServiceNotFoundException,
                    MicroServiceNameModificationException {
        /* Get Twitter and User Manager */
        MicroServiceSpec spec =
                MicroServiceSpec.newBuilder()
                        .setLang(Language.java)
                        .setJavaClassName("amino.run.appexamples.minnietwitter.TwitterManager")
                        .addDMSpec(
                                DMSpec.newBuilder()
                                        .setName(AtLeastOnceRPCPolicy.class.getName())
                                        .create())
                        .create();

        MicroServiceID microServiceId = oms.create(spec.toString());
        /* To set a name to microservice. It is required to set the name if the object has to be shared */
        oms.setName(microServiceId, microServiceName);
        return (TwitterManager) oms.acquireStub(microServiceId);
    }

    /**
     * Check peer tweets on an existing twitter microservice. This is based on TwitterActivityOne in
     * the original Sapphire examples.
     */
    private static void checkPeerTweets(TwitterManager tm) {
        UserManager usrMan = tm.getUserManager();
        TagManager tagMan = tm.getTagManager();
        for (int i = 0; i < USER_COUNT; i++) {
            String userName = "user" + Integer.toString(i);
            User user = usrMan.getUser(userName);
            Timeline timeline = user.getTimeline();
            List<Tweet> peerTweets = timeline.getTweets(0, EVENTS_PER_USER);
            System.out.println("User " + userName + " has " + peerTweets.size() + " tweets");

            for (Tweet t : peerTweets) {
                System.out.println(
                        "Tweet: '"
                                + t.getText()
                                + "'; number of retweets: "
                                + t.getRetweetes()
                                + "; number of favorites: "
                                + t.getFavorites());
            }
        }
    }

    /**
     * Check that following works on an existing twitter microservice. This is based on
     * TwitterActivityTwo in the original Sapphire examples.
     */
    private static void checkFollowing(TwitterManager tm) {
        UserManager usrMan = tm.getUserManager();
        TagManager tagMan = tm.getTagManager();

        for (int i = 0; i < USER_COUNT - 1; i = i + 2) {
            String myName = "user" + i, peerName = "user" + (i + 1);

            System.out.println("Peering " + myName + " and " + peerName);
            User me = usrMan.getUser(myName), peer = usrMan.getUser(peerName);
            Timeline myTimeline = me.getTimeline(), peerTimeline = peer.getTimeline();

            peer.addFollowing(me);
            me.addFollower(peer);

            peerTimeline.tweet("peer hello");
            List<Tweet> peerTweets = peerTimeline.getTweets(0, 1);
            myTimeline.retweet(peerTweets.get(0));

            List<Tweet> myTweets = myTimeline.getTweets(0, 1);
            System.out.println("My tweet: " + myTweets.get(0).getText());

            tagMan.addTag("#goodlife", myTweets.get(0));
            List<Tweet> tweetsForTag = tagMan.getTweetsForTag("#goodlife", 0, 1);
            System.out.println("Tweet for tag #goodlife: " + tweetsForTag.get(0).getText());

            System.out.println(
                    "Retweets: "
                            + peerTimeline.getRetweets(peerTweets.get(0).getId(), 0, 1).get(0));
        }
    }

    private static void runFullDemo(InetSocketAddress hostAddr, InetSocketAddress omsAddr)
            throws Exception {
        // First find OMS
        java.rmi.registry.Registry rmiRegistry;
        new KernelServerImpl(
                hostAddr,
                omsAddr); // TODO quinton: This is the so-called fake kernel server. Figure out a
        // better way.  This is messy.
        rmiRegistry = LocateRegistry.getRegistry(omsAddr.getHostString(), omsAddr.getPort());
        Registry oms = (Registry) rmiRegistry.lookup("io.amino.run.oms");

        String microServiceName = "MyTwitterManager";
        TwitterManager clients[] = new TwitterManager[3];
        // Start the microservice
        clients[0] = createTwitterManager(oms, microServiceName);

        // Client 0 populates a bunch of users, tweets, retweets etc
        populate(clients[0]);

        // Client 1 does some basic checks that the above stuff exists.
        /* Attach to microservice is to get reference to shared microservice. Generally it
          is not done in the same thread which creates microservice.
        */
        clients[1] = (TwitterManager) oms.attachTo(microServiceName);
        checkPeerTweets(clients[1]);

        // Client 2 does some following and stuff
        clients[2] = (TwitterManager) oms.attachTo(microServiceName);
        checkFollowing(clients[2]);

        /* Detach from the shared microservice. It is necessary to explicitly call detach to
             un-reference the microservice. This call is not required here if attach call was not
             made above
          TODO: Quinton: Whooah!  There's a problem here.  No way to independently detach clients.
          What is detach supposed to be used for?  Garbage collection? But it seems that the implementation
          of detachFrom() just deletes the one and only reference to the microservice in OMS.
          Venu, I think you implemented this?  It all seems very wrong?
          In addition to that, why does the creator of the microservice instance not need to
          detach their reference.  I think this might all be broken?
        */
        oms.detachFrom(microServiceName);
    }

    /**
     * Populate a TwitterManager with some data
     *
     * @param tm the TwitterManager to populate.
     */
    private static void populate(TwitterManager tm) {

        List<Timeline> timelines = new ArrayList<Timeline>();

        try {
            /* Get User and Tag Manager */
            UserManager userManager = tm.getUserManager();
            TagManager tagManager = tm.getTagManager();
            /*
             * TODO quinton: The rest of this function is still really horrible code.
             * It's the original UW code, that I've just moved here, but not had time to
             * clean up properly yet.
             */
            /* Create the users */
            List<User> users = new ArrayList<User>();
            for (int i = 0; i < USER_COUNT; i++) {
                long start = System.nanoTime();
                User u = userManager.addUser("user" + i, "user" + i);
                timelines.add(i, u.getTimeline());
                long end = System.nanoTime();
                System.out.println("Added user " + i + " in:" + ((end - start) / 1000000) + "ms");
            }

            System.out.println("Finished adding " + USER_COUNT + " users");

            /* Generate events */
            for (int i = 0; i < USER_COUNT * EVENTS_PER_USER; i++) {
                int userId = i % USER_COUNT;
                String event = events[gen.nextInt(events.length)];

                if (event.equals("TWEET")) {
                    long start = System.nanoTime();
                    String tweet = getTweet(i);
                    Timeline t = timelines.get(userId);
                    t.tweet(tweet);
                    long end = System.nanoTime();
                    System.out.println(
                            "@user"
                                    + Integer.toString(userId)
                                    + " tweeted: "
                                    + tweet
                                    + " in: "
                                    + ((end - start) / 1000000)
                                    + "ms");
                    continue;
                }

                if (event.equals("RETWEET") && userId > 0) {
                    /* Retweet one of the last 10 tweets of some user */
                    long start = System.nanoTime();
                    int id = getId(USER_COUNT);
                    List<Tweet> lastTweets = timelines.get(id).getTweets(0, 10);
                    if (lastTweets.size() > 0) {
                        Tweet t = lastTweets.get(gen.nextInt(lastTweets.size()));
                        timelines.get(userId).retweet(t);
                        long end = System.nanoTime();
                        System.out.println(
                                "@user"
                                        + Integer.toString(userId)
                                        + " retweeted from @user"
                                        + Integer.toString(id)
                                        + " in: "
                                        + ((end - start) / 1000000)
                                        + "ms");
                    }
                    continue;
                }

                if (event.equals("FAVORITE") && userId > 0) {
                    long start = System.nanoTime();
                    /* Favorite one of the last 10 tweets of some user */
                    int id = getId(userId);
                    List<Tweet> lastTweets = timelines.get(id).getTweets(0, 10);
                    if (lastTweets.size() > 0) {
                        timelines
                                .get(id)
                                .favorite(
                                        lastTweets.get(gen.nextInt(lastTweets.size())).getId(),
                                        "user" + Integer.toString(userId));
                        long end = System.nanoTime();
                        System.out.println(
                                "@user"
                                        + Integer.toString(userId)
                                        + " favorited from @user"
                                        + Integer.toString(id)
                                        + " in: "
                                        + ((end - start) / 1000000)
                                        + "ms");
                    }
                }
            }

            System.out.println("Done populating.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        OptionsParser parser = OptionsParser.newOptionsParser(AppArgumentParser.class);
        if (args.length < 8) {
            System.out.println("Incorrect arguments to the program");
            printUsage(parser);
            return;
        }
        try {
            parser.parse(args);
        } catch (Exception e) {
            printUsage(parser);
            return;
        }

        AppArgumentParser appArgs = parser.getOptions(AppArgumentParser.class);

        InetSocketAddress
                hostAddr = new InetSocketAddress(appArgs.kernelServerIP, appArgs.kernelServerPort),
                omsAddr = new InetSocketAddress(appArgs.omsIP, appArgs.omsPort);
        try {
            runFullDemo(hostAddr, omsAddr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printUsage(OptionsParser parser) {
        System.out.println(
                "Usage: java -cp <classpath> "
                        + MinnieTwitterMain.class.getSimpleName()
                        + System.lineSeparator()
                        + parser.describeOptions(
                                Collections.<String, String>emptyMap(),
                                OptionsParser.HelpVerbosity.LONG));
    }
}
