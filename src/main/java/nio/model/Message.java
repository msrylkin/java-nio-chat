package nio.model;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by user on 13.12.2015.
 */
@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    private int id;
    @Column (name = "message")
    private String message;
    @Column (name = "username")
    private String username;
    @Column (name = "date")
    private Date date;
    @Column (name = "room_name")
    private String roomName;

    public Message(String message, String username, Date date, String roomName) {
        this.message = message;
        this.username = username;
        this.date = date;
        this.roomName = roomName;
    }

    @Override
    public String toString() {
        return "Message='" + message + '\'' +
                ", nick name='" + username + '\'' +
                ", room name='" + roomName + '\'' +
                ", date=" + date;

    }

    public Message() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
