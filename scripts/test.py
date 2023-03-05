import json
from pathlib import Path

with open(Path(__file__).parent / path, "r") as myfile:
    a = json.load(myfile)
    print(len(a))
    myfile.close()