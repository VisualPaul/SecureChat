package ru.hse.securechat.server;

import ru.hse.securechat.LoginRequest;
import ru.hse.securechat.Message;
import ru.hse.securechat.ProtocolConstants;
import ru.hse.securechat.RegisterRequest;

import javax.crypto.Cipher;
import java.io.*;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Arrays;

public class UserConnection extends Thread{
    private Socket socket;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    public UserConnection(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        System.out.println("hello!");
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            User user = authenticate();
            if (user == null) {
                System.out.println("returned null!");
            } else {
                System.out.printf("hello, %s\n", user.getName());
            }
        } catch (IOException e) {
            System.out.println("i/o error:");
            e.printStackTrace();
        } catch (UnknownCodeException e) {
            System.out.format("unknown code transmitted: %d\n", e.getCode());
            e.printStackTrace();
        } catch (IncorrectRequest incorrectRequest) {
            System.out.println("incorrect request: ");
            incorrectRequest.printStackTrace();
        } catch (SQLException e) {
            System.out.println("sql error:");
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("socket closing error:");
                e.printStackTrace();
            }
        }
    }

    public synchronized boolean sendMessage(Message message){
        try {
            out.write(Message.CODE);
            out.writeObject(message);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private synchronized User authenticate() throws IOException, IncorrectRequest, SQLException {
        try {
            int code = in.read();
            Users users = Users.getInstance();
            if (code == RegisterRequest.CODE) {
                RegisterRequest req = (RegisterRequest)in.readObject();
                boolean hasSuchUser;
                if (users.hasUser(req.getName())) {
                    out.write(RegisterRequest.ALREADY_EXISTS);
                    out.flush();
                    return null;
                }
                users.addUser(req.getName(), req.getPublicKey());
                out.write(RegisterRequest.OK);
                out.flush();
                return users.getUser(req.getName());
            } else if (code == LoginRequest.CODE) {
                LoginRequest req = (LoginRequest)in.readObject();
                User user = users.getUser(req.getName());
                if (user == null) {
                    out.write(LoginRequest.NO_USER);
                    return null;
                }
                out.write(LoginRequest.OK);
                SecureRandom secureRandom = new SecureRandom();
                byte[] original = new byte[ProtocolConstants.CONFIRMATION_SIZE];
                secureRandom.nextBytes(original);
                Cipher cipher = Cipher.getInstance(ProtocolConstants.ALGORITHM_FULL);
                cipher.init(Cipher.ENCRYPT_MODE, user.getPublicKey());
                byte[] ciphered = cipher.doFinal(original);
                out.writeObject(ciphered);
                out.flush();
                byte[] response = (byte[])in.readObject();
                if (!Arrays.equals(original, response)) {
                    out.write(LoginRequest.FAILED);
                    return null;
                }
                out.write(LoginRequest.OK);
                out.flush();
                return user;
            } else {
                throw new UnknownCodeException(code);
            }
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
}
class IncorrectRequest extends Exception {}

class UnknownCodeException extends IncorrectRequest {
    private int code;
    UnknownCodeException(int code) {
        this.code = code;
    }
    int getCode() {
        return code;
    }
}
