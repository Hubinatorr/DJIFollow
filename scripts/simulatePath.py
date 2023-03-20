import json
import websocket
import threading
import time

file1 = open('./testData/outside.json', 'r')

positionsArray = json.load(file1)

counter = 0
max = len(positionsArray)


ws = websocket.WebSocket()
ws.connect("ws://147.229.193.119:8000", suppress_origin=True)

def do_every (interval, worker_func, iterations = 0):
  if iterations != 1:
    threading.Timer (
      interval,
      do_every, [interval, worker_func, 0 if iterations == 0 else iterations-1]
    ).start ()

  worker_func (max - iterations)

def sendData(iteration):
  ws.send(json.dumps(positionsArray[iteration]))

do_every(0.1, sendData, max)
