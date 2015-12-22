package nio.model;

import java.nio.ByteBuffer;

/**
 * Created by user on 15.12.2015.
 */
public class KeyIdentity {
    private String nickName;
    private ByteBuffer buffer;
    private String roomName;

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public KeyIdentity(ByteBuffer buffer, String roomName) {
        this.buffer = buffer;
        this.roomName = roomName;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getNickName() {
        return nickName;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public KeyIdentity(ByteBuffer buffer) {

        //this.nickName = nickName;
        this.buffer = buffer;
    }
}
