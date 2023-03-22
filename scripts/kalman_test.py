import json
import math
import sys
from pathlib import Path
import numpy as np
from numpy import cos, sin
from matplotlib import pyplot as plt
import help_module
import plotly.graph_objects as go

path = "testData/normal.json"
data = json.load(open(Path(__file__).parent / path, "r"))

class Kalman:
    def __init__(self):
        self.ekfState = np.array([0, 0, -1, 0, 0, 0, 0])
        self.prevState = self.ekfState
        self.initStdDevs = np.array([.1, .1, .3, .1, .1, .3, .05])

        self.QUAD_EKF_NUM_STATES = 7

        # Process Covariance Matrix
        self.Q = np.zeros((self.QUAD_EKF_NUM_STATES, self.QUAD_EKF_NUM_STATES))
        # GPS measurement covariance
        self.R_GPS = np.zeros((6, 6))
        # Magnetometer measurement covariance
        self.R_MAG = np.zeros((1, 1))

        self.ekfCov = np.identity(self.QUAD_EKF_NUM_STATES)
        for i in range(self.QUAD_EKF_NUM_STATES):
            self.ekfCov[i, i] = self.initStdDevs[i] * self.initStdDevs[i]

        QPosXYStd = 0.05
        QPosZStd = 0.05
        QVelXYStd = 0.2
        QVelZStd = 0.1
        QYawStd = 0.08

        # GPS measurement std deviations
        GPSPosXYStd = 1
        GPSPosZStd = 300
        GPSVelXYStd = .1
        GPSVelZStd = .3

        # Magnetometer
        MagYawStd = .1

        self.dtIMU = 0.002
        self.attitudeTau = 100

        self.pitchEst = 0
        self.rollEst = 0

        self.R_GPS[0][0] = pow(GPSPosXYStd, 2)
        self.R_GPS[1][1] = pow(GPSPosXYStd, 2)
        self.R_GPS[2][2] = pow(GPSPosZStd, 2)
        self.R_GPS[3][3] = pow(GPSVelXYStd, 2)
        self.R_GPS[4][4] = pow(GPSVelXYStd, 2)
        self.R_GPS[5][5] = pow(GPSVelZStd, 2)

        self.R_MAG[0][0] = pow(MagYawStd, 2)

        self.Q[0][0] = pow(QPosXYStd, 2)
        self.Q[1][1] = pow(QPosXYStd, 2)
        self.Q[2][2] = pow(QPosZStd, 2)
        self.Q[3][3] = pow(QVelXYStd, 2)
        self.Q[4][4] = pow(QVelXYStd, 2)
        self.Q[5][5] = pow(QVelZStd, 2)
        self.Q[6][6] = pow(QYawStd, 2)
        self.Q = np.multiply(self.Q, self.dtIMU)

        self.rollErr = self.pitchErr = self.maxEuler = 0
        self.posErrorMag = self.velErrorMag = 0


    def update(self, z, H, R, zFromX):
        toInvert = np.add(H @ self.ekfCov @ np.transpose(H), R)
        self.ekfCov @ np.transpose(H) @ np.linalg.inv(toInvert)
        K = self.ekfCov @ np.transpose(H) @ np.linalg.inv(toInvert)

        diffZ = np.subtract(z, zFromX)
        eye = np.identity(self.QUAD_EKF_NUM_STATES)
        self.ekfState = np.add(self.ekfState, K @ diffZ)
        self.ekfCov = (np.subtract(eye, K @ H)) @ self.ekfCov

    def updateFromGPS(self, pos):
        z = np.array([pos['x'] - data[0]['x'], pos['y'] - data[0]['y'], pos['z'] - data[0]['z'], pos['vX'], pos['vY'], pos['vZ']])
        zFromX = np.zeros(6)
        hPrime = np.zeros((6, self.QUAD_EKF_NUM_STATES))
        hPrime[0][0] = 1.0
        hPrime[1][1] = 1.0
        hPrime[2][2] = 1.0
        hPrime[3][3] = 1.0
        hPrime[4][4] = 1.0
        hPrime[5][5] = 1.0

        zFromX[0] = self.ekfState[0]
        zFromX[1] = self.ekfState[1]
        zFromX[2] = self.ekfState[2]
        zFromX[3] = self.ekfState[3]
        zFromX[4] = self.ekfState[4]
        zFromX[5] = self.ekfState[5]

        self.update(z, hPrime, self.R_GPS, zFromX)


kalman = Kalman()

xx = []

xEst = []
xZ = []

yEst = []
yZ = []

vxEst = []
vxZ = []

vyEst = []
vyZ = []

x = [pos["x"] for pos in data][1:]
y = [pos["y"] for pos in data][1:]
vX = [pos["vX"] for pos in data][1:]
vY = [pos["vY"] for pos in data][1:]


data = help_module.get_noise(data, 15, 1.0)

kalman.ekfState = np.array([data[0]["x"], data[0]["y"], data[0]["z"], data[0]["vX"], data[0]["vY"], data[0]["vZ"], data[0]['yaw']])
states = [kalman.ekfState]

for i in range(1, len(data)):
    dt = (data[i]["t"] - data[i - 1]["t"]) / 1000

    predictedState = kalman.ekfState
    predictedState[0] = predictedState[0] + predictedState[3] * dt
    predictedState[1] = predictedState[1] + predictedState[4] * dt
    predictedState[2] = predictedState[2] + predictedState[5] * dt

    predictedState[3] = predictedState[3]
    predictedState[4] = predictedState[4]
    predictedState[5] = predictedState[5]


    gPrime = np.identity(kalman.QUAD_EKF_NUM_STATES)
    gPrime[0][3] = dt
    gPrime[1][4] = dt
    gPrime[2][5] = dt
    gPrime[3][6] = dt
    gPrime[4][6] = dt
    gPrime[5][6] = dt

    kalman.ekfCov = np.add(gPrime @ (kalman.ekfCov @ np.transpose(gPrime)), kalman.Q)
    kalman.ekfState = predictedState
    kalman.updateFromGPS(data[i])
    states.append(kalman.ekfState)
    if i == 1:
        print(kalman.ekfCov)


real = json.load(open(Path(__file__).parent / "testData/normal.json", "r"))

fig = go.Figure()
fig.add_trace(go.Scatter(x=list(range(len(data))), y=[p["x"] for p in real], mode='lines+markers', name="real X"))
fig.add_trace(go.Scatter(x=list(range(len(data))), y=[p["x"] for p in data], mode='lines+markers', name="measurement X"))
fig.add_trace(go.Scatter(x=list(range(len(data))), y=[s[0] for s in states], mode='lines+markers', name="est X"))
fig.show()
