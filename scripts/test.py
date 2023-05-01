import json
import signal
from pathlib import Path
import sys

path = sys.argv[1]
data = json.load(open(Path(__file__).parent / path, "r"))
data = [p for p in data if p["id"]=="GPS"]


path = sys.argv[1]
with open(Path(__file__).parent / path, "w") as myfile:
    myfile.write(json.dumps(data))
    myfile.close()