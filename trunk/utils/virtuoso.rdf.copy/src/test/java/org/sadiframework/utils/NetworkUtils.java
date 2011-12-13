package org.sadiframework.utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NetworkUtils {

	public static List<Integer> getUnusedLocalPorts(int numPorts) throws IOException {
	
		Collection<ServerSocket> servers = new ArrayList<ServerSocket>();
		List<Integer> freePorts = new ArrayList<Integer>();
	
		for (int i = 0; i < numPorts; i++) {
			ServerSocket server = new ServerSocket(0);
			servers.add(server);
			freePorts.add(server.getLocalPort());
		}
	
		for (ServerSocket server : servers)
			server.close();
	
		return freePorts;
	}

}
