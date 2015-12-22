package nio;

import nio.headers.Header;
import nio.headers.TwoByteHeader;
import nio.model.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by user on 04.12.2015.
 */
public class Server implements Runnable {
    Selector selector;
    ServerSocketChannel serverSocketChannel;
    SelectionKey serverKey;
    DAOMessage daoMessage;
    Header header = new TwoByteHeader();
    private volatile int clientCounter;
    private final int port;
    private ExecutorService executorService;


    public Server(int port) {
        this.executorService = Executors.newFixedThreadPool(4);
        this.port = port;
        this.daoMessage = new DAOMessage();
        for (Message message : daoMessage.getAllMessages()) {
            System.out.println(message.toString());
        }
    }

    public static void main(String[] args) throws Exception {
        new Thread(new Server(6666)).start();
    }

    @Override
    public void run() {
        try {
            this.selector = Selector.open();
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.configureBlocking(false);
            this.serverKey = serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
            this.serverSocketChannel.bind(new InetSocketAddress("localhost", this.port));

            while (this.selector.select() > 0) {
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    try {
                        keys.remove();
                        if (key.isAcceptable()) {
                            acceptKey(key);
                        }
                        if (key.isReadable()) {
                            readKey(key);
                        }
                    } catch (IOException e) {
                        key.cancel();
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException("Server failed " + e.getMessage());
        } finally {
            try {
                this.selector.close();
                this.serverSocketChannel.socket().close();
                this.serverSocketChannel.close();
            } catch (IOException e) {
                //nop
            }
        }
    }

    private void readKey(SelectionKey key) throws IOException {
        //new Thread(() -> {
        SocketChannel clientChannel = (SocketChannel) key.channel();
//        if (key.attachment() == null) {
//            key.attach(new KeyIdentity(ByteBuffer.allocate(512), "Main room"));
//        }

        ByteBuffer currentBuffer = ((KeyIdentity) key.attachment()).getBuffer();

        try {
            clientChannel.read(currentBuffer);
        } catch (IOException e) {
            System.out.println("Client disconnected. Clients left: " + (--clientCounter));
            key.cancel();
            clientChannel.close();
            return;
        }
        currentBuffer.flip();
        parseBuffer(key);
    }

    private void parseBuffer(SelectionKey key) throws IOException {

        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer currentBuffer = ((KeyIdentity) key.attachment()).getBuffer();
        //check, is bytes enough to read header
        if (currentBuffer.remaining() > header.byteLength()) {
            byte[] header = new byte[this.header.byteLength()];
            currentBuffer.get(header);
            int bytesToRead = (int) this.header.bytesToLength(header);
            if ((currentBuffer.limit() - currentBuffer.position()) < bytesToRead) {
                //not enough data
                if (currentBuffer.limit() == currentBuffer.capacity()) {
                    //message can be too large, need to resize buffer
                    int oldCapacity = currentBuffer.capacity();
                    ByteBuffer temp = ByteBuffer.allocate(this.header.byteLength() + bytesToRead);
                    currentBuffer.position(0);
                    temp.put(currentBuffer);
                    currentBuffer = temp;
                    currentBuffer.position(oldCapacity);
                    currentBuffer.limit(currentBuffer.capacity());
                    return;
                } else {
                    //rest for writing
                    currentBuffer.position(currentBuffer.limit());
                    currentBuffer.limit(currentBuffer.capacity());
                    return;
                }
            }
            //if all OK, get result message
            byte[] result = new byte[bytesToRead];
            currentBuffer.get(result, 0, bytesToRead);
            int remaining = currentBuffer.remaining();
            currentBuffer.limit(currentBuffer.capacity());
            currentBuffer.compact();
            currentBuffer.position(0);
            currentBuffer.limit(remaining);
            String message = new String(result);
            System.out.println("from " + clientChannel.getRemoteAddress() + " :[" + message + "]");
            handleMessage(message, key);
            parseBuffer(key);
        } else {
            currentBuffer.position(currentBuffer.limit());
            currentBuffer.limit(currentBuffer.capacity());
        }
    }

    private void handleMessage(String message, SelectionKey key) {
        executorService.submit(() -> {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            if (message.matches("(^/snd .{1,150}$)")) {
                if (((KeyIdentity) key.attachment()).getNickName() == null) {
                    writeToClient("you should register first!", clientChannel);
                } else {
                    String dataMessage = message.replace("/snd ", "");
                    this.daoMessage.persist(new Message(dataMessage,
                            ((KeyIdentity) key.attachment()).getNickName(),
                            new Date(),
                            (((KeyIdentity) key.attachment()).getRoomName())));
                    sendToAll(key, "[" + ((KeyIdentity) key.attachment()).getRoomName() + "] " + ((KeyIdentity) key.attachment()).getNickName() + ": " + dataMessage);
                }
            } else if (message.matches("(^/chid [\\S]{1,17}$)")) {
                String nickName = message.replace("/chid ", "");
                if (!isNickNamePersist(nickName)) {
                    ((KeyIdentity) key.attachment()).setNickName(nickName);
                    writeToClient("You are registered, " + nickName + ".", clientChannel);
                } else {
                    writeToClient("This nick name already in use.", clientChannel);
                }
            } else if (message.matches("(^/hist$)")) {
                List<Message> messages = this.daoMessage.getAllMessages();
                for (Message messageFromDB : messages) {
                    writeToClient(messageFromDB.toString(), clientChannel);
                }
            } else if (message.matches("(^/chroom [\\S]{1,17}$)")) {
                if (((KeyIdentity) key.attachment()).getNickName() == null) {
                    writeToClient("You should register first!", clientChannel);
                } else {
                    String roomName = message.replace("/chroom ", "");
                    ((KeyIdentity) key.attachment()).setRoomName(roomName);
                    writeToClient("Room changed successfully.", clientChannel);
                }
            } else if (message.matches("(^/help$)")) {
                writeToClient("/chid <nickname> - register in chat\n" +
                        "/snd <message to send> - send message to other users\n" +
                        "/hist - view history of all messages\n" +
                        "/chroom - change talking room", clientChannel);
            } else {
                writeToClient("wrong command! Type /help to see available commands", clientChannel);
            }
        });

    }

    private synchronized void writeToClient(String response, SocketChannel clientChannel) {
        byte[] header = this.header.lengthToBytes(response.getBytes().length);
        try {
            clientChannel.write(ByteBuffer.wrap(header));
            clientChannel.write(ByteBuffer.wrap(response.getBytes()));
            //System.out.printf("send [%s] to %s\n", response, clientChannel.getRemoteAddress());
        } catch (IOException e) {
            System.out.println("Connection refused");
        }
    }

    private void sendToAll(SelectionKey key, String message) {
        String roomName = ((KeyIdentity) key.attachment()).getRoomName();
        Iterator<SelectionKey> toAllKeys = this.selector.keys().iterator();
        while (toAllKeys.hasNext()) {
            SelectionKey allKey = toAllKeys.next();
            if (allKey != this.serverKey  && allKey.attachment() != null && ((KeyIdentity) allKey.attachment()).getRoomName().equals(roomName)) {
                SocketChannel clientAll = (SocketChannel) allKey.channel();
                writeToClient(message, clientAll);
            }
        }
    }

    private boolean isNickNamePersist(String nickName) {
        Iterator<SelectionKey> allKeys = this.selector.keys().iterator();
        while (allKeys.hasNext()) {
            SelectionKey allKey = allKeys.next();
            if (allKey.attachment() == null)
                continue;
            String currentKeyNickName = ((KeyIdentity) allKey.attachment()).getNickName();
            if (currentKeyNickName != null && currentKeyNickName.equals(nickName)) {
                return true;
            }
        }
        return false;
    }

    private void acceptKey(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = (SocketChannel) server.accept();
        client.configureBlocking(false);
        SelectionKey readKey = client.register(this.selector, SelectionKey.OP_READ);
        readKey.attach(new KeyIdentity(ByteBuffer.allocate(512), "Main room"));
        writeToClient("Welcome to chat!",client);
        System.out.println("New client connected: " + client.getRemoteAddress() + ". Total clients: " + ++clientCounter);
    }
}
