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

gps_std = np.array([1.0, 1.0, 1.0, .1, .1, .3, .1, .1, .3])
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


data = json.load(open(Path(__file__).parent / "testData/full.json", "r"))
# init

state = np.array(
    [data[0]["x"], data[0]["y"], data[0]["z"], data[0]["vX"], data[0]["vY"], data[0]["vZ"], 0, 0, 0])

states = [state]

for i in range(1, len(data)):
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
    print("predict")

    data[i]["vZ"] = -data[i]["vZ"]

    H = np.identity(9)
    # predict
    predicted_state = F @ state
    print("pred_state")
    print(predicted_state)
    predicted_P = F @ P @ T(F) + Q
    print("pred_p")
    print(predicted_P)

    # update
    print("update")
    z = np.array(
        [data[i]["x"], data[i]["y"], data[i]["z"], data[i]["vX"], data[i]["vY"], data[i]["vZ"],
         (data[i]["vX"] - data[i - 1]["vX"]) / dt, (data[i]["vY"] - data[i - 1]["vY"]) / dt,
         (data[i]["vZ"] - data[i - 1]["vZ"]) / dt])
    I = np.identity(9)
    print("inv")
    print(H @ predicted_P @ T(H) + R)
    K = predicted_P @ T(H) @ inv(H @ predicted_P @ T(H) + R)
    print("k")
    print(K)
    state = predicted_state + K @ (z - H @ predicted_state)
    print("state")
    print(state)
    P = (I - K @ H) @ predicted_P @ T(I - K @ H) + K @ R @ T(K)
    print("P")
    print(P)
    print("----------")
    print(i)
    states.append(state)

kotlin = json.load(open(Path(__file__).parent / "resultData/kalmanTestFull.json", "r"))
kotlin1 = json.load(open(Path(__file__).parent / "resultData/fixedKalman.json", "r"))
fig = go.Figure()
fig.add_trace(go.Scatter(x=list(range(len(data))), y=[p["vY"] for p in data], mode='lines+markers',
                         name="data"))
fig.add_trace(go.Scatter(x=list(range(len(data))), y=[p[4] for p in states], mode='lines+markers',
                         name="stavy"))
# fig.add_trace(go.Scatter(x=list(range(len(kotlin))), y=[p["vZ"] for p in kotlin if p["id"] == "Kalman"], mode='lines+markers',
#                          name="kotlin"))
fig.add_trace(go.Scatter(x=list(range(len(kotlin))), y=[p["vY"] for p in kotlin1 if p["id"] == "KalmanResult"], mode='lines+markers',
                         name="kotlin2"))

# fig.add_trace(
#     go.Scatter(x=list(range(len(kotlin))), y=[s["x"] for s in kotlin], mode='lines+markers',
#                name="est kotlin X"))
fig.show()
