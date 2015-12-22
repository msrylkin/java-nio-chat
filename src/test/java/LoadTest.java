import nio.headers.TwoByteHeader;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by user on 17.12.2015.
 */
public class LoadTest {
    private static final int THREADS_COUNT = 0;
    private static final String HISTORY_MESSAGE = "/hist";
    private static final String SEND_MESSAGE = "/snd";
    private static final String REGISTER_MESSAGE = "/chid";


    @Test
    public void highLoadTest() throws Exception {

        TwoByteHeader twoByteHeader = new TwoByteHeader();
        ExecutorService executorService = Executors.newCachedThreadPool();


        for (int i = 0; i < THREADS_COUNT; i++) {
            Thread.sleep(50);
            executorService.submit(() -> {
                try {
                    //get connect with server
                    SocketChannel channel = SocketChannel.open(new InetSocketAddress("localhost", 6666));
                    channel.configureBlocking(false);
                    ByteBuffer buffer = ByteBuffer.allocate(512);

                    //wait init all threads
                    synchronized (this) {
                        wait();
                    }

                    //start writing
                    String registerMsgWithThreadName = REGISTER_MESSAGE +" "+ Thread.currentThread().getName();
                    channel.write(ByteBuffer.wrap(twoByteHeader.lengthToBytes(registerMsgWithThreadName.getBytes().length)));
                    channel.write(ByteBuffer.wrap(registerMsgWithThreadName.getBytes()));

                    while (channel.read(buffer) > 0){
                        buffer.clear();
                    }
                    Thread.sleep(1000);

                    String helloFromThreadMessage = SEND_MESSAGE + " hello from " + Thread.currentThread().getName();
                    channel.write(ByteBuffer.wrap(twoByteHeader.lengthToBytes(helloFromThreadMessage.getBytes().length)));
                    channel.write(ByteBuffer.wrap(helloFromThreadMessage.getBytes()));
                    while (channel.read(buffer) > 0){
                        buffer.clear();
                    }
                    Thread.sleep(2100);

                    channel.write(ByteBuffer.wrap(twoByteHeader.lengthToBytes(HISTORY_MESSAGE.getBytes().length)));
                    channel.write(ByteBuffer.wrap(HISTORY_MESSAGE.getBytes()));
                    while (channel.read(buffer) > 0){
                        buffer.clear();
                    }
                    Thread.sleep(380);

                    channel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        }

        Thread.sleep(5000);
        //thread.join();
        for (int i = 0; i < THREADS_COUNT; i++){
            Thread.sleep(100);
            synchronized (this) {
                notify();
            }
        }
        synchronized (this) {
            wait();
        }
    }


}
