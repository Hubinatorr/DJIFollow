from matplotlib import pyplot as plt
import json
from pathlib import Path
import sys
from math import sin, pi

path = "data/" + sys.argv[1] + '.json'
data = json.load(open(Path(__file__).parent / path, "r"))

maxNoise = 1
f = 5

t = [pos["t"] for pos in data]
x = [pos["x"] for pos in data]
xNoise = []

for i, posx in enumerate(x):
    n = sin((2*pi)/f * (i % f)) * maxNoise
    xNoise.append(posx + n)

for i, pos in enumerate(data):
    n = sin((2*pi)/f * (i % f)) * maxNoise
    pos["x"] = pos["x"] + n
    pos["y"] = pos["y"] + n

path = "data/" + sys.argv[1] + 'noise_' + str(maxNoise) +'_' + str(f)+'.json'
with open(Path(__file__).parent / path, "w") as myfile:
    myfile.write(json.dumps(data))
    myfile.close()

fig, axs = plt.subplots(2, 1)
axs[0].plot(t, x)
axs[0].set_title('x')
axs[1].plot(t, xNoise)
axs[1].set_title('xNoise')

for ax in axs.flat:
    ax.set(xlabel='x-label', ylabel='y-label')

# Hide x labels and tick labels for top plots and y ticks for right plots.
for ax in axs.flat:
    ax.label_outer()
plt.show()
