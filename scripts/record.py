import websocket
import json
import signal
from pathlib import Path
import sys

data = []
first = 1

def on_close(signum, frame):
    path = "data/" + sys.argv[1]
    with open(Path(__file__).parent / path, "w") as myfile:
        myfile.write(json.dumps(data))
        myfile.close()
    exit()

def on_message(wsapp, message):
    position = json.loads(message)
    data.append(position)
    print(data)


signal.signal(signal.SIGINT, on_close)
wsapp = websocket.WebSocketApp("ws://147.229.193.119:8000", on_message=on_message)
wsapp.run_forever()