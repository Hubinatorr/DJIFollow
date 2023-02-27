import json

modulo = 4


def normalize_file(name):
    data = json.load(open('../app/src/main/res/raw/' + name, 'r'))
    i = 4
    final = []
    oX = data[0]['x']
    oY = data[0]['y']
    oZ = data[0]['z']
    oT = data[0]['Timestamp']

    for position in data:
        if i % modulo == 0:
            position['Timestamp'] = position['Timestamp'] - oT
            position['x'] = position['x'] - oX
            position['y'] = position['y'] - oY
            position['z'] = position['z'] - oZ + 1
            final.append({
                "x": position['x'] - oX,
                "y": position['y'] - oY,
                "z": position['z'] - oZ + 1
            })
        i = i + 1

    with open('../app/src/main/res/raw/' + name + str(modulo * 40), 'w') as myfile:
        myfile.write(json.dumps(final))


if __name__ == "__main__":
    normalize_file('normal')
    normalize_file('full')
    normalize_file('shaky')
