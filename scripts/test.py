import math
import numpy as np
from numpy import cos, sin

pos = {
    "controls": {
        "lh": 0,
        "lv": 0,
        "rh": 0,
        "rv": 660
    },
    "id": "t",
    "pitch": -10.0,
    "roll": 0.0,
    "t": 26099,
    "vX": -3.299999952316284,
    "vY": 2.799999952316284,
    "vZ": 0.0,
    "x": 22.78000000026077,
    "y": -20.85999999998603,
    "yaw": 139.9,
    "z": 0.0
}

targetHeading = get_angle(pos["yaw"])
targetCommandSpeedX = ((pos["controls"]["rv"]/660.0)*10)
targetCommandSpeedY = ((pos["controls"]["rh"]/660.0)*10)
xX = targetCommandSpeedX * cos(math.radians(targetHeading))
yX = targetCommandSpeedX * sin(math.radians(targetHeading))
xY = targetCommandSpeedY * cos(math.radians(targetHeading + 90))
yY = targetCommandSpeedY * sin(math.radians(targetHeading + 90))

targetCommandX = xX + xY
targetCommandY = yX + yY

print(targetCommandX)
print(targetCommandY)