from matplotlib import pyplot as plt
import numpy as np
import websocket
import json
import signal
from pathlib import Path
import sys

path = "recordData/" + sys.argv[1] + '.json'
data = json.load(open(Path(__file__).parent / path, "r"))


t = [pos["t"] for pos in data]
x = [pos["x"] for pos in data]
y = [pos["y"] for pos in data]

fig, axs = plt.subplots(2, 1)
axs[0].plot(t, x)
axs[0].set_title('x')
axs[1].plot(t, y)
axs[1].set_title('y')

for ax in axs.flat:
    ax.set(xlabel='x-label', ylabel='y-label')

# Hide x labels and tick labels for top plots and y ticks for right plots.
for ax in axs.flat:
    ax.label_outer()
plt.show()
