import json
from pathlib import Path
import help_module
import numpy as np
from numpy import cos, sin

YawStd = 0.08


class Kalman:
    def __init__(self):
        self.K = None
        self.Q = None
        self.F = None
        self.system_state = [0, 0, 0, 0, 0, 0]
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

    def update(self, x_z, y_z):
        self.K = self.P @ np.transpose(self.H) @ (1 / np.add(self.H @ self.P @ np.transpose(self.H), self.R))
        z = np.zeros(2)
        z[0] = x_z
        z[1] = y_z
        self.system_state = np.add(self.system_state, self.K @ np.subtract(z, self.H @ self.system_state))

        I = np.identity(6)
        self.P = np.add(np.subtract(I, self.K @ self.H) @ self.P @ np.transpose(np.subtract(I, self.K @ self.H)) , self.K @ self.R @ np.transpose(self.K))

    def predict(self, dt):
        # F - state transition matrix
        self.F = np.identity(6)
        self.F[0][1] = dt
        self.F[0][2] = 0.5 * pow(dt, 2)
        self.F[1][2] = dt
        self.F[3][4] = dt
        self.F[3][5] = 0.5 * pow(dt, 2)
        self.F[4][5] = dt

        self.Q = np.matrix((6, 6),
                      [[pow(dt, 4) / 4, pow(dt, 3) / 2, pow(dt, 2) / 2, 0, 0, 0],
                       [pow(dt, 3) / 2, pow(dt, 2), dt, 0, 0, 0],
                       [pow(dt, 2) / 2, dt, 1, 0, 0, 0],
                       [0, 0, 0, pow(dt, 4) / 4, pow(dt, 3) / 2, pow(dt, 2) / 2],
                       [0, 0, 0, pow(dt, 3) / 2, pow(dt, 2), dt],
                       [0, 0, 0, pow(dt, 2) / 2, dt, 1]])

        # State extrapolation
        self.system_state = np.multiply(self.F, dt)

        self.P = np.add(self.F @ self.P @ np.transpose(self.F), Q)

        #
        # # The estimate uncertainty in matrix form is:
        # Pnn = np.matrix((6, 6),
        #                 [[pow(self.p_posXYStd, 2), pow(self.p_velXYStd, 2), pow(self.p_accXYStd, 2),
        #                   0, 0, 0],
        #                  [pow(self.p_velXYStd, 2), pow(self.p_velXYStd, 2), pow(self.p_accXYStd, 2),
        #                   0, 0, 0],
        #                  [pow(self.p_velXYStd, 2), pow(self.p_velXYStd, 2), pow(self.p_accXYStd, 2),
        #                   0, 0, 0],
        #                  [0, 0, 0, pow(self.p_posXYStd, 2), pow(self.p_velXYStd, 2),
        #                   pow(self.p_accXYStd, 2)],
        #                  [0, 0, 0, pow(self.p_velXYStd, 2), pow(self.p_velXYStd, 2),
        #                   pow(self.p_accXYStd, 2)],
        #                  [0, 0, 0, pow(self.p_velXYStd, 2), pow(self.p_velXYStd, 2),
        #                   pow(self.p_accXYStd, 2)]])
        #
        #
        # # process noise metrix
        # Q = Q @ pow(self.p_accXYStd, 2)
        #
        # Rn = [[pow(self.GPSPosXYStd, 2), 0], [0, pow(self.GPSPosXYStd, 2)]]
        #
        # # covariance extrapolation equation
        # Pnew = np.add(F @ Pnn @ np.transpose(F), Q)
        #
        # # measurement equation
        # # measureMentState = [x_gps, y_gps]
        # H = np.zeros((2, 6))
        # H[0][0] = 1.0
        # H[1][3] = 1.0
        #
        # # measurement covariance matrix

        # Kalman gain
        # Kn = Pnn @ np.transpose(H) @ np.add(H @ Pnn @ np.transpose(H), Rn)


path = "testData/normal.json"
data = json.load(open(Path(__file__).parent / path, "r"))
noise = help_module.get_random_noise(data, 2)

kalman = Kalman()
kalman.predict(0.1)

for pos in data:
    kalman.update(pos["x"], pos["y"])
