package ru.hse.securechat.client;

import ru.hse.securechat.*;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class ClientMain {
    private static LoginUser currentLoginUser = null;
    private static Socket socket;
    private static ObjectInputStream in;
    private static ObjectOutputStream out;
    private static ServerInteractor serverInteractor = null;
    public static Socket getSocket() {
        return socket;
    }
    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
            socket = new Socket("localhost", ProtocolConstants.PORT);
            in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            interact();
        } catch (IOException e) {
            System.err.println("i/o error:");
            e.printStackTrace();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("error while closing: ");
                    e.printStackTrace();
                }
            }
        }
    }

    private static void interact() throws IOException, SQLException {
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                try {
                    final String line = scanner.nextLine();
                    if (line.startsWith("\\")) {
                        final String[] commands = line.split("\\s");
                        switch (commands[0]) {
                            case "\\r":
                                register(scanner, commands);
                                break;
                            case "\\l":
                                login(scanner, commands);
                                break;
                            case "\\a":
                                addUser(scanner, commands);
                                break;
                            case "\\q":
                                if (commands.length != 1)
                                    throw new CommandException();
                                System.exit(0);
                                break;
                            case "\\d":
                                deleteUser(scanner, commands);
                                break;
                            case "\\L":
                                deleteLoginUser(scanner, commands);
                                break;
                            case "\\m":
                                messageBegin(scanner, commands, line);
                                break;
                            default:
                                System.out.println("unknown command");
                        }
                    }
                } catch (ConnectionException e) {
                    System.out.println("have not logged in yet");
                } catch (CommandException e) {
                    System.out.println("incorrect command");
                }
            }
        }
    }

    private HashMap<Integer, UserMessageSendRequest> sentMessages = new HashMap<>();

    private static void messageBegin(Scanner scanner, String[] commands, String line)
            throws CommandException, IOException, SQLException {
        requireLogin();
        if (commands.length == 1)
            throw new CommandException();
        String name = commands[1];
        User user = DB.getUser(name);
        if (user == null) {
            System.out.printf("no such user: %s\n", name);
        }
        int firstIndex = line.indexOf(name) + name.length();
        while (Character.isSpaceChar(line.codePointAt(firstIndex))) {
            firstIndex += Character.charCount(line.codePointAt(firstIndex));
            if (firstIndex == line.length()) {
                break;
            }
        }
        String messageString = line.substring(firstIndex);
        if (!messageString.equals("")) {
            serverInteractor.sendMessage(user, messageString);
        } else {
            assert false;
        }
    }

    private static void requireLogin() throws ConnectionException {
        if (currentLoginUser == null)
            throw new ConnectionException();
    }
    private static void deleteLoginUser(Scanner scanner, String[] commands) throws CommandException, SQLException {
        requireLogin();
        if (commands.length != 1)
            throw new CommandException();
        DB.deleteLoginUser(currentLoginUser.getName());
    }

    private static void deleteUser(Scanner scanner, String[] commands) throws CommandException, SQLException {
        String name = getName(scanner, commands);
        if (DB.getUser(name) == null) {
            System.out.printf("no used named %s known\n", name);
        } else if (DB.getLoginUser(name) != null) {
            System.out.println("cannot delete login user\n");
        } else {
            DB.deleteUser(name);
        }
    }

    private static void addUser(Scanner scanner, String[] commands) throws CommandException, IOException, SQLException {
        requireLogin();
        String name = getName(scanner, commands);
        if (DB.getUser(name) != null) {
            System.out.printf("user %s is already known\n", name);
        } else {
            serverInteractor.addUser(name);
        }
    }
    public static void addUserResponse(AddUserRequest request, AddUserResponse response) throws IOException, SQLException {
        if (request == null)
            throw new IOException(String.format("no such request: %d", response.getCode()));
        System.out.printf("added used %s:\nhis public key: %s\nyour public key: %s\nplease, check keys\n",
                request.getUserName(), getKeyDigest(response.getPublicKey()),
                getKeyDigest(currentLoginUser.getKeyPair().getPublic()));
        User user = new User(request.getUserName(), response.getPublicKey());
        DB.storeInDatabase(user);
    }

    private static String getName(Scanner scanner, String[] commands) throws CommandException {
        if (commands.length == 1) {
            System.out.println("enter your name:");
            return scanner.nextLine();
        } else if (commands.length == 2) {
            return commands[1];
        } else {
            throw new CommandException();
        }
    }

    private static void login(Scanner scanner, String[] commands) throws CommandException, IOException, SQLException {
        if (currentLoginUser != null) {
            System.out.println("already logged in");
            return;
        }
        String name = getName(scanner, commands);
        LoginUser user = DB.getLoginUser(name);
        if (user == null) {
            System.err.println("no such username");
            return;
        }
        out.write(LoginRequest.CODE);
        out.writeObject(new LoginRequest(name));
        out.flush();
        int resp = in.read();
        if (resp != LoginRequest.OK) {
            System.err.println("login failed");
            return;
        }
        byte[] to_decrypt;
        try {
             to_decrypt = (byte[]) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        try {
            Cipher cipher = Cipher.getInstance(ProtocolConstants.ALGORITHM_FULL);
            cipher.init(Cipher.DECRYPT_MODE, user.getKeyPair().getPrivate());
            byte[] encrypted = cipher.doFinal(to_decrypt);
            out.writeObject(encrypted);
            out.flush();
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        int resp2 = in.read();
        if (resp2 != LoginRequest.OK) {
            System.err.println("login failed");
            return;
        }
        currentLoginUser = user;
        serverInteractor = new ServerInteractor(socket, in, out);
        serverInteractor.start();
    }

    private static void register(Scanner scanner, String[] commands) throws IOException, SQLException, CommandException {
        if (currentLoginUser != null) {
            System.out.println("already logged in");
            return;
        }
        String name = getName(scanner, commands);
        out.write(RegisterRequest.CODE);
        KeyPair pair;
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance(ProtocolConstants.ALGORITHM);
            pair = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        out.writeObject(new RegisterRequest(pair.getPublic(), name));
        out.flush();
        int response = in.read();
        if (response == RegisterRequest.OK) {
            currentLoginUser = new LoginUser(name, pair);
            DB.storeInDatabase(currentLoginUser);
            System.out.println("OK!");
            serverInteractor = new ServerInteractor(socket, in, out);
            serverInteractor.start();
        } else if (response == RegisterRequest.ALREADY_EXISTS) {
            System.out.println("LoginUser with this name exists already");
        } else {
            throw new IOException("unexpected stream end or unknown code");
        }
    }
    public static void displayMessage(UserMessage message) {
        byte[] dec;
        try {
            Cipher cipher = Cipher.getInstance(ProtocolConstants.ALGORITHM_FULL);
            cipher.init(Cipher.DECRYPT_MODE, currentLoginUser.getKeyPair().getPrivate());
            dec = cipher.doFinal(message.getMessage());
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
        try {
            System.out.printf("new message from %s: %s\n", message.getFrom(), new String(dec, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getKeyDigest(Key key) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] output = md.digest(key.getEncoded());
            StringBuilder builder = new StringBuilder();
            for (byte b : output) {
                builder.append(String.format("%x", Byte.toUnsignedInt(b)));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static LoginUser getCurrentLoginUser() {
        return currentLoginUser;
    }


    public static void messageResponse(UserMessageSendRequest req, UserMessageSendResponse resp) {
        if (resp.getStatus() == UserMessageSendResponse.NO_USER)
            System.out.println("no such user");
        else if (resp.getStatus() == UserMessageSendResponse.USER_OFFLINE)
            System.out.println("user is offline");
        else
            System.out.println("OK, message sent!");
    }
}
