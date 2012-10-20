package ltg.commons;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an XMPP client that is ready to use. It handles only one connection which can be 
 * either point-to-point or a group chat. There are methods to actively (using 
 * <code>PacketCollector</code> and passively (using <code>PacketListener</code>) listen for
 * packets. There are send methods to send messages to the group chat or to individual users.
 * 
 * @author tebemis
 *
 */
public class SimpleXMPPClient {
	
	// Logger
	protected Logger log = LoggerFactory.getLogger(getClass());
	// Connection
	protected XMPPConnection connection = null;
	// Group chat (can be null)
	protected MultiUserChat groupChat = null;
	// Packet collector (can be null)
	protected PacketCollector packetCollector = null; 
	
	
	public SimpleXMPPClient(String fullJid, String password) {
		// Parse username and hostname
		String[] sa = fullJid.split("@", 2);
		String username = sa[0];
		String hostname = sa[1];
		// Connect
		connection = new XMPPConnection(hostname);
		try {
			connection.connect();
		} catch (XMPPException e) {
			log.error("Impossible to CONNECT to the XMPP server, terminating");
			Thread.currentThread().interrupt();
		}
		// Authenticate
		try {
			connection.login(username, password);
		} catch (XMPPException e) {
			log.error("Impossible to LOGIN to the XMPP server, terminating");
			Thread.currentThread().interrupt();
		} catch (IllegalArgumentException e) {
			// This needs to be here because the MultiUserChat implementation
			// in smackx is crappy. They throw exceptions if the username is "".
			log.error("Impossible to LOGIN to the XMPP server, terminating");
			Thread.currentThread().interrupt();
		}
	}
	
	
	public SimpleXMPPClient(String fullJid, String password, String chatRoom) {
		// Connect and authenticate
		this(fullJid, password);
		// Initialize and join chatRoom
		 groupChat = new MultiUserChat(connection, chatRoom);
		 try {
			groupChat.join(connection.getUser());
		} catch (XMPPException e) {
			log.error("Impossible to join GROUPCHAT, terminating");
			Thread.currentThread().interrupt();
		}
	}
	
	
	public Event nextEvent() {
		if (packetCollector==null)
			packetCollector = connection.createPacketCollector(new PacketTypeFilter(Message.class));
		Message m = (Message) packetCollector.nextResult();
		return new Event(m.getFrom(), m.getBody());
	}
	
	
	public void registerEventListener(final EventListener eventListener) {
		PacketListener pl = new PacketListener() {
			@Override
			public void processPacket(Packet packet) {
				Message m = (Message) packet;
				Event e = new Event(m.getFrom(), m.getBody());
				eventListener.processEvent(e);
			}
		};
		connection.addPacketListener(pl, new PacketTypeFilter(Message.class));
	}
	
	
	public void sendMessage(String to, String message) {
		if (connection==null || !connection.isAuthenticated()){
			log.error("Impossible to send message to " +to + ": we have been disconnected! Terminating");
			Thread.currentThread().interrupt();
		}
		Message m = new Message(to, Message.Type.normal);
		m.setBody(message);
		connection.sendPacket(m);
	}
	
	
	public void sendMessage(String message) {
		if (connection==null || !connection.isAuthenticated() || !groupChat.isJoined()){
			log.error("Impossible to send message to groupchat: we have been disconnected! Terminating");
			Thread.currentThread().interrupt();
		}
		Message m = new Message(groupChat.getRoom(), Message.Type.groupchat);
		m.setBody(message);
        connection.sendPacket(m);
	}
	
	
	public void disconnect() {
		if (groupChat.isJoined()) {
			groupChat.leave();
			groupChat = null;
		}
		if (connection.isAuthenticated()) {
			connection.disconnect();
			connection = null;
		}
	}

}
