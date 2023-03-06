from matplotlib import pyplot as plt
import numpy as np
import websocket
import json
import signal
from pathlib import Path
import sys

files = ["speed10"]

for file in files:
    path = "data/" + file + ".json"
    with open(Path(__file__).parent / path, "r") as myfile:
        data = json.load(myfile)
        y = list(map(lambda d: d["vX"], data))
        x = list(map(lambda d: d["t"]- data[0]["t"], data))
        aY = []
        aX = []
        for i, d in enumerate(data):
            if d["vX"] > 0.0:
                a = (d["vX"] - data[i-1]["vX"]) / ((d["t"] - data[i-1]["t"])/1000)
                aY.append(a)
                aX.append(x[i])
        print(y)
        plt.plot(x,y)
        myfile.close()

plt.show()
