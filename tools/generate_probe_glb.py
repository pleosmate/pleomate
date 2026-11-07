"""Generate our own small glTF 2.0 probe, using only the Python standard library.
Not a final bear/rabbit asset: exercises PBR meshes and node animation in Filament.
"""
import json
import math
import struct
import sys
from pathlib import Path

STUDY = '--study' in sys.argv
OUTPUT = Path(__file__).resolve().parents[1] / ('app/src/debug/assets/models/companion-study.glb' if STUDY else 'app/src/debug/assets/models/companion-probe.glb')


def build():
    binary = bytearray()
    doc = {'asset': {'version': '2.0', 'generator': 'PleosMate local probe generator'},
           'scene': 0, 'scenes': [{'nodes': [0, 1]}], 'nodes': [], 'meshes': [],
           'materials': [], 'bufferViews': [], 'accessors': [], 'animations': []}

    def accessor(values, width, kind='VEC3', component=5126, target=None, bounds=False):
        while len(binary) % 4:
            binary.append(0)
        offset = len(binary)
        code = 'f' if component == 5126 else 'H'
        flat = [number for value in values for number in value]
        binary.extend(struct.pack('<' + code * len(flat), *flat))
        view = {'buffer': 0, 'byteOffset': offset, 'byteLength': len(binary) - offset}
        if target:
            view['target'] = target
        doc['bufferViews'].append(view)
        result = {'bufferView': len(doc['bufferViews']) - 1, 'componentType': component,
                  'count': len(values), 'type': kind}
        if bounds:
            result['min'] = [min(v[i] for v in values) for i in range(width)]
            result['max'] = [max(v[i] for v in values) for i in range(width)]
        doc['accessors'].append(result)
        return len(doc['accessors']) - 1

    def geometry(positions, normals, triangles):
        position = accessor(positions, 3, target=34962, bounds=True)
        normal = accessor(normals, 3, target=34962)
        indices = accessor([[i] for tri in triangles for i in tri], 1, 'SCALAR', 5123, 34963)
        return {'attributes': {'POSITION': position, 'NORMAL': normal}, 'indices': indices}

    def sphere_geometry(pear=False):
        positions, normals, triangles = [], [], []
        latitudes, longitudes = 48, 64
        for lat in range(latitudes + 1):
            theta = math.pi * lat / latitudes
            radius = 1 + 0.12 * (1 - math.cos(theta)) / 2 if pear else 1
            dr = 0.06 * math.sin(theta) if pear else 0
            for lon in range(longitudes + 1):
                phi = math.tau * lon / longitudes
                positions.append([radius * math.sin(theta) * math.cos(phi), math.cos(theta), radius * math.sin(theta) * math.sin(phi)])
                horizontal = math.sin(theta)
                vertical = dr * math.sin(theta) + radius * math.cos(theta)
                length = math.hypot(horizontal, vertical)
                normals.append([horizontal * math.cos(phi) / length, vertical / length, horizontal * math.sin(phi) / length])
        for lat in range(latitudes):
            for lon in range(longitudes):
                a = lat * (longitudes + 1) + lon
                b = a + longitudes + 1
                if lat > 0:
                    triangles.append([a, a + 1, b])
                if lat < latitudes - 1:
                    triangles.append([a + 1, b + 1, b])
        return geometry(positions, normals, triangles)

    sphere_shape = sphere_geometry()
    body_shape = sphere_geometry(pear=True)
    materials = [
        ('ivory', [0.96, 0.88, 0.78, 1] if STUDY else [0.91, 0.84, 0.73, 1], 0.67),
        ('ear', [0.90, 0.60, 0.47, 1], 0.8),
        ('eyes', [0.035, 0.021, 0.016, 1], 0.19),
        ('base', [0.78, 0.72, 0.62, 1], 0.85),
        ('muzzle', [0.98, 0.92, 0.81, 1], 0.7),
        ('blush', [0.89, 0.54, 0.45, 1], 0.9),
        ('highlight', [1, 1, 1, 1], 0.16),
        ('cushion', [0.45, 0.54, 0.59, 1], 0.93),
    ]
    for name, color, roughness in materials:
        material = len(doc['materials'])
        doc['materials'].append({'name': name, 'pbrMetallicRoughness': {
            'baseColorFactor': color, 'metallicFactor': 0, 'roughnessFactor': roughness}})
        doc['meshes'].append({'name': name, 'primitives': [{**sphere_shape, 'material': material}]})
    body_mesh = len(doc['meshes'])
    doc['meshes'].append({'name': 'soft_pear_body', 'primitives': [{**body_shape, 'material': 0}]})
    doc['nodes'] = [{'name': 'companion', 'children': []},
                    {'name': 'platform', 'mesh': 3, 'translation': [0, -0.045, 0], 'scale': [0.95, 0.065, 0.7]}]

    def sphere(name, translation, scale, mesh=0, parent=0):
        doc['nodes'].append({'name': name, 'mesh': mesh, 'translation': translation, 'scale': scale})
        node = len(doc['nodes']) - 1
        doc['nodes'][parent].setdefault('children', []).append(node)
        return node

    sphere('body', [0, 0.82, 0], ([0.68, 0.68, 0.49] if STUDY else [0.59, 0.75, 0.48]), body_mesh)
    for x in [-0.40, 0.40]:
        sphere('ear', [x, 1.45, -0.05], [0.19, 0.225, 0.135])
        sphere('ear_inner', [x, 1.46, 0.062], [0.115, 0.145, 0.035], 1)
        sphere('foot', [x * 0.7, 0.12, 0.29], [0.17, 0.13, 0.20])
        sphere('cheek', [x * 0.85, 0.91, 0.436], [0.088, 0.038, 0.018], 5)
    eyes = [sphere('eye', [x, 1.06, 0.475], ([0.062, 0.068, 0.035] if STUDY else [0.054, 0.074, 0.035]), 2) for x in [-0.21, 0.21]]
    for eye in eyes:
        sphere('eye_glint', [-0.26, 0.35, 0.83], [0.24, 0.19, 0.25], 6, eye)
    sphere('muzzle', [0, 0.87, 0.482], [0.22, 0.14, 0.115], 4)
    sphere('nose', [0, 0.93, 0.59], [0.047, 0.029, 0.025], 2)

    # A real curved smile tube follows the muzzle, instead of a single dark bead.
    positions, normals, triangles = [], [], []
    segments, sides = 24, 8
    for i in range(segments + 1):
        t = i / segments
        x, y = (t - 0.5) * 0.12, 0.862 - 0.026 * math.sin(math.pi * t)
        tangent_y = -0.026 * math.pi * math.cos(math.pi * t)
        length = math.hypot(0.12, tangent_y)
        nx, ny = -tangent_y / length, 0.12 / length
        for j in range(sides + 1):
            angle = j / sides * math.tau
            n = [nx * math.cos(angle), ny * math.cos(angle), math.sin(angle)]
            positions.append([x + 0.007 * n[0], y + 0.007 * n[1], 0.596 + 0.007 * n[2]])
            normals.append(n)
    for i in range(segments):
        for j in range(sides):
            a = i * (sides + 1) + j
            b = a + sides + 1
            triangles.extend([[a, a + 1, b], [a + 1, b + 1, b]])
    smile_mesh = len(doc['meshes'])
    doc['meshes'].append({'name': 'smile', 'primitives': [{**geometry(positions, normals, triangles), 'material': 2}]})
    sphere('smile', [0, 0, 0], [1, 1, 1], smile_mesh)
    sphere('resting_paw', [-0.58, 0.69, 0.04], [0.135, 0.23, 0.16])
    doc['nodes'].append({'name': 'wave_pivot', 'translation': [0.58, 0.92, 0.06]})
    paw = len(doc['nodes']) - 1
    doc['nodes'][0]['children'].append(paw)
    sphere('paw', [0, -0.16, 0], [0.14, 0.23, 0.15], parent=paw)
    cushion = sphere('cushion', [-0.64, 0.055, -0.22], [0.25, 0.085, 0.23], 7, 1)
    doc['nodes'][1]['children'].remove(cushion)
    doc['scenes'][0]['nodes'].append(cushion)

    def animation(name, times, tracks):
        time = accessor([[t] for t in times], 1, 'SCALAR', bounds=True)
        result = {'name': name, 'samplers': [], 'channels': []}
        for node, path, values in tracks:
            output = accessor(values, len(values[0]), 'VEC4' if path == 'rotation' else 'VEC3')
            result['samplers'].append({'input': time, 'output': output, 'interpolation': 'LINEAR'})
            result['channels'].append({'sampler': len(result['samplers']) - 1, 'target': {'node': node, 'path': path}})
        doc['animations'].append(result)

    times = [0, 1.1, 2.2, 2.5, 2.59, 2.68, 3.3, 4.4]
    scales = [[1 + 0.003 * math.sin(t / 4.4 * math.tau), 1 + 0.009 * math.sin(t / 4.4 * math.tau), 1] for t in times]
    blink = [[0.062 if STUDY else 0.054, 0.006 if t == 2.59 else (0.068 if STUDY else 0.074), 0.035] for t in times]
    animation('Idle', times, [(0, 'scale', scales)] + [(eye, 'scale', blink) for eye in eyes])
    angles = [0, -0.65, -0.25, -0.65, -0.2, 0]
    rotations = [[0, 0, math.sin(a / 2), math.cos(a / 2)] for a in angles]
    animation('Greeting', [0, 0.3, 0.6, 0.9, 1.2, 1.5], [(paw, 'rotation', rotations)])

    doc['buffers'] = [{'byteLength': len(binary)}]
    while len(binary) % 4:
        binary.append(0)
    encoded = json.dumps(doc, separators=(',', ':')).encode()
    encoded += b' ' * (-len(encoded) % 4)
    length = 12 + 8 + len(encoded) + 8 + len(binary)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_bytes(struct.pack('<4sII', b'glTF', 2, length) + struct.pack('<I4s', len(encoded), b'JSON') +
                       encoded + struct.pack('<I4s', len(binary), b'BIN\x00') + binary)
    print(f'{OUTPUT}: {length} bytes, {len(doc["nodes"])} nodes, smooth body/spheres/smile, Idle/Greeting')


if __name__ == '__main__':
    build()
