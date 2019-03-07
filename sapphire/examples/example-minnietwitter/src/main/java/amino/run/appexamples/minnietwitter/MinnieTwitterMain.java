package amino.run.appexamples.minnietwitter;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import amino.run.app.Language;
import amino.run.app.MicroServiceSpec;
import amino.run.app.Registry;
import amino.run.common.MicroServiceID;
import amino.run.kernel.server.KernelServer;
import amino.run.kernel.server.KernelServerImpl;

public class MinnieTwitterMain {
  static final int EVENTS_PER_USER = 100;
  static final int USERS_NUM = 1;
  static final int TAG_NUM = 5;

  static final int MAX_TAGS_PER_TWEET = 2;
  static final int MAX_MENTIONS_PER_TWEET = 2;

  private static final Random gen = new Random();
  private static final String[] events = {"TWEET", "TWEET", "TWEET", "RETWEET", "FAVORITE"};

  private static String getTweet() {
    int numTags = gen.nextInt(MAX_TAGS_PER_TWEET);
    int numMentions = gen.nextInt(MAX_MENTIONS_PER_TWEET);

    String tweet = "Tweet ";

    for (int i = 0; i < numTags; i++) {
      tweet += getTag() + " ";
    }

    for (int i = 0; i < numMentions; i++) {
      tweet += "@" + getUserName() + " ";
    }

    return tweet;
  }

  private static String getTweet(int num) {
    int numTags = gen.nextInt(MAX_TAGS_PER_TWEET);
    int numMentions = gen.nextInt(MAX_MENTIONS_PER_TWEET);

    String tweet = "Tweet " + num;

    return tweet;
  }

  private static String getTag() {
    return "#tag" + gen.nextInt(TAG_NUM);
  }

  private static String getUserName() {
    return "user" + gen.nextInt(USERS_NUM);
  }

  private static int getId(int max) {
    return gen.nextInt(max);
  }

  private static void printStatistics() {}

  /**
   * (Sungwook Moon, 12/5/2017) This is a temporary method for demo to show shift policy. This
   * method adds a single user and send tweet messages for predefined times.
   */
  private static void ExecuteSingleUserDemo(InetSocketAddress hostAddr, InetSocketAddress omsAddr) {
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

      /* To set a name to sapphire object. It is required to set the name if the object has to be shared */
      server.setName(microServiceId, "MyTwitterManager");

      /* Attach to sapphire object is to get reference to shared sapphire object. Generally it
      is not done in the same thread which creates sapphire object. In this example,
      Twitter manager sapphire object is created just above in same thread. Below attach call
      has no significance. It is just used to show the usage of API. */
      TwitterManager tmAttached =
          (TwitterManager) server.attachTo("MyTwitterManager");

      /* Detach from the shared sapphire object. It is necessary to explicitly call detach to
      un-reference the sapphire object. This call is not required here if attach call was not
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
        int userId = i % USERS_NUM;
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

  private static void ExecuteFullDemo(InetSocketAddress hostAddr, InetSocketAddress omsAddr) {

    java.rmi.registry.Registry registry;
    List<Timeline> timelines = new ArrayList<Timeline>();

    try {
      registry = LocateRegistry.getRegistry(omsAddr.getHostString(), omsAddr.getPort());
      Registry server = (Registry) registry.lookup("io.amino.run.oms");

      KernelServer nodeServer = new KernelServerImpl(hostAddr, omsAddr);

      /* Get Twitter and User Manager */
      MicroServiceID microServiceId =
          server.create(
              "amino.run.appexamples.minnietwitter.TwitterManager", new Object[0]);
      TwitterManager tm = (TwitterManager) server.acquireStub(microServiceId);
      UserManager userManager = tm.getUserManager();
      TagManager tagManager = tm.getTagManager();

      /* Create the users */
      List<User> users = new ArrayList<User>();
      for (int i = 0; i < USERS_NUM; i++) {
        long start = System.nanoTime();
        User u = userManager.addUser("user" + Integer.toString(i), "user" + Integer.toString(i));
        timelines.add(i, u.getTimeline());
        long end = System.nanoTime();
        System.out.println(
            "Added user " + Integer.toString(i) + " in:" + ((end - start) / 1000000) + "ms");
      }

      System.out.println("Added users!");

      /* Generate events */
      for (int i = 0; i < USERS_NUM * EVENTS_PER_USER; i++) {
        int userId = i % USERS_NUM;
        String event = events[gen.nextInt(events.length)];

        if (event.equals("TWEET")) {
          long start = System.nanoTime();
          String tweet = getTweet();
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
          int id = getId(userId);
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

      System.out.println("Done populating!");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {

    if (args.length < 4) {
      System.out.println("Incorrect arguments to the program");
      System.out.println(
          "usage: "
              + MinnieTwitterMain.class.getSimpleName()
              + " <oms ip> <oms-port> <host-ip> <host-port>");
      System.exit(1);
    }
    String omsIp = args[0], omsPort = args[1], hostIp = args[2], hostPort = args[3];
    InetSocketAddress hostAddr = new InetSocketAddress(hostIp, Integer.parseInt(hostPort)),
        omsAddr = new InetSocketAddress(omsIp, Integer.parseInt(omsPort));
    ExecuteSingleUserDemo(hostAddr, omsAddr);
  }
}
