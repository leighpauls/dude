import BaseHTTPServer
import cgi

HOST_NAME = ''
PORT_NUMBER = 8080

class DudeServerHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_POST(self):
        print "post received"
        if self.path != "/send_dude":
            self.send_response(404)
            self.end_headers()
            self.wfile.write("unknown service requested\n")
            return
        print "expanding forms"

        form = cgi.FieldStorage(
            fp=self.rfile,
            headers=self.headers,
            environ={
                'REQUEST_METHOD': 'POST',
                'CONTENT_TYPE': self.headers['Content-Type']})

        print "examining forms"
        if 'dude_sound' not in form:
            self.send_response(400)
            self.end_headers()
            self.wfile.write("no \"dude_sound\" field found\n")
            return

        print "verifying form type"
        if not form['dude_sound'].file:
            self.send_response(400)
            self.end_headers()
            self.wfile.write("dude_sound was not a file\n")
            return

        print "starting upload..."
        # save the uploaded file
        with open("/tmp/dude_sound.mp4", 'w+b') as f:
            f.write(form['dude_sound'].file.read())
            self.wfile.write("\n")
            print "saved file"

        self.send_response(200)
        self.end_headers()
        self.wfile.write("upload successful\n")

if __name__=='__main__':
    httpd = BaseHTTPServer.HTTPServer(
        (HOST_NAME, PORT_NUMBER),
        DudeServerHandler)
    print "Server starting..."
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
    print "Server closed..."
