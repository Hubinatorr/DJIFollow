import json
import signal
from pathlib import Path
import sys

path = "data/" + sys.argv[1] + '.json'
data = json.load(open(Path(__file__).parent / path, "r"))

oX = data[0]["x"]
oY = data[0]["y"]
oZ = data[0]["z"]
oT = data[0]["t"]

for pos in data:
    pos["x"] = pos["x"]-oX
    pos["y"] = pos["y"]-oY
    pos["z"] = pos["z"]-oZ
    pos["t"] = pos["t"]-oT

path = "data/" + sys.argv[1] + '.json'
with open(Path(__file__).parent / path, "w") as myfile:
    myfile.write(json.dumps(data))
    myfile.close()