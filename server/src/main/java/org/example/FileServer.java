package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileServer {
    private static final String SERVER_DIR = "received_files";
    private static final int SERVER_PORT = 12345;

    // Helper function to send a UTF string using 4-byte (int) length prefix
    private static void sendUTF(DataOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes("UTF-8");
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    // Helper function to receive a UTF string using 4-byte (int) length prefix
    private static String receiveUTF(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, "UTF-8");
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("[*] Listening on localhost:" + SERVER_PORT);
            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                     DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

                    System.out.println("[+] Client connected: " + clientSocket.getRemoteSocketAddress());

                    while (true) {
                        try {
                            String command = receiveUTF(in);

                            if ("UPLOAD".equals(command)) {
                                int filenameLength = in.readInt();
                                byte[] filenameBytes = new byte[filenameLength];
                                in.readFully(filenameBytes);
                                String filename = new String(filenameBytes);

                                int fileContentLength = in.readInt();
                                byte[] fileContent = new byte[fileContentLength];
                                in.readFully(fileContent);

                                Path filePath = Paths.get(SERVER_DIR, filename);
                                Files.write(filePath, fileContent);

                                sendUTF(out, "File uploaded successfully!");
                                System.out.println("[*] File received: " + filename);
                            }
                        } catch (EOFException e) {
                            System.out.println("[-] Client disconnected");
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
