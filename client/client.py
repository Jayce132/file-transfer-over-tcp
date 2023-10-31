import socket
import os

SERVER_HOST = '127.0.0.1'
SERVER_PORT = 12345
RECEIVED_FILES_DIR = "received_files"


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

if not os.path.exists(RECEIVED_FILES_DIR):
    os.makedirs(RECEIVED_FILES_DIR)
    print(f"[*] Directory '{RECEIVED_FILES_DIR}' created.")

while True:
    print("\nCommands: UPLOAD DOWNLOAD")
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

    elif command == "DOWNLOAD":
        # First, request a list of available files from the server
        send_utf_string(client_socket, "LIST")
        file_list = receive_utf_string(client_socket)
        if file_list:
            print("Available files for download:")
            print(file_list)
        else:
            print("No files available for download.")
            continue

        filename = input("Enter the name of the file you want to download: ")

        send_utf_string(client_socket, command)
        send_utf_string(client_socket, filename)

        # Receive the file length, then the file content
        file_length_bytes = client_socket.recv(4)
        file_length = int.from_bytes(file_length_bytes, byteorder='big')

        # Loop to ensure all bytes of the file are received
        chunks = []
        bytes_received = 0
        while bytes_received < file_length:
            chunk = client_socket.recv(
                min(file_length - bytes_received, 4096))  # 4096 is just a buffer size, can be adjusted
            if not chunk:
                break
            chunks.append(chunk)
            bytes_received += len(chunk)
        file_content = b''.join(chunks)

        if file_content.startswith(b"Error:"):
            print(file_content.decode())
        else:
            file_path = os.path.join(RECEIVED_FILES_DIR, filename)
            with open(file_path, "wb") as f:
                f.write(file_content)
            print(f"File '{filename}' downloaded successfully!")
