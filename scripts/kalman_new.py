import json
from pathlib import Path
from plotly.subplots import make_subplots
import plotly.graph_objects as go
import numpy as np
from numpy import transpose as T
from numpy.linalg import inv
from help_module import get_random_noise, get_noise


def get_F(dt):
    return np.array([[1, dt, 0.5 * (dt ** 2), 0, 0, 0],
                     [0, 1, dt, 0, 0, 0],
                     [0, 0, 1, 0, 0, 0],
                     [0, 0, 0, 1, dt, 0.5 * (dt ** 2)],
                     [0, 0, 0, 0, 1, dt],
                     [0, 0, 0, 0, 0, 1]])


def get_Q(dt):
    return np.array([[pow(dt, 4) / 4, pow(dt, 3) / 2, pow(dt, 2) / 2, 0, 0, 0],
                     [pow(dt, 3) / 2, pow(dt, 2), dt, 0, 0, 0],
                     [pow(dt, 2) / 2, dt, 1, 0, 0, 0],
                     [0, 0, 0, pow(dt, 4) / 4, pow(dt, 3) / 2, pow(dt, 2) / 2],
                     [0, 0, 0, pow(dt, 3) / 2, pow(dt, 2), dt],
                     [0, 0, 0, pow(dt, 2) / 2, dt, 1]])

a_std = 0.2
xy_z_std = 3

I = np.identity(6)

R = np.array([[xy_z_std ** 2, 0],
              [0, xy_z_std ** 2]])

H = np.array([[1, 0, 0, 0, 0, 0],
              [0, 0, 0, 1, 0, 0]])

data = json.load(open(Path(__file__).parent / "testData/normal.json", "r"))
data = get_noise(data, 15, 1.0)

# init with first measurement
state = np.array([data[0]["x"], data[0]["vX"], 0, data[0]["y"], data[0]["vY"], 0])

P = np.array([[0.1**2, 0, 0, 0, 0, 0],
              [0, 0.1**2, 0, 0, 0, 0],
              [0, 0, 0.1**2, 0, 0, 0],
              [0, 0, 0, 0.1**2, 0, 0],
              [0, 0, 0, 0, 0.1**2, 0],
              [0, 0, 0, 0, 0, 0.1**2],
              ])

states = [state]
dist = 0

for i in range(1, len(data)):
    dt = (data[i]["t"] - data[i - 1]["t"]) / 1000
    F = get_F(dt)
    Q = get_Q(dt)

    # predict
    predicted_state = F @ state
    predicted_P = F @ P @ T(F) + Q
    # update
    z = np.array([data[i]["x"], data[i]["y"]])
    K = predicted_P @ T(H) @ inv(H @ predicted_P @ T(H) + R)
    state = predicted_state + K @ (z - H @ predicted_state)
    P = (I - K @ H) @ predicted_P @ T(I - K @ H) + K @ R @ T(K)
    states.append(state)
    if i == 1:
        print(P)


real = json.load(open(Path(__file__).parent / "testData/normal.json", "r"))

# fig = go.Figure()
# fig.add_trace(go.Scatter(x=list(range(len(data))), y=[p["x"] for p in real], mode='lines+markers', name="real X"))
# fig.add_trace(go.Scatter(x=list(range(len(data))), y=[p["x"] for p in data], mode='lines+markers', name="measurement X"))
# fig.add_trace(go.Scatter(x=list(range(len(data))), y=[s[0] for s in states], mode='lines+markers', name="est X"))
# fig.show()

# fig = go.Figure()
# fig.add_trace(go.Scatter(x=list(range(len(data))), y=[p["y"] for p in real], mode='lines+markers', name="real Y"), row=2, col=1)
# fig.add_trace(go.Scatter(x=list(range(len(data))), y=[p["y"] for p in data], mode='lines+markers', name="measurement Y"), row=2, col=1)
# fig.add_trace(go.Scatter(x=list(range(len(data))), y=[s[3] for s in states], mode='lines+markers', name="est Y"), row=2, col=1)
# fig.show()
