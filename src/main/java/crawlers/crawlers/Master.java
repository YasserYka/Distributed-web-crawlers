package crawlers.crawlers;

import crawlers.configuration.all;
import java.util.LinkedList;
import java.util.Queue;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.google.gson.Gson;


public class Master {

	
	public static void main(String args[]) {
		System.out.println("Master running");
		new Master().init();	
	}
	
	//Holds slaves whom ready for work
	private Queue<Slave> queueOfSlaves;
	//Router socket for Dealer-Router pattern
	private ZMQ.Socket ROUTER;
	//Publisher socket for Subscriber-Publisher pattern used to send heart beat to master
	private ZMQ.Socket PUB;
	//to read from multiple sockets
	private ZMQ.Poller poller;
	//Time to wait before sending next heart beat
	private int heartbeatInterval;
	//Time to send heart beat in msec
	private long nextHeartbeat;
	//event heart-beat
	private final String heartbeat = "\001";
	//event ready-for-work;
	private final String readyforTask = "\002";
	//event task is done
	private final String finishedTask = "\003";
	//event task to be done
	private final String taskToBeDone = "\004";
	//Address to bind-to for Dealer-Router locally
	private final String routerAddress = "tcp://127.0.0.1:5555";
	//Address to bind-to for Subscriber-Publisher locally
	private final String publisherAddress = "tcp://*:5556";
	//Subscr
	public Master() {
		queueOfSlaves = new LinkedList<Slave>();
		heartbeatInterval = all.HEARTBEAT_INTERVAL;
	}

	
	//This return first available slave's address
	public String getAvailableSlaves() {return queueOfSlaves.remove().getAddress();}
	
	public void dispatch() {
		//while there is URLs in frontier give them to slaves
		while(queueOfSlaves.size() > 0) {
			if(existUrlInFrontier()) {
				//sendTask(getAvailableSlaves());
			}
		}
	}
	
	public void sendTask(String address){
		ROUTER.sendMore(address);
		ROUTER.send(taskToBeDone, ZMQ.SNDMORE);
		ROUTER.send(getUrlFromFrontier());
		ROUTER.sendMore("");
	}
	
	private String _constructJsonWithEventAndBody(String event, String body) {
		return event + " " + body;
	}
	
	//If url exit fetch it 
	public boolean existUrlInFrontier() {
		return false;
	}
	
	//This removes slaves from the queue if they are expired
	public void killExpiredSlaves() {queueOfSlaves.removeIf(slave -> slave.getExpiration() > System.currentTimeMillis());}
	
	public void init() {
		try (ZContext context = new ZContext()) {
			
		      ROUTER = context.createSocket(SocketType.ROUTER);
		      PUB = context.createSocket(SocketType.PUB);
		      poller = context.createPoller(2);
		      
		      PUB.bind(publisherAddress);
		      ROUTER.bind(routerAddress);
		      
		      //Register two sockets in poller so to listen on both sockets
		      poller.register(ROUTER, ZMQ.Poller.POLLIN);
		      poller.register(PUB, ZMQ.Poller.POLLIN);
		      nextHeartbeat = System.currentTimeMillis() + heartbeatInterval;
		      
		      while (true) {
		    	  
		    	poller.poll(heartbeatInterval);
		    	
		    	//if it's time to send heart beat send it
		    	sendHearbeat();
		    	
		    	//message received from slave requesting for work
		    	if(poller.pollin(0)) {
		    		/*The message is received from the crawler is consist of three frames [Header][Event][Body]
		    			So recvMsg must be called three times
		    			Header contain the address of the slave*/
		    		ZMsg message = ZMsg.recvMsg(ROUTER);
		    		//if slave sent message of size 1 means it's ready for work
		    		if(message.size() == 1)
			    		insertSlave(ROUTER.recv(0).toString());
		    		else {
		    		//If header received from slave that would means its available for work otherwise it would be busy requesting a web page
		    		insertSlave(ROUTER.recv(0).toString());
		    		//Take action upon the event received
		    		handleEvent(ROUTER.recv(0).toString());
		    		//Receiving a body means slave got the job done
		    		handleDoneJob(ROUTER.recv(0).toString());
		    		//This life for debugging the content of message
		    		message.dump(System.out);
		    		}
		    	}
		    	killExpiredSlaves();
		     }
		}
	}
	
	//Takes the event frame and take action upon it
	public void handleEvent(String string) {
		if(string.equals(readyforTask)) {
			
		}
	}
	
	//When slave sends back response that means an crawled 
	public void handleDoneJob(String string) {
		
	}
	
	//Creates a new slave object for an address and enqueue it
	public void insertSlave(String address){queueOfSlaves.add(new Slave());}
	
	//TODO: override equal
	//This remove slave that is ready for work
	public void removeSlave(Slave slave) {
		//queueOfSlaves.remove(slave);
	}
	
	//This needs to be sent to alert slaves that master a live and if any new subscriber haven't pushed in queue and in idle state 
	public void sendHearbeat() {
		//It's time to send heart beat to all subscriber
		if(System.currentTimeMillis() > nextHeartbeat) {
			PUB.send(heartbeat);
			nextHeartbeat = System.currentTimeMillis() + heartbeatInterval;
		}
	}
	
	//TODO: contact frontier for URL
	public String getUrlFromFrontier() {
		return "";
	}

	
}