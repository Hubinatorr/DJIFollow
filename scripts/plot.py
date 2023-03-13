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

from help_module import get_angle

def plot_controls(data):
    xx = []
    yy = []
    t = [pos["t"] for pos in data]
    vX = [pos["vX"] for pos in data]
    vY = [pos["vY"] for pos in data]
    acX = [0]
    acY = [0]
    for i, pos in enumerate(data):
        targetHeading = get_angle(pos["yaw"])
        targetCommandSpeedX = ((pos["controls"]["rv"]/660.0)*10)
        targetCommandSpeedY = ((pos["controls"]["rh"]/660.0)*10)
        xX = targetCommandSpeedX * cos(math.radians(targetHeading))
        yX = targetCommandSpeedX * sin(math.radians(targetHeading))
        xY = targetCommandSpeedY * cos(math.radians(targetHeading + 90))
        yY = targetCommandSpeedY * sin(math.radians(targetHeading + 90))
        if i != 0:
            acX.append((pos["vX"] - data[i-1]["vX"])/ ((pos["t"] - data[i-1]["t"])/1000))
            acY.append((pos["vY"] - data[i-1]["vY"])/ (pos["t"] - data[i-1]["t"]))
            print(acX)
        targetCommandX = xX + xY
        targetCommandY = yX + yY
        xx.append(targetCommandX)
        yy.append(targetCommandY)

    fig, axs = plt.subplots(2, 1)
    axs[0].plot(t, xx, label='Command')
    axs[0].plot(t, acX)
    axs[0].plot(t, vX, label='speed')
    axs[0].legend(loc="upper left")
    axs[0].set_title('x')
    axs[1].plot(t, yy, label='Command')
    axs[1].plot(t, acY)
    axs[1].plot(t, vY, label='speed')
    axs[1].legend(loc="upper left")
    axs[1].set_title('y')

    for ax in axs.flat:
        ax.set(xlabel='x-label', ylabel='y-label')

    # Hide x labels and tick labels for top plots and y ticks for right plots.
    for ax in axs.flat:
        ax.label_outer()
    plt.show()




path = sys.argv[1]
data = json.load(open(Path(__file__).parent / path, "r"))
plot_controls(data)

