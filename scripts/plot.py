from matplotlib import pyplot as plt
import numpy as np
import websocket
import json
import signal
from pathlib import Path
import sys
import numpy as np
from numpy import cos, sin
import math

path = sys.argv[1]
data = json.load(open(Path(__file__).parent / path, "r"))

def get_angle(angle):
    if angle in range(180):
        return angle
    else:
        return 360 + angle

t = [pos["t"] for pos in data]
x = [pos["vX"] for pos in data]
y = [pos["vY"] for pos in data]

xx = []
yy = []
command = []
for pos in data:
    targetHeading = get_angle(pos["yaw"])
    targetCommandSpeedX = ((pos["controls"]["rv"]/660.0)*10)
    targetCommandSpeedY = ((pos["controls"]["rh"]/660.0)*10)
    xX = targetCommandSpeedX * cos(math.radians(targetHeading))
    yX = targetCommandSpeedX * sin(math.radians(targetHeading))
    xY = targetCommandSpeedY * cos(math.radians(targetHeading + 90))
    yY = targetCommandSpeedY * sin(math.radians(targetHeading + 90))

    targetCommandX = xX + xY
    targetCommandY = yX + yY
    xx.append(targetCommandX)
    yy.append(targetCommandY)



fig, axs = plt.subplots(2, 1)
axs[0].plot(t, command)
axs[0].plot(t, x)
# axs[0].plot(t, xx)
axs[0].set_title('x')
axs[1].plot(t, y)
axs[1].plot(t, yy)
axs[1].set_title('y')

for ax in axs.flat:
    ax.set(xlabel='x-label', ylabel='y-label')

# Hide x labels and tick labels for top plots and y ticks for right plots.
for ax in axs.flat:
    ax.label_outer()
plt.show()
