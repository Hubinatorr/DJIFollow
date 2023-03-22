import json
from pathlib import Path
import plotly.graph_objects as go
import numpy as np
from numpy import transpose as T
from numpy.linalg import inv
from help_module import get_random_noise, get_noise

est_std = np.array([.1, .1, .3, .1, .1, .3, .1, .1, .3])
P = np.array([[est_std[0] ** 2, 0, 0, 0, 0, 0, 0, 0, 0],
              [0, est_std[1] ** 2, 0, 0, 0, 0, 0, 0, 0],
              [0, 0, est_std[2] ** 2, 0, 0, 0, 0, 0, 0],
              [0, 0, 0, est_std[3] ** 2, 0, 0, 0, 0, 0],
              [0, 0, 0, 0, est_std[4] ** 2, 0, 0, 0, 0],
              [0, 0, 0, 0, 0, est_std[5] ** 2, 0, 0, 0],
              [0, 0, 0, 0, 0, 0, est_std[6] ** 2, 0, 0],
              [0, 0, 0, 0, 0, 0, 0, est_std[7] ** 2, 0],
              [0, 0, 0, 0, 0, 0, 0, 0, est_std[8] ** 2],
              ])

gps_std = np.array([1, 1, 300, .1, .1, .3, .1, .1, .3])
R = np.array([[gps_std[0] ** 2, 0, 0, 0, 0, 0, 0, 0, 0],
              [0, gps_std[1] ** 2, 0, 0, 0, 0, 0, 0, 0],
              [0, 0, gps_std[2] ** 2, 0, 0, 0, 0, 0, 0],
              [0, 0, 0, gps_std[3] ** 2, 0, 0, 0, 0, 0],
              [0, 0, 0, 0, gps_std[4] ** 2, 0, 0, 0, 0],
              [0, 0, 0, 0, 0, gps_std[5] ** 2, 0, 0, 0],
              [0, 0, 0, 0, 0, 0, gps_std[6] ** 2, 0, 0],
              [0, 0, 0, 0, 0, 0, 0, gps_std[7] ** 2, 0],
              [0, 0, 0, 0, 0, 0, 0, 0, gps_std[8] ** 2],
              ])

q_std = np.array([0.05, 0.05, 0.05, 0.2, 0.2, 0.1, 0.2, 0.2, 0.1])


def get_Q(dt):
    return np.array(np.array([[q_std[0] ** 2, 0, 0, 0, 0, 0, 0, 0, 0],
                              [0, q_std[1] ** 2, 0, 0, 0, 0, 0, 0, 0],
                              [0, 0, q_std[2] ** 2, 0, 0, 0, 0, 0, 0],
                              [0, 0, 0, q_std[3] ** 2, 0, 0, 0, 0, 0],
                              [0, 0, 0, 0, q_std[4] ** 2, 0, 0, 0, 0],
                              [0, 0, 0, 0, 0, q_std[5] ** 2, 0, 0, 0],
                              [0, 0, 0, 0, 0, 0, q_std[6] ** 2, 0, 0],
                              [0, 0, 0, 0, 0, 0, 0, q_std[7] ** 2, 0],
                              [0, 0, 0, 0, 0, 0, 0, 0, q_std[8] ** 2],
                              ])) * dt


data = get_noise(json.load(open(Path(__file__).parent / "testData/normal.json", "r")), 5, 1.0)
real = json.load(open(Path(__file__).parent / "testData/normal.json", "r"))

# init

prev = np.array(
    [0, 0, 0,
     0, 0, 0,
     0, 0, 0])

state = np.array(
    [0, 0, 0, 0, 0, 0, 0, 0, 0])

states = [prev, state]

for i in range(2, len(data)):
    dt = (data[i]["t"] - data[i - 1]["t"]) / 1000

    F = np.array([[1, 0, 0, dt, 0, 0, 0.5 * (dt ** 2), 0, 0],
                  [0, 1, 0, 0, dt, 0, 0, 0.5 * (dt ** 2), 0],
                  [0, 0, 1, 0, 0, dt, 0, 0, 0.5 * (dt ** 2)],
                  [0, 0, 0, 1, 0, 0, dt, 0, 0],
                  [0, 0, 0, 0, 1, 0, 0, dt, 0],
                  [0, 0, 0, 0, 0, 1, 0, 0, dt],
                  [0, 0, 0, 0, 0, 0, 1, 0, 0],
                  [0, 0, 0, 0, 0, 0, 0, 1, 0],
                  [0, 0, 0, 0, 0, 0, 0, 0, 1],
                  ])
    Q = get_Q(dt)

    H = np.identity(9)
    # predict
    predicted_state = F @ state
    predicted_P = F @ P @ T(F) + Q
    # update
    z = np.array(
        [data[i]["x"], data[i]["y"], data[i]["z"], data[i]["vX"], data[i]["vY"], data[i]["vZ"],
         data[i]["vX"] - data[i - 1]["vX"], data[i]["vY"] - data[i - 1]["vY"],
         data[i]["vZ"] - data[i - 1]["vZ"]])
    I = np.identity(9)
    K = predicted_P @ T(H) @ inv(H @ predicted_P @ T(H) + R)
    state = predicted_state + K @ (z - H @ predicted_state)
    P = (I - K @ H) @ predicted_P @ T(I - K @ H) + K @ R @ T(K)
    states.append(state)

fig = go.Figure()
fig.add_trace(go.Scatter(x=list(range(len(data))), y=[p["x"] for p in real], mode='lines+markers',
                         name="real X"))
fig.add_trace(go.Scatter(x=list(range(len(data))), y=[p["x"] for p in data], mode='lines+markers',
                         name="measurement X"))
fig.add_trace(go.Scatter(x=list(range(len(data))), y=[s[0] for s in states], mode='lines+markers',
                         name="est X"))
fig.show()
