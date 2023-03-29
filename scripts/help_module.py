from math import sin, pi
import random


def get_noise(data, f, max_noise):
    noise_data = data
    for i, pos in enumerate(noise_data):
        n = sin((2 * pi) / f * (i % f)) * max_noise
        pos["x"] = pos["x"] + n
        pos["y"] = pos["y"] + n
        # pos["vX"] = pos["vX"] + n/2
        # pos["vY"] = pos["vY"] + n/2
    return noise_data


def get_random_noise(data, max_noise):
    noise_data = data
    for i, pos in enumerate(noise_data):
        n = random.randrange(-max_noise, max_noise)
        pos["x"] = pos["x"] + n
        pos["y"] = pos["y"] + n
        # pos["vX"] = pos["vX"] + n
        # pos["vY"] = pos["vY"] + n
    return noise_data


def get_angle(angle):
    if angle in range(180):
        return angle
    else:
        return 360 + angle
