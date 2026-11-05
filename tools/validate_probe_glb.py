"""Structural checks for the bundled probe; does not replace glTF Validator or device rendering."""
import json
import math
import struct
from pathlib import Path

MODEL = Path(__file__).resolve().parents[1] / 'app/src/debug/assets/models/companion-probe.glb'


def validate():
    raw = MODEL.read_bytes()
    magic, version, length = struct.unpack_from('<4sII', raw)
    assert (magic, version, length) == (b'glTF', 2, len(raw))
    json_size, json_type = struct.unpack_from('<I4s', raw, 12)
    assert json_type == b'JSON' and json_size % 4 == 0
    doc = json.loads(raw[20:20 + json_size])
    offset = 20 + json_size
    binary_size, binary_type = struct.unpack_from('<I4s', raw, offset)
    assert binary_type == b'BIN\x00' and offset + 8 + binary_size == len(raw)
    binary = raw[offset + 8:]
    assert len(doc['buffers']) == 1 and 'uri' not in doc['buffers'][0]
    assert doc['buffers'][0]['byteLength'] <= len(binary)
    assert 'images' not in doc and 'extensionsRequired' not in doc

    def read(index):
        a = doc['accessors'][index]
        view = doc['bufferViews'][a['bufferView']]
        count = {'SCALAR': 1, 'VEC3': 3, 'VEC4': 4}[a['type']]
        code, size = {5126: ('f', 4), 5123: ('H', 2)}[a['componentType']]
        begin = view.get('byteOffset', 0) + a.get('byteOffset', 0)
        end = begin + a['count'] * count * size
        assert begin % size == 0 and view.get('byteOffset', 0) % 4 == 0
        assert end <= view.get('byteOffset', 0) + view['byteLength'] <= doc['buffers'][0]['byteLength']
        values = struct.unpack_from('<' + code * (a['count'] * count), binary, begin)
        assert all(math.isfinite(x) for x in values)
        return [values[i:i + count] for i in range(0, len(values), count)]

    for i in range(len(doc['accessors'])):
        read(i)
    for mesh in doc['meshes']:
        for primitive in mesh['primitives']:
            points = read(primitive['attributes']['POSITION'])
            normals = read(primitive['attributes']['NORMAL'])
            indices = [i[0] for i in read(primitive['indices'])]
            assert len(points) == len(normals) and len(indices) % 3 == 0
            assert all(0 <= i < len(points) for i in indices)
            assert all(abs(sum(x*x for x in n) - 1) < 1e-5 for n in normals)
            for start in range(0, len(indices), 3):
                tri_indices = indices[start:start+3]
                a, b, c = [points[i] for i in tri_indices]
                ab = [b[i]-a[i] for i in range(3)]
                ac = [c[i]-a[i] for i in range(3)]
                cross = [ab[1]*ac[2]-ab[2]*ac[1], ab[2]*ac[0]-ab[0]*ac[2], ab[0]*ac[1]-ab[1]*ac[0]]
                normal = [sum(normals[v][i] for v in tri_indices) for i in range(3)]
                assert sum(cross[i] * normal[i] for i in range(3)) > 0, 'Winding disagrees with vertex normals'
    assert [a['name'] for a in doc['animations']] == ['Idle', 'Greeting']
    for animation in doc['animations']:
        targets = set()
        for channel in animation['channels']:
            target = channel['target']
            assert 0 <= target['node'] < len(doc['nodes'])
            identity = (target['node'], target['path'])
            assert identity not in targets
            targets.add(identity)
            sampler = animation['samplers'][channel['sampler']]
            times, values = read(sampler['input']), read(sampler['output'])
            assert len(times) == len(values) and times[0][0] == 0
            assert all(a[0] < b[0] for a, b in zip(times, times[1:]))
            if target['path'] == 'rotation':
                assert all(abs(sum(x*x for x in q)-1) < 1e-5 for q in values)
            if target['path'] == 'scale':
                assert all(x > 0 for row in values for x in row)
    print(f'PASS: GLB header, bounds, mesh winding, normals, animation times/targets/quaternions ({len(raw)} bytes)')


if __name__ == '__main__':
    validate()
