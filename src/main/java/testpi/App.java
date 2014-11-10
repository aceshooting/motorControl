package testpi;
import io.undertow.Undertow;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import java.util.ArrayList;
import static io.undertow.Handlers.*;
/**
* Created by duke on 7/19/14.
*/
public class App implements Runnable {
private static final ArrayList<WebSocketChannel> connected = new ArrayList<WebSocketChannel>();
public static void main(String[] args) {
/*
Undertow server = Undertow.builder()
.addHttpListener(8080, "localhost")
.setHandler(new HttpHandler() {
@Override
public void handleRequest(final HttpServerExchange exchange) throws Exception {
exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
exchange.getResponseSender().send("Hello World");
}
}).build();
server.start();
*/
new Thread(new App()).start();
Undertow server = Undertow.builder()
.addHttpListener(8080, "localhost")
.setHandler(path()
.addPrefixPath("/camremote", websocket(new WebSocketConnectionCallback() {
@Override
public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
connected.add(channel);
channel.getReceiveSetter().set(new AbstractReceiveListener() {
@Override
protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
String data = message.getData();
if(data.trim().equals("start")){
System.out.println("Start !!!");
}
if(data.trim().equals("stop")){
System.out.println("stop !!!");
}
System.out.println(">"+data);
//WebSockets.sendText(message.getData(), channel, null);
}
});
channel.resumeReceives();
}
}))
.addPrefixPath("/", resource(new ClassPathResourceManager(App.class.getClassLoader(), App.class.getPackage())).addWelcomeFiles("index.html").setDirectoryListingEnabled(true)))
.build();
server.start();
}
@Override
public void run() {
while (true) {
try {
Thread.sleep(2000);
} catch (InterruptedException e) {
e.printStackTrace();
}
//Broadcast to all
for (WebSocketChannel c : connected) {
WebSockets.sendText("Timer : " + System.currentTimeMillis(), c, null);
}
}
}
}
