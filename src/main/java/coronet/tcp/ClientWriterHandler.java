package coronet.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.PrintWriter;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.interrupted;

public class ClientWriterHandler implements Runnable
{

    StandardTCPClient client;
    LinkedBlockingQueue<String>  sendQueue;
    PrintWriter outputStream;


    private static final Logger Log = LoggerFactory.getLogger(ClientWriterHandler.class);

    public ClientWriterHandler(StandardTCPClient client,
                               LinkedBlockingQueue<String> sendQueue, PrintWriter outputStream)
    {
        this.client = client;
        this.sendQueue = sendQueue;
        this.outputStream = outputStream;
    }

    public void run()
    {
        try
        {
            while (!interrupted() && client.isRunning())
                outputStream.printf(sendQueue.take() + "\n");
        }
        catch (InterruptedException ex)
        {
            Log.info("sending thread interrupted.");
            Thread.currentThread().interrupt();
        }
        catch(Exception ex)
        {
            Log.error("Unexpected exception in sending thread:", ex);
        }
        client.setRunning(false);
        synchronized (client)
        {
            client.notifyAll();
        }
    }


}
