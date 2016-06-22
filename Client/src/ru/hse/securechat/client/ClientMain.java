package ru.hse.securechat.client;

import ru.hse.securechat.LoginRequest;
import ru.hse.securechat.ProtocolConstants;
import ru.hse.securechat.RegisterRequest;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class ClientMain {
    private static LoginUser currentLoginUser = null;
    private static Socket socket;
    private static ObjectInputStream in;
    private static ObjectOutputStream out;
    private static Connection connection;
    public static Socket getSocket() {
        return socket;
    }
    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:client.db");
            connection.setAutoCommit(false);
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
                            default:
                                System.out.println("unknown command");
                        }
                    }
                } catch (CommandException e) {
                    System.out.println("incorrect command");
                }
            }
        }
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
            connection.commit();
            System.out.println("OK!");
        } else if (response == RegisterRequest.ALREADY_EXISTS) {
            System.out.println("LoginUser with this name exists already");
        } else {
            throw new IOException("unexpected stream end or unknown code");
        }
    }

    public static Connection getConnection() {
        return connection;
    }
}
