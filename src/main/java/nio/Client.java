package nio;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import nio.headers.Header;
import nio.headers.TwoByteHeader;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by user on 06.12.2015.
 */
public class Client extends Application  {
    public SocketChannel channel;
    ByteBuffer byteBuffer;
    Header header;

    TextArea textArea;
    private TextField textField;
    private Scene scene;
    private BorderPane borderPane;




    public Client() {

    }

    @Override
    public void init() throws Exception {


        this.textArea = new TextArea();
        this.textField = new TextField();
        this.borderPane = new BorderPane();
        this.scene = new Scene(this.borderPane, 700, 450);

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Task<Void> asd = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (true){
                    if (channel.read(byteBuffer) > 0){
                        byteBuffer.flip();
                        List<String> messages = new LinkedList<>();
                        String message;
                        while ((message=handleData(byteBuffer, header))!=null){
                            messages.add(message);
                        }
                        Platform.runLater(() -> {
                            for (String msg : messages){
                                textArea.appendText(msg+"\n");
                            }
                        });
                    }
                }
            }
        };

        Thread th = new Thread(asd);
        th.setDaemon(true);

        textField.setOnAction(e -> {
            try {
                writeToClient(textField.getText(), this.channel);
            } catch (IOException e1) {
                textArea.appendText("ERROR: server connect failure" + System.lineSeparator());
            }
            textField.clear();
        });
        textField.setPromptText("type your message...");


        textArea.setEditable(false);
        textArea.setStyle("-fx-border-style: none");
        textArea.setFocusTraversable(false);


        borderPane.setCenter(textArea);
        borderPane.setBottom(textField);

        primaryStage.setTitle("CLI chat");
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();

        try {
            this.channel = SocketChannel.open(new InetSocketAddress("localhost", 6666));
            this.channel.configureBlocking(false);
            this.byteBuffer = ByteBuffer.allocate(1024);
            this.header = new TwoByteHeader();
        } catch (Exception e) {
            displayAlertBox("ERROR", "Failed connect to server!");
            primaryStage.close();
        }

        th.start();
    }



    public static void main(String[] args) throws IOException {
        launch(args);
    }

    private void writeToClient(String response, SocketChannel clientChannel) throws IOException {
        byte[] header = this.header.lengthToBytes(response.getBytes().length);
        clientChannel.write(ByteBuffer.wrap(header));
        clientChannel.write(ByteBuffer.wrap(response.getBytes()));
    }


    public void displayAlertBox(String title, String message) {
        Stage window = new Stage();

        //Block events to other windows
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle(title);
        window.setMinWidth(250);

        Label label = new Label();
        label.setText(message);
        Button closeButton = new Button("Exit");
        closeButton.setOnAction(e -> window.close());

        VBox layout = new VBox(10);
        layout.getChildren().addAll(label, closeButton);
        layout.setAlignment(Pos.CENTER);

        //Display window and wait for it to be closed before returning
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }


    private String handleData(ByteBuffer currentBuffer, Header header) {

        if (currentBuffer.remaining() > header.byteLength()) {
            byte[] headerBytes = new byte[header.byteLength()];
            currentBuffer.get(headerBytes);
            int bytesToRead = (int) header.bytesToLength(headerBytes);
            if ((currentBuffer.limit() - currentBuffer.position()) < bytesToRead) {
                //not enough data
                if (currentBuffer.limit() == currentBuffer.capacity()) {
                    //message can be too large, need to resize buffer
                    int oldCapacity = currentBuffer.capacity();
                    ByteBuffer temp = ByteBuffer.allocate(header.byteLength() + bytesToRead);
                    currentBuffer.position(0);
                    temp.put(currentBuffer);
                    currentBuffer = temp;
                    currentBuffer.position(oldCapacity);
                    currentBuffer.limit(currentBuffer.capacity());
                    return null;
                } else {
                    //rest for writing
                    currentBuffer.position(currentBuffer.limit());
                    currentBuffer.limit(currentBuffer.capacity());
                    return null;
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
            String message = new String(result, 0, result.length);
            //System.out.println(message);
            //this.textArea.appendText(message + System.lineSeparator());
            return message;
            //handleData(currentBuffer, header);
        } else {
            //rest for writing again
            currentBuffer.position(currentBuffer.limit());
            currentBuffer.limit(currentBuffer.capacity());
            //return;
            return null;
        }
    }
}
