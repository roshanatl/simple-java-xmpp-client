package com.roshan.chat;
 
public class XmppTest {
 
public static void main(String[] args) throws Exception {
 
String username = "java";
String password = "password";
 
XmppManager xmppManager = new XmppManager("3.1.172.94", 5222);
 
xmppManager.init();
xmppManager.performLogin(username, password);
xmppManager.setStatus(true, "Hello everyone");
 
//xmppManager.createEntry(buddyJID, buddyName);
 
xmppManager.sendMessage("hi this is a message", "bob@3.1.172.94");
 
boolean isRunning = true;
 
while (isRunning) {
Thread.sleep(50);
}
 
xmppManager.destroy();
 
}
 
}