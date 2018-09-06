package coronet.tcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import static java.lang.Thread.interrupted;

public class ClientReaderHandler implements Runnable
{
    StandardTCPClient client;
    LinkedBlockingQueue<String> receivedQueue;
    BufferedReader inputStream;


    private static final Logger Log = LoggerFactory.getLogger(ClientReaderHandler.class);

    public ClientReaderHandler(StandardTCPClient client,
                               LinkedBlockingQueue<String> receivedQueue, BufferedReader inputStream)
    {
        this.client = client;
        this.receivedQueue = receivedQueue;
        this.inputStream = inputStream;
    }

    public void run()
    {
        try
        {
            while (!interrupted() && client.isRunning())
                receivedQueue.put(inputStream.readLine());
        }
        catch (java.io.IOException ex)
        {
            Log.warn( "IO Exception: ", ex);
        }
        catch (InterruptedException ex)
        {
            Log.info("Receiving thread interrupted.");
            Thread.currentThread().interrupt();
        }
        catch(Exception ex)
        {
            Log.error("Unexpected exception in receiving thread:", ex);
        }
        client.setRunning(false);
        synchronized (client)
        {
            client.notifyAll();
        }
    }
}
