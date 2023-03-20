import json
import signal
from pathlib import Path
import sys

path = sys.argv[1]
data = json.load(open(Path(__file__).parent / path, "r"))



for pos in data:
    pos["z"] = pos["z"] + 1.0

path = sys.argv[1]
with open(Path(__file__).parent / path, "w") as myfile:
    myfile.write(json.dumps(data))
    myfile.close()