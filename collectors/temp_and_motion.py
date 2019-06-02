# -*- coding: utf-8 -*-

# Import Libraries
import time
import http.server
import urllib
from urllib.parse import urlparse, parse_qs

HOST_NAME = '192.168.0.11'  # !!!REMEMBER TO CHANGE THIS!!!
PORT_NUMBER = 9999  # Maybe set this to 9000.


class MyHandler(http.server.BaseHTTPRequestHandler):

    # def do_head(self):
    #     self.send_response(200)
    #     self.send_header("Content-type", "application/json")
    #     self.send_header("Access-Control-Allow-Origin", "*")
    #     self.end_headers()

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])  # <--- Gets the size of data
        post_data = self.rfile.read(content_length)  # <--- Gets the data itself
        #print("POST request,\nPath: %s\nHeaders:\n%s\n\nBody:\n%s\n",
        #             str(self.path), str(self.headers), post_data.decode('utf-8'))
        args_dict = urllib.parse.parse_qs(post_data.decode('utf-8'))

        print(args_dict)

        self.send_header('Content-type', 'text/html')
        self.end_headers()

        # Send message back to client
        message = "ok"
        # Write content as utf-8 data
        self.wfile.write(bytes(message, "utf8"))
        self.send_response(200)
        return


if __name__ == '__main__':
    server_class = http.server.HTTPServer
    httpd = server_class((HOST_NAME, PORT_NUMBER), MyHandler)
    print(time.asctime(), "Server Starts - %s:%s" % (HOST_NAME, PORT_NUMBER))
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
    print(time.asctime(), "Server Stops - %s:%s" % (HOST_NAME, PORT_NUMBER))

