import json
import math
import sys
from pathlib import Path
import numpy as np
from numpy import cos, sin
from matplotlib import pyplot as plt
import help_module

path = "testData/"+ sys.argv[1] +".json"
data = json.load(open(Path(__file__).parent / path, "r"))

class Kalman:
    def __init__(self):
        self.system_state = np.array([0, 0, -1, 0, 0, 0, 0])
        self.prevState = self.system_state
        self.initStdDevs = np.array([.1, .1, .3, .1, .1, .3, .05])

        self.QUAD_EKF_NUM_STATES = 7

        # Process Covariance Matrix
        self.Q = np.zeros((self.QUAD_EKF_NUM_STATES, self.QUAD_EKF_NUM_STATES))
        # GPS measurement covariance
        self.R_GPS = np.zeros((6, 6))
        # Magnetometer measurement covariance
        self.R_MAG = np.zeros((1, 1))

        self.P = np.identity(self.QUAD_EKF_NUM_STATES)
        for i in range(self.QUAD_EKF_NUM_STATES):
            self.P[i, i] = self.initStdDevs[i] * self.initStdDevs[i]

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



    def predict(self, dt, i, pos):
        newState = self.predictState(dt, targetCommandX, targetCommandY, i)


        F = np.identity(self.QUAD_EKF_NUM_STATES)
        F[0][3] = dt
        F[1][4] = dt
        F[2][5] = dt
        F[3][6] = math.tanh((targetCommandX - self.system_state[3]) / 15)*dt
        F[4][6] = math.tanh((targetCommandY - self.system_state[4]) / 15)*dt
        F[5][6] = 0*dt

        self.P = np.add(F @ (self.P @ np.transpose(F)), self.Q)
        self.system_state = newState

    def predictState(self, dt, cX, cY, i ):
        predictedState = self.system_state
        predictedState[0] = predictedState[0] + self.system_state[3] * dt
        predictedState[1] = predictedState[1] + self.system_state[4] * dt
        predictedState[2] = predictedState[2] + self.system_state[5] * dt
        if i != 0:
            predictedState[3] = predictedState[3] + math.tanh((cX - predictedState[3])/15)*dt
            predictedState[4] = predictedState[4] + math.tanh((cY - predictedState[4])/15)*dt
            predictedState[5] = predictedState[5]

        return predictedState

    def update(self, z, H, R, zFromX):
        toInvert = np.add(H @ self.P @ np.transpose(H), R)
        self.P @ np.transpose(H) @ np.linalg.inv(toInvert)
        K = self.P @ np.transpose(H) @ np.linalg.inv(toInvert)

        diffZ = np.subtract(z, zFromX)
        eye = np.identity(self.QUAD_EKF_NUM_STATES)
        self.system_state = np.add(self.system_state, K @ diffZ)
        self.P = (np.subtract(eye, K @ H)) @ self.P

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

        zFromX[0] = self.system_state[0]
        zFromX[1] = self.system_state[1]
        zFromX[2] = self.system_state[2]
        zFromX[3] = self.system_state[3]
        zFromX[4] = self.system_state[4]
        zFromX[5] = self.system_state[5]

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


for i, pos in enumerate(data):
    if i < len(data) - 1:
        kalman.prevState = kalman.system_state
        kalman.predict((data[i+1]["t"] - data[i]["t"])/1000, i, pos)
        kalman.updateFromGPS(pos)
        xEst.append(kalman.system_state[0])
        xZ.append(pos["x"] - data[0]["x"])
        yEst.append(kalman.system_state[1])
        yZ.append(pos["y"] - data[0]["y"])
        vxEst.append(kalman.system_state[3])
        vxZ.append(pos["vX"])
        vyEst.append(kalman.system_state[4])
        vyZ.append(pos["vY"])
        xx.append(pos["t"] - data[0]["t"])


fig, axs = plt.subplots(2, 2)
axs[0, 0].plot(xx, xEst, label='KF estimate')
axs[0, 0].plot(xx, xZ, label='measurement')
axs[0, 0].plot(xx, x, label='true value')
axs[0, 0].set_title('x')
axs[0, 0].legend(loc="upper left")

axs[0, 1].plot(xx, yEst, label='KF estimate')
axs[0, 1].plot(xx, yZ, label='measurement')
axs[0, 1].plot(xx, y, label='true value')
axs[0, 1].set_title('y')
axs[0, 1].legend(loc="upper left")

axs[1, 0].plot(xx, vxEst, label='KF estimate')
axs[1, 0].plot(xx, vxZ, label='measurement')
axs[1, 0].plot(xx, vX, label='true value')
axs[1, 0].set_title('velocityX')
axs[1, 0].legend(loc="upper left")

axs[1, 1].plot(xx, vyEst, label='KF estimate')
axs[1, 1].plot(xx, vyZ, label='measurement')
axs[1, 1].plot(xx, vY, label='true value')
axs[1, 1].set_title('velocityY')
axs[1, 1].legend(loc="upper left")


for ax in axs.flat:
    ax.set(xlabel='x-label', ylabel='y-label')

# Hide x labels and tick labels for top plots and y ticks for right plots.
for ax in axs.flat:
    ax.label_outer()

plt.show()