#!/usr/bin/env python3
"""BME688 + VEML7700 + OPT4048 color + RFID + SSD1305 OLED + Cloud Firestore.

Merged from pcb_sensors_oled_firestore.py and TruongAnNguyen-OPT4048.py.

    python3 pcb_sensors_color_oled_firestore.py            # run everything
    python3 pcb_sensors_color_oled_firestore.py calibrate  # color calibration only
                                                           # (stop the normal run first)

Python 3.7+ compatible. No Sense HAT. All GPIO goes through Blinka (digitalio),
so RPi.GPIO is NOT used any more (mixing it with digitalio and calling
GPIO.cleanup() could release the OLED reset pin).
"""
import json
import math
import os
import signal
import socket
import sys
import threading
import time
from collections import deque
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

import board
import busio
import digitalio
import adafruit_bme680
import adafruit_veml7700
import adafruit_ssd1305
from PIL import Image, ImageDraw, ImageFont
from smbus2 import SMBus

# The color sensor is optional: if its library is missing or the chip is not
# found, everything else keeps running.
try:
    import adafruit_opt4048
    from adafruit_opt4048 import ConversionTime, Mode, Range
    OPT_IMPORT_ERROR = ''
except ImportError as _exc:
    adafruit_opt4048 = None
    OPT_IMPORT_ERROR = str(_exc)

# ---------------- Addresses / timing (unchanged from PCB script) ----------------
RFID_ADDR = 0x15
BME_ADDR = 0x77       # Change to 0x76 if detected there
VEML_ADDR = 0x10
OLED_ADDR = 0x3C
PAGE_SECONDS = 5.0
BME_SECONDS = 2.0
VEML_SECONDS = 0.2
RFID_SECONDS = 0.15
OLED_SECONDS = 0.5
FIREBASE_SECONDS = 60.0
OLED_PAGES = 6
RFID_ABSENT_READS = 3     # empty reads in a row before "tag removed" (debounce)
RFID_ERROR_READS = 5      # failed reads in a row before the tag state is cleared
MIN_EVENT_GAP = 1.0       # seconds; coalesces bursts of RFID changes into one upload

# ---------------- Color sensor / LED config (from OPT4048 script) ----------------
LED_PIN = board.D17            # BCM 17, same pin as before
LED_BLINK_BELOW = 300          # blink LED when OPT4048 lux is below this
CAL_FILE = str(Path(__file__).resolve().parent / 'color_calibration_v4.json')
COLORS = ['GREEN', 'YELLOW', 'BROWN', 'RED']
POSITIONS = 3
COLOR_SECONDS = 1.0
COLOR_DEBUG = False            # set True for the old per-second [DEBUG] prints

# Points: [G/R, B/R, log10(lux+1)]
DEFAULT_REFS = {
    'GREEN':  [[0.7915, 0.4123, 2.389]],
    'YELLOW': [[0.7354, 0.2960, 2.569]],
    'BROWN':  [[0.7273, 0.3955, 2.444]],
    'RED':    [[0.6522, 0.3696, 2.324]],
}
MIN_LUX = 20
MAX_LUX = 1000
LUX_WEIGHT = 0.10   # how much brightness counts in the match
MAX_DIST = 0.05     # farther than this from every point -> UNKNOWN
SAMPLES = 12

# ---------------- Firestore destination ----------------
# collection / document / subcollection / document
FIRESTORE_DEVICE_ID = os.getenv('FIRESTORE_DEVICE_ID', 'AA:BB:CC:DD:EE:FF')
SERVICE_ACCOUNT_FILE = os.getenv(
    'GOOGLE_APPLICATION_CREDENTIALS',
    str(Path(__file__).resolve().parent / 'smartfarm-ceng322-8-firebase-adminsdk-fbsvc-2f310ee9ae.json')
)

stop_event = threading.Event()
i2c_lock = threading.Lock()   # ONE lock for every device on the I2C bus
upload_now = threading.Event()  # set by the RFID worker to trigger an immediate upload


@dataclass
class State:
    lock: threading.Lock = field(default_factory=threading.Lock)
    temperature: Optional[float] = None
    humidity: Optional[float] = None
    pressure: Optional[float] = None
    gas: Optional[float] = None
    lux: Optional[float] = None
    ambient: Optional[float] = None
    light_mode: str = 'ERROR'
    bme_error: str = 'Waiting for BME688'
    veml_error: str = 'Waiting for VEML7700'
    rfid_error: str = ''
    tag_present: bool = False
    uid: str = ''
    # OPT4048 color sensor
    color_r: Optional[float] = None
    color_g: Optional[float] = None
    color_b: Optional[float] = None
    color_lux: Optional[float] = None
    color: str = 'UNKNOWN'
    color_error: str = 'Waiting for OPT4048'


state = State()


def rnd(value, digits=2):
    return None if value is None else round(value, digits)


# ---------------- Color helpers ----------------
def feat(r, g, b, lux):
    if r <= 0:
        return None
    return (g / r, b / r, math.log10(lux + 1))


def dist(a, b):
    return math.sqrt((a[0] - b[0]) ** 2 +
                     (a[1] - b[1]) ** 2 +
                     (LUX_WEIGHT * (a[2] - b[2])) ** 2)


def median(values):
    v = sorted(values)
    return v[len(v) // 2]


def load_refs():
    try:
        with open(CAL_FILE) as fh:
            print('Color: using saved calibration.')
            return json.load(fh)
    except FileNotFoundError:
        print('Color: using built-in default values.')
        return DEFAULT_REFS


def classify(r, g, b, lux, refs):
    if lux < MIN_LUX or lux > MAX_LUX:
        return 'UNKNOWN', {}
    f = feat(r, g, b, lux)
    if f is None:
        return 'UNKNOWN', {}
    dists = {name: min(dist(f, p) for p in pts) for name, pts in refs.items()}
    best = min(dists, key=dists.get)
    if dists[best] > MAX_DIST:
        return 'UNKNOWN', dists
    return best, dists


def init_opt4048(i2c):
    if adafruit_opt4048 is None:
        raise RuntimeError('adafruit_opt4048 not available: ' + OPT_IMPORT_ERROR)
    sensor = adafruit_opt4048.OPT4048(i2c)
    sensor.range = Range.AUTO
    sensor.conversion_time = ConversionTime.TIME_100MS
    sensor.mode = Mode.CONTINUOUS
    time.sleep(1)
    return sensor


# ---------------- Light-mode helper (VEML7700) ----------------
def light_mode(lux):
    if lux is None:
        return 'ERROR'
    if lux > 300:
        return 'FLASHLIGHT'
    if lux < 5:
        return 'DARK'
    if lux > 50:
        return 'NORMAL'
    return 'HEARTBEAT'


# ---------------- Workers ----------------
def bme_worker(sensor):
    while not stop_event.is_set():
        try:
            with i2c_lock:
                t = float(sensor.temperature)
                h = float(sensor.relative_humidity)
                p = float(sensor.pressure)
                g = float(sensor.gas)
            with state.lock:
                state.temperature, state.humidity = t, h
                state.pressure, state.gas = p, g
                state.bme_error = ''
            print('BME688: %.1f C | %.1f%% | %.1f hPa | %.0f ohm' % (t, h, p, g))
        except Exception as exc:
            with state.lock:
                state.bme_error = str(exc)
            print('BME688 error:', exc)
        stop_event.wait(BME_SECONDS)


def veml_worker(sensor):
    count = 0
    previous_mode = None
    while not stop_event.is_set():
        try:
            with i2c_lock:
                lux = float(sensor.lux)
                ambient = float(sensor.light)
            mode = light_mode(lux)
            with state.lock:
                state.lux = lux
                state.ambient = ambient
                state.light_mode = mode
                state.veml_error = ''
            if mode != previous_mode:
                print('VEML7700 mode:', mode)
                previous_mode = mode
            count += 1
            if count % 5 == 0:
                print('VEML7700: %.2f lux | ambient %.0f | %s' % (lux, ambient, mode))
            if count % 100 == 0:
                try:
                    with open('veml7700_debug_log.txt', 'a') as log:
                        log.write('%s, %.2f, %.0f, %s\n' %
                                  (time.strftime('%Y-%m-%d %H:%M:%S'), lux, ambient, mode))
                except OSError as exc:
                    print('VEML7700 log error:', exc)
        except Exception as exc:
            with state.lock:
                state.lux = None
                state.ambient = None
                state.light_mode = 'ERROR'
                state.veml_error = str(exc)
            if previous_mode != 'ERROR':
                print('VEML7700 error:', exc)
            previous_mode = 'ERROR'
        stop_event.wait(VEML_SECONDS)


def color_worker(sensor, refs):
    """Read the OPT4048, median-filter the last 5 samples, classify the color."""
    history = deque(maxlen=5)
    previous = None
    while not stop_event.is_set():
        try:
            with i2c_lock:
                r, g, b, lux = sensor.rgb
            history.append((r, g, b, lux))
            mr = median([x[0] for x in history])
            mg = median([x[1] for x in history])
            mb = median([x[2] for x in history])
            ml = median([x[3] for x in history])
            condition, dists = classify(mr, mg, mb, ml, refs)
            with state.lock:
                state.color_r, state.color_g, state.color_b = r, g, b
                state.color_lux = lux
                state.color = condition
                state.color_error = ''
            if COLOR_DEBUG:
                d = ' '.join('%s=%.3f' % (k, v) for k, v in dists.items())
                print('[DEBUG] lux=%.1f %s -> %s' % (ml, d, condition))
            elif condition != previous:
                print('Color:', condition)
            previous = condition
        except Exception as exc:
            with state.lock:
                state.color = 'UNKNOWN'
                state.color_lux = None      # LED goes off while the sensor is bad
                state.color_error = str(exc)
            print('OPT4048 error:', exc)
            history.clear()
            previous = None
            stop_event.wait(2)
            try:
                with i2c_lock:
                    sensor.mode = Mode.CONTINUOUS
            except Exception as exc2:
                print('OPT4048 restart failed:', exc2)
        stop_event.wait(COLOR_SECONDS)


def led_worker(led):
    """Blink the LED while the OPT4048 sees less than LED_BLINK_BELOW lux."""
    blink = False
    try:
        while not stop_event.is_set():
            with state.lock:
                lux = state.color_lux
            if lux is not None and lux < LED_BLINK_BELOW:
                blink = not blink
                led.value = blink
            else:
                led.value = False
                blink = False
            stop_event.wait(0.5)
    finally:
        try:
            led.value = False
        except Exception:
            pass


def rfid_worker(bus):
    """Poll the RFID reader. Any change in tag/UID wakes the Firestore uploader."""
    empty = 0
    errors = 0

    def publish(present, uid, error):
        with state.lock:
            changed = (state.tag_present, state.uid) != (present, uid)
            state.tag_present, state.uid = present, uid
            state.rfid_error = error
        if changed:
            print('RFID tag detected:' if present else 'RFID tag removed', uid)
            upload_now.set()

    while not stop_event.is_set():
        try:
            with i2c_lock:
                data = bus.read_i2c_block_data(RFID_ADDR, 0x00, 16)
            errors = 0
            uid_bytes = data[:6]
            if len(uid_bytes) == 6 and any(uid_bytes):
                empty = 0
                publish(True, ''.join('%02X' % x for x in uid_bytes), '')
            else:
                empty += 1
                if empty >= RFID_ABSENT_READS:      # debounce brief dropouts
                    publish(False, '', '')
        except OSError as exc:
            errors += 1
            with state.lock:
                state.rfid_error = str(exc)
            if errors == RFID_ERROR_READS:
                # Reader keeps failing: do not keep reporting a stale tag.
                publish(False, '', str(exc))
        stop_event.wait(RFID_SECONDS)


def wait_for_upload(deadline):
    """Block until an RFID change, the deadline, or shutdown.
    Returns 'rfid_change', 'periodic', or None when stopping."""
    while not stop_event.is_set():
        if upload_now.is_set():
            upload_now.clear()
            return 'rfid_change'
        if time.monotonic() >= deadline:
            return 'periodic'
        stop_event.wait(0.2)
    return None


def firestore_worker():
    """Upload readings every FIREBASE_SECONDS and immediately on RFID changes."""
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore

        if not os.path.isfile(SERVICE_ACCOUNT_FILE):
            print('Firestore disabled: service account JSON not found:',
                  SERVICE_ACCOUNT_FILE)
            return

        if not firebase_admin._apps:
            firebase_admin.initialize_app(credentials.Certificate(SERVICE_ACCOUNT_FILE))

        client = firestore.client()
        readings_ref = (client.collection('sensor_data')
                              .document(FIRESTORE_DEVICE_ID)
                              .collection('readings'))

        print('Firestore upload thread started')
        print('Collection:', 'sensor_data/{}/readings'.format(FIRESTORE_DEVICE_ID))

        deadline = time.monotonic()          # first upload right away
        while not stop_event.is_set():
            trigger = wait_for_upload(deadline)
            if trigger is None:
                break
            retry_in = FIREBASE_SECONDS
            try:
                with state.lock:
                    snapshot = {
                        'bme688': {
                            'temperature': rnd(state.temperature),
                            'humidity': rnd(state.humidity),
                            'pressure': rnd(state.pressure),
                            'gas_resistance': rnd(state.gas),
                        },
                        'veml7700': {
                            'lux': rnd(state.lux),
                            'ambient': rnd(state.ambient),
                            'light_mode': state.light_mode,
                        },
                        'rfid': {
                            'tag_present': state.tag_present,
                            'uid': state.uid,
                            'read_error': bool(state.rfid_error),   # NEW, additive
                        },
                        'opt4048': {                                # NEW, additive
                            'r': rnd(state.color_r),
                            'g': rnd(state.color_g),
                            'b': rnd(state.color_b),
                            'lux': rnd(state.color_lux),
                            'color': state.color,
                        },
                    }
                    ready = (
                        not state.bme_error
                        and not state.veml_error
                        and state.temperature is not None
                        and state.humidity is not None
                        and state.pressure is not None
                        and state.gas is not None
                        and state.lux is not None
                        and state.ambient is not None
                    )

                if ready:
                    timestamp = int(time.time() * 1000)   # Unix ms as document ID
                    snapshot['timestamp'] = timestamp
                    snapshot['trigger'] = trigger          # NEW: 'periodic' / 'rfid_change'
                    snapshot['updated_at'] = firestore.SERVER_TIMESTAMP
                    doc_ref = readings_ref.document(str(timestamp))
                    doc_ref.set(snapshot, timeout=8)
                    print('Firestore upload successful! (%s)' % trigger)
                    print('Document:', doc_ref.path)
                else:
                    print('Firestore: waiting for valid sensor readings')
                    retry_in = 5.0                         # do not lose an RFID change

            except Exception as exc:
                print('Firestore upload error:', exc)
                retry_in = 5.0

            deadline = time.monotonic() + retry_in
            stop_event.wait(MIN_EVENT_GAP)                 # coalesce bursts

    except Exception as exc:
        print('Firestore setup error:', exc)


def local_ip():
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
            sock.settimeout(0.5)
            sock.connect(('1.1.1.1', 53))
            return sock.getsockname()[0]
    except OSError:
        return 'Unavailable'


def fmt(value, digits=1):
    return 'N/A' if value is None else ('%.*f' % (digits, value))


def oled_worker(display):
    image = Image.new('1', (128, 32))
    draw = ImageDraw.Draw(image)
    font = ImageFont.load_default()
    page = 0
    next_page = time.monotonic() + PAGE_SECONDS
    try:
        while not stop_event.is_set():
            if time.monotonic() >= next_page:
                page = (page + 1) % OLED_PAGES
                next_page = time.monotonic() + PAGE_SECONDS
            with state.lock:
                t, h, p, g = state.temperature, state.humidity, state.pressure, state.gas
                lux, ambient, mode = state.lux, state.ambient, state.light_mode
                present, uid = state.tag_present, state.uid
                be, ve, re = state.bme_error, state.veml_error, state.rfid_error
                color, clux, ce = state.color, state.color_lux, state.color_error
            if page == 0:
                title = 'BME688 ENVIRONMENT'
                line1 = 'T:%sC H:%s%%' % (fmt(t), fmt(h))
                line2 = 'P:%s hPa' % fmt(p)
            elif page == 1:
                title = 'BME688 GAS'
                line1 = 'Gas:%s ohm' % fmt(g, 0)
                line2 = 'OK' if not be else 'Sensor read error'
            elif page == 2:
                title = 'VEML7700 LIGHT'
                line1 = 'Lux:%s A:%s' % (fmt(lux), fmt(ambient, 0))
                line2 = 'Mode:' + mode
            elif page == 3:
                title = 'OPT4048 COLOR'
                line1 = 'Color:' + color
                line2 = ('Lux:%s' % fmt(clux)) if not ce else 'Sensor read error'
            elif page == 4:
                title = 'RFID READER'
                line1 = 'TAG PRESENT' if present else 'NO TAG'
                line2 = ('UID:' + uid) if present else ('Read error' if re else 'Waiting for tag')
            else:
                title = 'PCB SYSTEM'
                line1 = 'B:%s V:%s R:%s C:%s' % ('ERR' if be else 'OK', 'ERR' if ve else 'OK',
                                                 'ERR' if re else 'OK', 'ERR' if ce else 'OK')
                line2 = 'IP:' + local_ip()
            draw.rectangle((0, 0, 127, 31), fill=0)
            draw.rectangle((0, 0, 127, 9), fill=1)
            draw.text((1, 0), title[:21], font=font, fill=0)
            draw.text((1, 10), line1[:21], font=font, fill=1)
            draw.text((1, 20), line2[:21], font=font, fill=1)
            with i2c_lock:
                display.image(image)
                display.show()
            stop_event.wait(OLED_SECONDS)
    finally:
        try:
            with i2c_lock:
                display.fill(0)
                display.show()
        except Exception:
            pass


# ---------------- Calibration mode (run alone, no other threads) ----------------
def calibrate():
    i2c = busio.I2C(board.SCL, board.SDA)
    led = digitalio.DigitalInOut(LED_PIN)
    led.direction = digitalio.Direction.OUTPUT
    led.value = False
    try:
        sensor = init_opt4048(i2c)
        refs = {}
        print('CALIBRATION - vary the distance a little for each point.')
        for name in COLORS:
            refs[name] = []
            for k in range(1, POSITIONS + 1):
                input('\n%s - point %d/%d: place it, press Enter...' % (name, k, POSITIONS))
                vals = []
                for _ in range(SAMPLES):
                    r, g, b, lux = sensor.rgb
                    f = feat(r, g, b, lux)
                    if f and lux >= MIN_LUX:
                        vals.append(f)
                    time.sleep(0.4)
                if not vals:
                    print('  No valid readings, skipped.')
                    continue
                p = [round(sum(v[i] for v in vals) / len(vals), 4) for i in range(3)]
                refs[name].append(p)
                print('  G/R=%s  B/R=%s  lux~%.0f' % (p[0], p[1], 10 ** p[2] - 1))

        names = [n for n in refs if refs[n]]
        for i in range(len(names)):
            for j in range(i + 1, len(names)):
                d = min(dist(a, b) for a in refs[names[i]] for b in refs[names[j]])
                print('Closest %s vs %s: %.3f' % (names[i], names[j], d))

        with open(CAL_FILE, 'w') as fh:
            json.dump(refs, fh, indent=2)
        print('\nSaved to %s.' % CAL_FILE)
    finally:
        led.value = False
        led.deinit()
        i2c.deinit()


# ---------------- Normal run ----------------
def main():
    signal.signal(signal.SIGINT, lambda *_: stop_event.set())
    signal.signal(signal.SIGTERM, lambda *_: stop_event.set())
    bus = None
    i2c = None
    reset = None
    led = None
    opt = None
    threads = []
    try:
        i2c = busio.I2C(board.SCL, board.SDA)      # the only busio.I2C instance
        bus = SMBus(1)
        reset = digitalio.DigitalInOut(board.D4)
        with i2c_lock:
            bme = adafruit_bme680.Adafruit_BME680_I2C(i2c, address=BME_ADDR)
            veml = adafruit_veml7700.VEML7700(i2c)
            oled = adafruit_ssd1305.SSD1305_I2C(128, 32, i2c, addr=OLED_ADDR, reset=reset)
            oled.fill(0)
            oled.show()
            try:
                opt = init_opt4048(i2c)
            except Exception as exc:
                opt = None
                with state.lock:
                    state.color_error = 'init failed: %s' % exc
                print('OPT4048 disabled, continuing without color:', exc)

        tasks = [(bme_worker, (bme,), 'BME688'),
                 (veml_worker, (veml,), 'VEML7700'),
                 (rfid_worker, (bus,), 'RFID'),
                 (oled_worker, (oled,), 'OLED'),
                 (firestore_worker, (), 'Firestore')]
        if opt is not None:
            led = digitalio.DigitalInOut(LED_PIN)
            led.direction = digitalio.Direction.OUTPUT
            led.value = False
            tasks += [(color_worker, (opt, load_refs()), 'Color'),
                      (led_worker, (led,), 'LED')]

        for target, args, name in tasks:
            thread = threading.Thread(target=target, args=args, name=name)
            thread.start()
            threads.append(thread)
        print('BME688 + VEML7700 + OPT4048 + RFID + OLED + Firestore running. Ctrl+C to stop.')
        while not stop_event.wait(0.5):
            if not all(thread.is_alive() for thread in threads):
                print('Worker exited unexpectedly; stopping.')
                break
    finally:
        stop_event.set()
        for thread in threads:
            thread.join(timeout=4)
        if led is not None:
            try:
                led.value = False
            except Exception:
                pass
        if any(thread.is_alive() for thread in threads):
            print('WARNING: worker still running; restart Pi before rerunning.')
        else:
            if led is not None:
                led.deinit()
            if bus is not None:
                bus.close()
            if reset is not None:
                reset.deinit()
            if i2c is not None:
                i2c.deinit()
        print('Shutdown complete.')


if __name__ == '__main__':
    if len(sys.argv) > 1 and sys.argv[1] == 'calibrate':
        calibrate()
    else:
        main()
