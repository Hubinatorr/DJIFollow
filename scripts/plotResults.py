import json
from pathlib import Path
from plotly.subplots import make_subplots
import plotly.graph_objects as go
import numpy as np

# Create figure
fig = go.Figure()

path = "resultData/normal.json"
follower = json.load(open(Path(__file__).parent / "testData/outside.json", "r"))
target = json.load(open(Path(__file__).parent / "testData/outside.json", "r"))

fig = make_subplots(
    rows=3, cols=2,
    subplot_titles=("X", "VX", "Y", "VY", "Z", "VZ")
)

fig.add_trace(
    go.Scatter(x=[p["t"] for p in follower],
               y=[p["x"] for p in follower],
               mode='lines+markers',
               name='follower'),
    row=1, col=1
)
fig.add_trace(
    go.Scatter(x=[p["t"] for p in target],
               y=[p["x"] for p in target],
               mode='lines+markers',
               name='target'),
    row=1, col=1
)

fig.add_trace(
    go.Scatter(x=[p["t"] for p in follower],
               y=[p["y"] for p in follower],
               mode='lines+markers',
               name='follower'),
    row=2, col=1
)

fig.add_trace(
    go.Scatter(x=[p["t"] for p in target],
               y=[p["y"] for p in target],
               mode='lines+markers',
               name='target'),
    row=2, col=1
)


fig.add_trace(
    go.Scatter(x=[p["t"] for p in follower],
               y=[p["z"] for p in follower],
               mode='lines+markers',
               name='follower'),
    row=3, col=1
)

fig.add_trace(
    go.Scatter(x=[p["t"] for p in target],
               y=[p["z"] for p in target],
               mode='lines+markers',
               name='target'),
    row=3, col=1
)

fig.add_trace(
    go.Scatter(x=[p["t"] for p in follower],
               y=[p["vX"] for p in follower],
               mode='lines+markers',
               name='follower'),
    row=1, col=2
)
fig.add_trace(
    go.Scatter(x=[p["t"] for p in target],
               y=[p["vX"] for p in target],
               mode='lines+markers',
               name='target'),
    row=1, col=2
)

fig.add_trace(
    go.Scatter(x=[p["t"] for p in follower],
               y=[p["vY"] for p in follower],
               mode='lines+markers',
               name='follower'),
    row=2, col=2
)

fig.add_trace(
    go.Scatter(x=[p["t"] for p in target],
               y=[p["vY"] for p in target],
               mode='lines+markers',
               name='target'),
    row=2, col=2
)


fig.add_trace(
    go.Scatter(x=[p["t"] for p in follower],
               y=[p["vZ"] for p in follower],
               mode='lines+markers',
               name='follower'),
    row=3, col=2
)

fig.add_trace(
    go.Scatter(x=[p["t"] for p in target],
               y=[p["vZ"] for p in target],
               mode='lines+markers',
               name='target'),
    row=3, col=2
)

fig.show()

