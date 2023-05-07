import websocket
import json
import signal
from pathlib import Path
import sys

data = []


def on_close(signum, frame):
    path = "resultData/full/" + sys.argv[1] + ".json"
    with open(Path(__file__).parent / path, "w") as myfile:
        myfile.write(json.dumps(data))
        myfile.close()
    exit()


def on_message(wsapp, message):
    position = json.loads(message)
    data.append(position)


signal.signal(signal.SIGINT, on_close)
wsapp = websocket.WebSocketApp("ws://147.229.193.119:8000", on_message=on_message)
wsapp.run_forever()
