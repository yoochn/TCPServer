package coronet.tcp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;;import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


@Configuration
@ComponentScan
public class TCPServerConfig 
{
       @Bean//(initMethod = "start" , destroyMethod = "stop")
       public TCPServer tcpServer()
      {
          return  new TCPServer(5010,getServerDataFile());
      }

        @Bean//(initMethod = "startConnection" , destroyMethod = "stop")
        public TCPClient tcpClient()
        {
            return  new TCPClient("127.0.0.1", 5010);
        }
        @Bean(initMethod = "init")
        public TCPServerDataFile getServerDataFile()
        {
            return  new TCPServerDataFile("C:\\coronet","coronetJson.txt");
        }


    @Bean//(initMethod = "startConnection" , destroyMethod = "stop")
    public StandardTCPClient tcpStandardClient()
    {
        return  new StandardTCPClient("127.0.0.1", 5010);
    }


}
