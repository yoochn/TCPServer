import coronet.tcp.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TCPServerConfig.class)
public class TCPClientTest
{
    @Autowired
    private  TCPServer server;

    @Autowired
    private StandardTCPClient client;


    @Before
    public void beforeTestClass() throws Exception
    {
        Thread serverThread = new Thread(new Runnable()
        {
                @Override
                public void run() {
                    try {
                        server.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            Thread clientThread = new Thread(client);
            serverThread.start();

            clientThread.start();

    }

    @Test
    public void testClientgetAllKeys() throws IOException, InterruptedException {
        client.leftAdd("yosef","hello server test 1");
        client.leftAdd("yoav","hello server test 2");
        TCPResponse<ArrayList<String>> message = client.getAllKeys("yo");
        Assert.assertEquals(message.getResponseCode(),TCPResponse.status.SUCCESS);
        Assert.assertNotNull( message.getResult());
        int size = message.getResult().size() -1;
        Assert.assertTrue(message.getResult().size() == 2);
        Assert.assertTrue(message.getResult().get(0).equals("yosef"));

    }

    @Test
    public void testClientSet() throws IOException
    {
        TCPResponse message = client.rightAdd("yosef","hello server test 1");
        message = client.leftAdd("yosef","hello server test 2");
        TCPResponse<LinkedHashMap<String,String>> response = client.set("yosef", Arrays.asList("message 0", "message 1", "message 3"));
        Assert.assertEquals(response.getResponseCode(),TCPResponse.status.SUCCESS);
        Assert.assertNotNull( response.getResult());
        int size = response.getResult().size() -1;
        Assert.assertTrue(response.getResult().size() == 2);
        Assert.assertTrue(response.getResult().get("0").equals("hello server test 1"));
        Assert.assertTrue(response.getResult().get("-1").equals("hello server test 2"));
    }

    @Test
    public void testClientleftAdd() throws IOException
    {
        TCPResponse<LinkedHashMap<String,String>>  message = client.leftAdd("yos","hello server test 1");
        Assert.assertNotNull(message);
        Assert.assertEquals(message.getResponseCode(),TCPResponse.status.SUCCESS);
        Assert.assertTrue(message.getResult().size() > 0);
        Assert.assertTrue(message.getResult().get("0").contains("hello server"));
    }

    @Test
    public void testClientrightAdd() throws IOException
    {
        TCPResponse<LinkedHashMap<String,String>>  message = client.rightAdd("yosef","hello server test 2");
        Assert.assertEquals(message.getResponseCode(),TCPResponse.status.SUCCESS);
        int size = message.getResult().size() -1;
        Assert.assertTrue(message.getResult().get("0").contains("hello server"));
    }


    @Test
    public void testClientgetByKey() throws IOException
    {
        TCPResponse message = client.leftAdd("yoav","hello server test 1");
        message = client.leftAdd("yoav","hello server test 1");
        TCPResponse<LinkedHashMap<String,String>>  response = client.get("yoav");
        Assert.assertEquals(response.getResponseCode(),TCPResponse.status.SUCCESS);
        Assert.assertNotNull( response.getResult());
        Assert.assertTrue(response.getResult().size() == 2);
        Assert.assertTrue(response.getResult().get("0").contains("hello server test"));
    }

    @Test
    public void testClientOrder() throws IOException
    {
        TCPResponse message = client.leftAdd("yoav","hello server test left 1");
        message = client.leftAdd("yoav","hello server test left 2");
        message = client.rightAdd("yoav","hello server test right 1");
        TCPResponse<LinkedHashMap<String,String>> response = client.get("yoav");
        Assert.assertEquals(response.getResponseCode(),TCPResponse.status.SUCCESS);
        Assert.assertNotNull( response.getResult());
        int size = response.getResult().size() -1;
        Assert.assertTrue(response.getResult().get("0").contains("hello server test"));
    }

    @After
    public void tearDown() throws IOException
    {
        client.setRunning(false);
        server.stop();
    }

}
