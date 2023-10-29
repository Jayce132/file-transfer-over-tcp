import socket
import os

SERVER_HOST = '127.0.0.1'
SERVER_PORT = 12345


def send_utf_string(socket, text):
    # Convert the string to bytes to get its length in bytes.
    encoded_text = text.encode('utf-8')
    # Sending the length first ensures the receiver knows exactly how many bytes to read next.
    socket.send(len(encoded_text).to_bytes(4, byteorder='big'))
    socket.send(encoded_text)


def receive_utf_string(socket):
    # Receive the 4-byte (int) length of the string first to know how many bytes to read next.
    length_bytes = socket.recv(4)
    length = int.from_bytes(length_bytes, byteorder='big')
    # Read the actual number of bytes specified and decode.
    return socket.recv(length).decode('utf-8')


client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect((SERVER_HOST, SERVER_PORT))

while True:
    print("\nCommand: UPLOAD")
    command = input("Enter command: ").upper()

    if command == "UPLOAD":
        filename = input("Enter the full path of the file you want to send: ")

        if not os.path.exists(filename):
            print("File does not exist!")
            continue

        with open(filename, "rb") as f:
            file_content = f.read()

        basename = os.path.basename(filename)

        send_utf_string(client_socket, command)
        client_socket.send(len(basename).to_bytes(4, byteorder='big'))
        client_socket.send(basename.encode())
        client_socket.send(len(file_content).to_bytes(4, byteorder='big'))
        client_socket.sendall(file_content)

        # Obtain confirmation from the server.
        confirmation = receive_utf_string(client_socket)
        print(confirmation)
