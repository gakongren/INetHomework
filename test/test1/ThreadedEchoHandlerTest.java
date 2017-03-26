package test1;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by kongren on 2017/3/25.
 */
public class ThreadedEchoHandlerTest {
    ServerSocket server;

    @Before
    public void setup() throws IOException {
        server = new ServerSocket(8888);
    }

    @Test
    public void testInput() throws IOException {
        new Thread(() -> {
            PrintWriter out;
            try {
                wait(10);
                Socket soc = new Socket("127.0.0.1", 8888);
                soc.setKeepAlive(true);
                soc.setReuseAddress(true);
                out = new PrintWriter(soc.getOutputStream());
            } catch (IOException|InterruptedException e) {
                return;
            }

            Scanner in = new Scanner(System.in);
            String s;
            int i = 0;
            while (!(s = in.nextLine()).equals("STOP")) {
                out.write(s);
                if (i % 2 == 0)
                    out.flush();
                i++;
            }
        }).start();

        Socket cli = server.accept();
        Scanner CLI_IN = new Scanner(cli.getInputStream());
        new Thread(()->{
            while(true){
                if(System.nanoTime()%1000000==0)
                    System.out.println(CLI_IN.hasNextLine());
            }
        }).start();
    }
}