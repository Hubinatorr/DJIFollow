import json
from pathlib import Path

from plotly.subplots import make_subplots
import plotly.graph_objects as go
import help_module
import numpy as np
from numpy import cos, sin

YawStd = 0.08


class Kalman:
    def __init__(self):
        self.K = None
        self.Q = None
        self.F = None
        self.system_state = np.array([0, 0, 0, 0, 0, 0])
        self.GPSPosXYStd = 1
        self.GPSVelXYStd = .1
        self.p_posXYStd = 0.05
        self.p_velXYStd = 0.2
        self.p_accXYStd = 0.2
        self.initStdDevs = np.array([.1, .1, .3, .1, .1, .3])
        self.P = np.identity(6)
        for i in range(6):
            self.P[i, i] = self.initStdDevs[i] * self.initStdDevs[i]
        # measurement equation
        self.H = np.zeros((2, 6))
        self.H[0][0] = 1.0
        self.H[1][3] = 1.0
        self.R = [[pow(self.GPSPosXYStd, 2), 0], [0, pow(self.GPSPosXYStd, 2)]]
        dt = 0.1
        self.Q = np.matrix([[pow(dt, 4) / 4, pow(dt, 3) / 2, pow(dt, 2) / 2, 0, 0, 0],
                            [pow(dt, 3) / 2, pow(dt, 2), dt, 0, 0, 0],
                            [pow(dt, 2) / 2, dt, 1, 0, 0, 0],
                            [0, 0, 0, pow(dt, 4) / 4, pow(dt, 3) / 2, pow(dt, 2) / 2],
                            [0, 0, 0, pow(dt, 3) / 2, pow(dt, 2), dt],
                            [0, 0, 0, pow(dt, 2) / 2, dt, 1]])
        self.Q = np.multiply(self.Q, pow(self.p_accXYStd, 2))
        # F - state transition matrix

    def update(self, pos):
        z = np.array([pos["x"], pos["y"]])
        zFromX = np.zeros(2)
        zFromX[0] = self.system_state[0]
        zFromX[1] = self.system_state[3]

        diffZ = np.subtract(z, zFromX)
        invert = np.add(self.H @ self.P @ np.transpose(self.H), self.R)

        self.K = self.P @ np.transpose(self.H) @ np.linalg.inv(invert)

        self.system_state = np.add(self.system_state, self.K @ diffZ)

        I = np.identity(6)
        self.P = np.add(np.subtract(I, self.K @ self.H) @ self.P @ np.transpose(np.subtract(I, self.K @ self.H)) , self.K @ self.R @ np.transpose(self.K))

    def predict(self, dt):
        # State extrapolation
        F = np.identity(6)
        F[0][1] = dt
        F[0][2] = 0.5 * pow(dt, 2)
        F[1][2] = dt
        F[3][4] = dt
        F[3][5] = 0.5 * pow(dt, 2)
        F[4][5] = dt

        new_state = self.system_state
        new_state[0] = new_state[0] + new_state[1]*dt + 0.5*new_state[2]*pow(dt, 2)
        new_state[1] = new_state[1] + new_state[2]*dt
        new_state[2] = new_state[2]
        new_state[3] = new_state[3] + new_state[4]*dt + 0.5*new_state[5]*pow(dt, 2)
        new_state[4] = new_state[4] + new_state[5]*dt
        new_state[5] = new_state[5]

        self.P = np.add(F @ self.P @ np.transpose(F), self.Q)
        self.system_state = new_state

path = "testData/normal.json"
data = json.load(open(Path(__file__).parent / "testData/normal.json", "r"))
noise = help_module.get_random_noise(data, 2)

kalman = Kalman()
kalman.predict(0.1)

xEst = []
yEst = []
xx = []
for i, pos in enumerate(data):
    kalman.update(pos)
    kalman.predict(0.1)
    xEst.append(kalman.system_state[0])
    yEst.append(kalman.system_state[1])
    xx.append(pos["t"])

print(xEst)
print(yEst)

# fig = make_subplots(rows=2, cols=1, subplot_titles=("x", "y"))
# fig.add_trace(go.Scatter(x=xx, y=xEst, mode='lines+markers', name="xKalman"), row=1, col=1)
# fig.add_trace(go.Scatter(x=xx, y=[pos["x"] for pos in data], mode='lines+markers', name="xTrue"), row=1, col=1)
# fig.add_trace(go.Scatter(x=xx, y=yEst, mode='lines+markers', name="yKalman"), row=2, col=1)
# fig.add_trace(go.Scatter(x=xx, y=[pos["y"] for pos in data], mode='lines+markers', name="yTrue"), row=2, col=1)
#
# fig.show()