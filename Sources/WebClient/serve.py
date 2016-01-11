import SimpleHTTPServer
import BaseHTTPServer
import sys

class MyHTTPRequestHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_my_headers()

        SimpleHTTPServer.SimpleHTTPRequestHandler.end_headers(self)

    def send_my_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")


if __name__ == '__main__':
    try:
        port = int(sys.argv[1])
    except:
	port = 8000

    server_address = ('localhost', port)
    httpd = BaseHTTPServer.HTTPServer(server_address, MyHTTPRequestHandler)
    sa = httpd.socket.getsockname()
    print "Serving HTTP on", sa[0], "port", sa[1], "..."
    httpd.serve_forever()
    #SimpleHTTPServer.test(HandlerClass=MyHTTPRequestHandler)
