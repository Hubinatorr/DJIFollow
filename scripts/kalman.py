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

    def getRbgPrime(self, roll, pitch, yaw):
        theta = math.radians(roll)
        phi = math.radians(pitch)
        psi = math.radians(yaw)
        rbgPrime = np.zeros((3,3))
        rbgPrime[0][0] = -cos(theta)*sin(psi)
        rbgPrime[0][1] = -sin(phi)*sin(theta)*sin(psi) - cos(phi)*cos(psi)
        rbgPrime[0][2] = -cos(phi)*sin(theta)*sin(psi) + sin(phi)*cos(psi)
        rbgPrime[1][0] = cos(theta)*cos(psi)
        rbgPrime[1][1] = sin(phi)*sin(theta)*cos(psi) - cos(phi)*sin(psi)
        rbgPrime[1][2] = cos(phi)*sin(theta)*cos(psi) + sin(phi)*sin(psi)

        return rbgPrime

    def predict(self, dt, i, pos):

        targetHeading = help_module.get_angle(pos["yaw"])
        targetCommandSpeedX = ((pos["controls"]["rv"]/660.0)*10)
        targetCommandSpeedY = ((pos["controls"]["rh"]/660.0)*10)
        xX = targetCommandSpeedX * cos(math.radians(targetHeading))
        yX = targetCommandSpeedX * sin(math.radians(targetHeading))
        xY = targetCommandSpeedY * cos(math.radians(targetHeading + 90))
        yY = targetCommandSpeedY * sin(math.radians(targetHeading + 90))
        targetCommandX = xX + xY
        targetCommandY = yX + yY


        newState = self.predictState(dt, targetCommandX, targetCommandY, i)


        gPrime = np.identity(self.QUAD_EKF_NUM_STATES)
        gPrime[0][3] = dt
        gPrime[1][4] = dt
        gPrime[2][5] = dt
        gPrime[3][6] = math.tanh((targetCommandX - self.ekfState[3]) / 10)*dt
        gPrime[4][6] = math.tanh((targetCommandY - self.ekfState[4]) / 10)*dt
        gPrime[5][6] = 0*dt

        self.ekfCov = np.add(gPrime @ (self.ekfCov @ np.transpose(gPrime)), self.Q)
        self.ekfState = newState

    def predictState(self, dt, cX, cY, i ):
        predictedState = self.ekfState
        predictedState[0] = predictedState[0] + self.ekfState[3] * dt
        predictedState[1] = predictedState[1] + self.ekfState[4] * dt
        predictedState[2] = predictedState[2] + self.ekfState[5] * dt
        if i != 0:
            predictedState[3] = predictedState[3] + math.tanh((cX - predictedState[3])/5)*dt
            predictedState[4] = predictedState[4] + math.tanh((cY - predictedState[4])/15)*dt
            predictedState[5] = predictedState[5]

        return predictedState

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

data = help_module.get_noise(data, 20, 1)


for i, pos in enumerate(data):
    if i < len(data) - 1:
        kalman.prevState = kalman.ekfState

        kalman.predict((data[i+1]["t"] - data[i]["t"])/1000, i, pos)

        xEst.append(kalman.ekfState[0])
        xZ.append(pos["x"] - data[0]["x"])
        yEst.append(kalman.ekfState[1])
        yZ.append(pos["y"] - data[0]["y"])
        vxEst.append(kalman.ekfState[3])
        vxZ.append(pos["vX"])
        vyEst.append(kalman.ekfState[4])
        vyZ.append(pos["vY"])
        xx.append(pos["t"] - data[0]["t"])
        kalman.updateFromGPS(pos)

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