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
        // Check if the directory exists, if not create it
        Path serverDirPath = Paths.get(SERVER_DIR);
        try {
            if (!Files.exists(serverDirPath)) {
                Files.createDirectory(serverDirPath);
                System.out.println("[*] Directory '" + SERVER_DIR + "' created.");
            }
        } catch (IOException e) {
            System.err.println("[-] Failed to create directory: " + SERVER_DIR);
            e.printStackTrace();
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("[*] Listening on localhost:" + SERVER_PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // For every client, spawn a new thread
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
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
                        } else if ("DOWNLOAD".equals(command)) {
                            String filename = receiveUTF(in);
                            Path filePath = Paths.get(SERVER_DIR, filename);
                            if (Files.exists(filePath)) {
                                byte[] fileContent = Files.readAllBytes(filePath);
                                out.writeInt(fileContent.length);
                                out.write(fileContent);
                                System.out.println("[*] File sent: " + filename);
                            } else {
                                sendUTF(out, "Error: File not found!");
                            }
                        } else if ("LIST".equals(command)) {
                            File[] files = new File(SERVER_DIR).listFiles();
                            StringBuilder fileList = new StringBuilder();
                            if (files != null) {
                                for (File file : files) {
                                    if (file.isFile()) {
                                        fileList.append(file.getName()).append("\n");
                                    }
                                }
                            }
                            sendUTF(out, fileList.toString());
                        }
                    } catch (EOFException e) {
                        System.out.println("[-] Client disconnected");
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
