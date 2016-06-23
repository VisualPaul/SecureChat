package ru.hse.securechat.client;

import ru.hse.securechat.*;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HashMap;

public class ServerInteractor extends Thread {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private int lastMessageCode;

    public ServerInteractor(Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        lastMessageCode = 0;
    }

    public Socket getSocket() {
        return socket;
    }
    private final HashMap<Integer, AddUserRequest> addUserRequests = new HashMap<>();
    private final HashMap<Integer, UserMessageSendRequest> messageSendRequests = new HashMap<>();

    @Override
    public void run() {
        try {
            for (; ; ) {
                int code = in.read();
                if (code == -1)
                    break;
                if (code == UserMessage.CODE) {
                    ClientMain.displayMessage((UserMessage) in.readObject());
                } else if (code == AddUserResponse.CODE) {
                    AddUserResponse resp = (AddUserResponse) in.readObject();
                    AddUserRequest request;
                    synchronized (addUserRequests) {
                        request = addUserRequests.remove(resp.getCode());
                    }
                    ClientMain.addUserResponse(request, resp);
                } else if (code == UserMessageSendResponse.CODE) {
                    UserMessageSendResponse resp = (UserMessageSendResponse)in.readObject();
                    UserMessageSendRequest req;
                    synchronized (messageSendRequests) {
                        req = messageSendRequests.remove(resp.getCode());
                    }
                    ClientMain.messageResponse(req, resp);
                } else {
                    throw new IOException(String.format("unknown code %d", code));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("i/o error:");
            e.printStackTrace();
            System.exit(1);
        } catch (SQLException e) {
            System.err.println("sql exception:");
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
    public AddUserRequest addUser(String name) throws IOException {
        int messageCode = lastMessageCode++;
        AddUserRequest request = new AddUserRequest(messageCode, name);
        synchronized (addUserRequests) {
            out.write(AddUserRequest.CODE);
            out.writeObject(request);
            addUserRequests.put(messageCode, request);
        }
        return request;
    }

    public UserMessageSendRequest sendMessage(User user, String messageString) throws IOException {
        byte[] enc;
        try {
            Cipher cipher = Cipher.getInstance(ProtocolConstants.ALGORITHM_FULL);
            cipher.init(Cipher.ENCRYPT_MODE, user.getPublicKey());
            enc = cipher.doFinal(messageString.getBytes("UTF-8"));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }

        UserMessageSendRequest message = new UserMessageSendRequest(lastMessageCode++,
                user.getName(), enc);
        synchronized (messageSendRequests) {
            out.write(UserMessage.CODE);
            out.writeObject(message);
            messageSendRequests.put(message.getCode(), message);
        }
        return message;
    }
}
