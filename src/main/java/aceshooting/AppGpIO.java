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
public class AppGpIO implements Runnable {

    private static final ArrayList<WebSocketChannel> connected = new ArrayList<WebSocketChannel>();


    private static int runmotor=2000;
    private static int stopmotor=2000;
    private static boolean startProcess=false;
    private static boolean directionForward = true;

    private static void motorStep() {
        try {
            if (directionForward) {
                pins[0].high();
                pins[1].high();
                pins[2].low();
                System.out.println("[MOTOR]: Running forward " + runmotor + " ms");
                Thread.sleep(runmotor);
            } else {
                pins[0].high();
                pins[1].low();
                pins[2].high();
                System.out.println("[MOTOR]: Running backward " + runmotor + " ms");
                Thread.sleep(runmotor);
            }
            pins[0].low();
            pins[1].low();
            pins[2].low();
            System.out.println("[MOTOR]: Sleeping " + stopmotor + " ms");
            Thread.sleep(stopmotor);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    // create gpio controller
   private static final GpioController gpio = GpioFactory.getInstance();

    // provision gpio pins #00 to #03 as output pins and ensure in LOW state
    private static final GpioPinDigitalOutput[] pins = {
            gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, PinState.LOW),
            gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, PinState.LOW),
            gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, PinState.LOW),
            // gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, PinState.LOW)
    };

    public static void main(String[] args) {
           // this will ensure that the motor is stopped when the program terminates
           gpio.setShutdownOptions(true, PinState.LOW, pins);



           new Thread(new AppGpIO()).start();

        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(path()
                        .addPrefixPath("/camremote", websocket(new WebSocketConnectionCallback() {
                            @Override
                            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                                System.out.println("A client connected");
                                connected.add(channel);
                                channel.getReceiveSetter().set(new AbstractReceiveListener() {
                                                                   @Override
                                                                   protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                                                                       String data = message.getData();
                                                                       System.out.println(">" + data);

                                                                       String vals[] = data.split(",");
                                                                       if (vals[0].equals("r")) {
                                                                           runmotor = Integer.parseInt(vals[1]);
                                                                       } else if (vals[0].equals("s")) {
                                                                           stopmotor = Integer.parseInt(vals[1]);

                                                                       } else if (data.equals("start"))

                                                                       {
                                                                           startProcess = true;
                                                                       } else if (data.equals("stop"))
                                                                       {
                                                                           startProcess = false;
                                                                       } else if (data.equals("forward"))

                                                                       {
                                                                           directionForward = true;
                                                                       } else if (data.equals("backward"))

                                                                       {
                                                                           directionForward = false;
                                                                       }


                                                                       //WebSockets.sendText(runmotor + "," + stopmotor, channel, null);
                                                                   }
                                                               }

                                );
                                channel.resumeReceives();
                            }
                        }))
                        .addPrefixPath("/", resource(new ClassPathResourceManager(AppGpIO.class.getClassLoader(), AppGpIO.class.getPackage())).addWelcomeFiles("index.html").setDirectoryListingEnabled(true)))
                .build();
        server.start();


    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(50);
                if(startProcess){
                    motorStep();
                }
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
