#!/usr/bin/env python3

import os
import sys
import socket
import time
from scgi.scgi_server import SCGIServer, SCGIHandler

SOCK = '/home/alan/bin/apogee/src/test/run/scgi_test.sock'
if os.path.exists(SOCK):
    os.remove(SOCK)

class ExampleHandler(SCGIHandler):
    
    def produce(self, env, bodysize, input, output):
        print('"produce" method of ExampleHandler called with the following environment:')
        print(env)
        print("body is:")
        print(input.read(bodysize))
        path = env['PATH_INFO']
        if not path:
            # No path: Write env
            output.write(b'20 text/plain\r\n')
            output.write(str(env).encode())
        elif path == '/test_redirect':
            output.write(b'31 /redirect/to\r\n')
        elif path == '/test_cgi_error':
            output.write(b'42 Testing SCGI error\r\n')
        elif path == '/test_need_cert':
            output.write(b'60 SCGI says you need a cert\r\n')
        elif path == '/test_bad_cert':
            output.write(b'61 SCGI says bad cert\r\n')
        elif path == '/test_server_error':
            output.write(b'51 SCGI says error\r\n')
        elif path == '/test_actual_scgi_error_1':
            output.write(b'egsfea3d\r\n')
        elif path == '/test_actual_scgi_error_2':
            output.write(b'\r\n')
        elif path == '/test_actual_scgi_error_3':
            print("I am called")
            output.write(b'')
        elif path == '/test_sleep_5':
            time.sleep(5)
            output.write(b'20 text/plain\r\nslept 5\n')
        elif path == '/test_sleep_15':
            time.sleep(15)
            output.write(b'20 text/plain\r\nslept 15\n')
        elif path == '/client_auth':
            fp = env["TLS_CLIENT_HASH"]
            output.write(b'20 text/plain\r\n' + fp.encode() + b'\n')
        else:
            output.write(b'20 text/plain\r\nsome other path received\n')


s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
s.bind(SOCK)

server = SCGIServer(handler_class=ExampleHandler)
server.serve_on_socket(s)

