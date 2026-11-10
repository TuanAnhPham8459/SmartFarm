#!/usr/bin/env python3
"""BME688 + VEML7700 + RFID + SSD1305 OLED + Cloud Firestore; no LED control.
Python 3.7+ compatible. No Sense HAT.
"""
import signal
import os
from pathlib import Path
import socket
import threading
import time
from dataclasses import dataclass, field
from typing import Optional

import board
import busio
import digitalio
import adafruit_bme680
import adafruit_veml7700
import adafruit_ssd1305
from PIL import Image, ImageDraw, ImageFont
from smbus2 import SMBus
from firebase_admin import firestore

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



# Firestore destination: collection / document / subcollection / document.
# Adjust FIRESTORE_READING_DOC if your Android app expects a different document ID.
FIRESTORE_DEVICE_ID = os.getenv('FIRESTORE_DEVICE_ID', 'AA:BB:CC:DD:EE:FF')

SERVICE_ACCOUNT_FILE = os.getenv(
    'GOOGLE_APPLICATION_CREDENTIALS',
    str(Path(__file__).resolve().parent / 'smartfarm-ceng322-8-firebase-adminsdk-fbsvc-2f310ee9ae.json')
)

stop_event = threading.Event()
i2c_lock = threading.Lock()

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

state = State()

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

def rfid_worker(bus):
    previous_uid = None
    while not stop_event.is_set():
        try:
            with i2c_lock:
                data = bus.read_i2c_block_data(RFID_ADDR, 0x00, 16)
            uid_bytes = data[:6]
            if len(uid_bytes) == 6 and all(x == 0 for x in uid_bytes):
                with state.lock:
                    state.tag_present = False
                    state.uid = ''
                    state.rfid_error = ''
                previous_uid = None
            elif len(uid_bytes) == 6 and any(uid_bytes):
                uid = ''.join('%02X' % x for x in uid_bytes)
                with state.lock:
                    if uid != previous_uid:
                        print('RFID tag detected:', uid)
                    state.tag_present = True
                    state.uid = uid
                    state.rfid_error = ''
                previous_uid = uid
        except OSError as exc:
            with state.lock:
                state.rfid_error = str(exc)
        stop_event.wait(RFID_SECONDS)


def firestore_worker():
    """Upload PCB sensor readings to Cloud Firestore every 60 seconds."""
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore

        if not os.path.isfile(SERVICE_ACCOUNT_FILE):
            print("Firestore disabled: service account JSON not found:",
                  SERVICE_ACCOUNT_FILE)
            return

        if not firebase_admin._apps:
            firebase_admin.initialize_app(
                credentials.Certificate(SERVICE_ACCOUNT_FILE)
            )

        client = firestore.client()

        readings_ref = (
            client.collection("sensor_data")
                  .document(FIRESTORE_DEVICE_ID)
                  .collection("readings")
        )

        print("Firestore upload thread started")
        print(
    "Collection:",
    "sensor_data/{}/readings".format(FIRESTORE_DEVICE_ID)
)

        while not stop_event.is_set():
            try:
                with state.lock:
                    snapshot = {
                        "bme688": {
                            "temperature": round(state.temperature, 2)
                            if state.temperature is not None else None,

                            "humidity": round(state.humidity, 2)
                            if state.humidity is not None else None,

                            "pressure": round(state.pressure, 2)
                            if state.pressure is not None else None,

                            "gas_resistance": round(state.gas, 2)
                            if state.gas is not None else None,
                        },

                        "veml7700": {
                            "lux": round(state.lux, 2)
                            if state.lux is not None else None,

                            "ambient": round(state.ambient, 2)
                            if state.ambient is not None else None,

                            "light_mode": state.light_mode,
                        },

                        "rfid": {
                            "tag_present": state.tag_present,
                            "uid": state.uid,
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
                    # Generate document ID using Unix milliseconds
                    timestamp = int(time.time() * 1000)

                    snapshot["timestamp"] = timestamp
                    snapshot["updated_at"] = firestore.SERVER_TIMESTAMP

                    doc_ref = readings_ref.document(str(timestamp))

                    doc_ref.set(snapshot, timeout=8)

                    print("Firestore upload successful!")
                    print("Document:", doc_ref.path)
                    print("Timestamp:", timestamp)

                else:
                    print("Firestore: waiting for valid sensor readings")

            except Exception as exc:
                print("Firestore upload error:", exc)

            stop_event.wait(FIREBASE_SECONDS)

    except Exception as exc:
        print("Firestore setup error:", exc)

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
                page = (page + 1) % 5
                next_page = time.monotonic() + PAGE_SECONDS
            with state.lock:
                t, h, p, g = state.temperature, state.humidity, state.pressure, state.gas
                lux, ambient, mode = state.lux, state.ambient, state.light_mode
                present, uid = state.tag_present, state.uid
                be, ve, re = state.bme_error, state.veml_error, state.rfid_error
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
                title = 'RFID READER'
                line1 = 'TAG PRESENT' if present else 'NO TAG'
                line2 = ('UID:' + uid) if present else ('Read error' if re else 'Waiting for tag')
            else:
                title = 'PCB SYSTEM'
                line1 = 'B:%s V:%s R:%s' % ('ERR' if be else 'OK', 'ERR' if ve else 'OK', 'ERR' if re else 'OK')
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

def main():
    signal.signal(signal.SIGINT, lambda *_: stop_event.set())
    signal.signal(signal.SIGTERM, lambda *_: stop_event.set())
    bus = None
    i2c = None
    reset = None
    threads = []
    try:
        i2c = busio.I2C(board.SCL, board.SDA)
        bus = SMBus(1)
        reset = digitalio.DigitalInOut(board.D4)
        with i2c_lock:
            bme = adafruit_bme680.Adafruit_BME680_I2C(i2c, address=BME_ADDR)
            veml = adafruit_veml7700.VEML7700(i2c)
            oled = adafruit_ssd1305.SSD1305_I2C(128, 32, i2c, addr=OLED_ADDR, reset=reset)
            oled.fill(0)
            oled.show()
        tasks = [(bme_worker, (bme,), 'BME688'),
                 (veml_worker, (veml,), 'VEML7700'),
                 (rfid_worker, (bus,), 'RFID'),
                 (oled_worker, (oled,), 'OLED'),
                 (firestore_worker, (), 'Firestore')]
        for target, args, name in tasks:
            thread = threading.Thread(target=target, args=args, name=name)
            thread.start()
            threads.append(thread)
        print('BME688 + VEML7700 + RFID + OLED + Firestore running. Ctrl+C to stop.')
        while not stop_event.wait(0.5):
            if not all(thread.is_alive() for thread in threads):
                print('Worker exited unexpectedly; stopping.')
                break
    finally:
        stop_event.set()
        for thread in threads:
            thread.join(timeout=4)
        if any(thread.is_alive() for thread in threads):
            print('WARNING: worker still running; restart Pi before rerunning.')
        else:
            if bus is not None:
                bus.close()
            if reset is not None:
                reset.deinit()
            if i2c is not None:
                i2c.deinit()
        print('Shutdown complete.')

if __name__ == '__main__':
    main()
