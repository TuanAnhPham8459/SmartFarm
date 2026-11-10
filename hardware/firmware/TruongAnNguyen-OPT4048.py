import board
import busio
import time
import sys
import json
import math
import threading
from collections import deque
import RPi.GPIO as GPIO
import adafruit_opt4048
from adafruit_opt4048 import ConversionTime, Mode, Range

# ---------------- Config ----------------
LED_PIN = 17
CAL_FILE = "color_calibration_v4.json"
COLORS = ["GREEN", "YELLOW", "BROWN", "RED"]
POSITIONS = 3

# Points: [G/R, B/R, log10(lux+1)]  (from your latest readings)
DEFAULT_REFS = {
    "GREEN":  [[0.7915, 0.4123, 2.389]],
    "YELLOW": [[0.7354, 0.2960, 2.569]],
    "BROWN":  [[0.7273, 0.3955, 2.444]],
    "RED":    [[0.6522, 0.3696, 2.324]],
}

MIN_LUX    = 20
MAX_LUX    = 1000
LUX_WEIGHT = 0.10   # how much brightness counts in the match
MAX_DIST   = 0.05   # farther than this from every point -> UNKNOWN
SAMPLES    = 12
DEBUG      = True

# ---------------- Hardware setup ----------------
GPIO.setwarnings(False)
GPIO.setmode(GPIO.BCM)
GPIO.setup(LED_PIN, GPIO.OUT)
GPIO.output(LED_PIN, GPIO.LOW)

i2c = busio.I2C(board.SCL, board.SDA)
sensor = adafruit_opt4048.OPT4048(i2c)
sensor.range = Range.AUTO
sensor.conversion_time = ConversionTime.TIME_100MS
sensor.mode = Mode.CONTINUOUS
time.sleep(1)


# ---------------- Helpers ----------------
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
            print("Using saved calibration.")
            return json.load(fh)
    except FileNotFoundError:
        print("Using built-in default values.")
        return DEFAULT_REFS


def classify(r, g, b, lux, refs):
    if lux < MIN_LUX or lux > MAX_LUX:
        return "UNKNOWN", {}
    f = feat(r, g, b, lux)
    if f is None:
        return "UNKNOWN", {}
    dists = {name: min(dist(f, p) for p in pts) for name, pts in refs.items()}
    best = min(dists, key=dists.get)
    if dists[best] > MAX_DIST:
        return "UNKNOWN", dists
    return best, dists


# ---------------- Calibration mode ----------------
def calibrate():
    GPIO.output(LED_PIN, GPIO.LOW)
    refs = {}
    print("CALIBRATION - vary the distance a little for each point.")
    for name in COLORS:
        refs[name] = []
        for k in range(1, POSITIONS + 1):
            input(f"\n{name} - point {k}/{POSITIONS}: place it, press Enter...")
            vals = []
            for _ in range(SAMPLES):
                r, g, b, lux = sensor.rgb
                f = feat(r, g, b, lux)
                if f and lux >= MIN_LUX:
                    vals.append(f)
                time.sleep(0.4)
            if not vals:
                print("  No valid readings, skipped.")
                continue
            p = [round(sum(v[i] for v in vals) / len(vals), 4) for i in range(3)]
            refs[name].append(p)
            print(f"  G/R={p[0]}  B/R={p[1]}  lux~{10 ** p[2] - 1:.0f}")

    names = [n for n in refs if refs[n]]
    for i in range(len(names)):
        for j in range(i + 1, len(names)):
            d = min(dist(a, b) for a in refs[names[i]] for b in refs[names[j]])
            print(f"Closest {names[i]} vs {names[j]}: {d:.3f}")

    with open(CAL_FILE, "w") as fh:
        json.dump(refs, fh, indent=2)
    print(f"\nSaved to {CAL_FILE}.")
    GPIO.cleanup()


# ---------------- Run mode ----------------
def run():
    refs = load_refs()
    sensor_data = {"r": 0, "g": 0, "b": 0, "brightness": 0, "condition": "UNKNOWN"}
    data_lock = threading.Lock()
    i2c_lock = threading.Lock()
    stop_threads = threading.Event()
    history = deque(maxlen=5)

    def read_sensor():
        while not stop_threads.is_set():
            try:
                with i2c_lock:
                    r, g, b, brightness = sensor.rgb
                with data_lock:
                    sensor_data.update(r=r, g=g, b=b, brightness=brightness)
            except (OSError, RuntimeError) as e:
                print("I2C Error:", e)
                with data_lock:
                    sensor_data["condition"] = "UNKNOWN"
                time.sleep(2)
                try:
                    with i2c_lock:
                        sensor.mode = Mode.CONTINUOUS
                except (OSError, RuntimeError) as e2:
                    print("Restart failed:", e2)
            time.sleep(1)

    def analyze_color():
        while not stop_threads.is_set():
            with data_lock:
                sample = (sensor_data["r"], sensor_data["g"],
                          sensor_data["b"], sensor_data["brightness"])
            history.append(sample)
            mr = median([x[0] for x in history])
            mg = median([x[1] for x in history])
            mb = median([x[2] for x in history])
            ml = median([x[3] for x in history])

            condition, dists = classify(mr, mg, mb, ml, refs)
            if DEBUG:
                d = " ".join(f"{k}={v:.3f}" for k, v in dists.items())
                print(f"[DEBUG] lux={ml:.1f} {d} -> {condition}")
            with data_lock:
                sensor_data["condition"] = condition
            time.sleep(1)

    def control_led():
        blink = False
        while not stop_threads.is_set():
            with data_lock:
                brightness = sensor_data["brightness"]
            if brightness < 300:
                blink = not blink
                GPIO.output(LED_PIN, GPIO.HIGH if blink else GPIO.LOW)
            else:
                GPIO.output(LED_PIN, GPIO.LOW)
                blink = False
            time.sleep(0.5)

    for fn in (read_sensor, analyze_color, control_led):
        threading.Thread(target=fn, daemon=True).start()

    try:
        while not stop_threads.is_set():
            with data_lock:
                print("----------------------")
                print("Red:", round(sensor_data["r"]))
                print("Green:", round(sensor_data["g"]))
                print("Blue:", round(sensor_data["b"]))
                print("Brightness:", round(sensor_data["brightness"], 2))
                print("Color:", sensor_data["condition"])
            time.sleep(1)
    except KeyboardInterrupt:
        print("\nStopping program...")
        stop_threads.set()
        time.sleep(0.5)
        GPIO.output(LED_PIN, GPIO.LOW)
        GPIO.cleanup()
        print("Cleanup complete")


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "calibrate":
        calibrate()
    else:
        run()
