package aceshooting;

import com.pi4j.io.gpio.*;
import io.undertow.Undertow;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.util.ArrayList;

import static io.undertow.Handlers.*;

/**
 * Created by duke on 7/19/14.
 */
public class App implements Runnable {

    private static final ArrayList<WebSocketChannel> connected = new ArrayList<WebSocketChannel>();

    private static void motorStep(int power, int time, GpioPinDigitalOutput[] pins){
        if(time>0){
            pins[0].high();
            pins[1].high();
            pins[2].low();
            try {
                Thread.sleep(time);
            }
            catch (Exception ex){
            }
        }
        else{
            time =-time;
            pins[0].high();
            pins[1].low();
            pins[2].high();
            try {
                Thread.sleep(time);
            }
            catch (Exception ex){

            }
        }
        pins[0].low();
        pins[1].low();
        pins[2].low();

    }

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
        // create gpio controller
        final GpioController gpio = GpioFactory.getInstance();

        // provision gpio pins #00 to #03 as output pins and ensure in LOW state
        final GpioPinDigitalOutput[] pins = {
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, PinState.LOW),
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW),
                gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, PinState.LOW),
                // gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, PinState.LOW)
        };

        // this will ensure that the motor is stopped when the program terminates
        gpio.setShutdownOptions(true, PinState.LOW, pins);




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
                                            System.out.println("Start clicked !!!");
                                            System.out.println("   Motor FORWARD for 2038 steps.");
                                            motorStep(255, 2038,pins);
                                            System.out.println("   Motor STOPPED for 2 seconds.");
                                            try {
                                                Thread.sleep(2000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        if(data.trim().equals("stop")){
                                            System.out.println("Stop clicked !!!");

                                            System.out.println("   Motor REVERSE for 2038 steps.");
                                            motorStep(255, -2038,pins);
                                            System.out.println("   Motor STOPPED for 2 seconds.");
                                            try {
                                                Thread.sleep(2000);
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
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
                // WebSockets.sendText("Timer : " + System.currentTimeMillis(), c, null);
            }
        }
    }
}
