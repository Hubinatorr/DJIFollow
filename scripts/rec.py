import websocket
from pathlib import Path


def on_message(wsapp, message):
    path = "resultData/kalman_kotlin.json"
    with open(Path(__file__).parent / path, "w") as myfile:
        myfile.write(message)


wsapp = websocket.WebSocketApp("ws://147.229.193.119:8000", on_message=on_message)
wsapp.run_forever()
