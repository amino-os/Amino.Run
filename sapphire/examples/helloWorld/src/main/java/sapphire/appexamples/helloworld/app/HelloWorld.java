package sapphire.appexamples.helloworld.app;

import java.io.Serializable;
import java.util.ArrayList;

import sapphire.app.*;
import sapphire.policy.cache.CacheLeasePolicy;

public class HelloWorld  implements SapphireObject<CacheLeasePolicy>{
	String name = "initialized name";

	public HelloWorld(String name) {
		this.name = name;
	}

	public String printName(String clientName) {
		System.out.println("Client Name is:" + clientName);
		System.out.println("Hello World!");
		return "Hello World Printed On: " + this.name;
	}
}
