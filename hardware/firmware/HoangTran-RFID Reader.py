#!/usr/bin/env python3

from smbus2 import SMBus
import RPi.GPIO as GPIO
import time
import threading


I2C_BUS = 1
RFID_ADDR = 0x15

LED_PIN = 17


IDLE_DELAY = 1.0        # No tag: blink every 1 second

TAG_BLINK_COUNT = 5     # Tag detected: blink 5 times
TAG_BLINK_DELAY = 0.2   # Fast blink speed
# -----------------------------
# GPIO setup
# -----------------------------
GPIO.setwarnings(False)
GPIO.setmode(GPIO.BCM)
GPIO.setup(
    LED_PIN,
    GPIO.OUT,
    initial=GPIO.LOW
)

bus = SMBus(I2C_BUS)

tag_present = False
new_tag = False

lock = threading.Lock()

running = True


def is_removal_signal(data):

    return (
        len(data) >= 6
        and all(b == 0 for b in data[:6])
    )

def rfid_thread():

    global tag_present
    global new_tag
    previous_uid = None
    print("RFID thread started")
    while running:
        try:
            data = bus.read_i2c_block_data(
                RFID_ADDR,
                0x00,
                16
            )
            # No card
            if is_removal_signal(data):

                with lock:
                    tag_present = False
            else:

                uid = data[:6]
                if any(uid):
                    with lock:
                        # New card detected
                        if not tag_present:
                            tag_present = True
                            new_tag = True

                            print("\nTag detected!")
                            print("UID:", uid)

        except OSError:
            pass
# -----------------------------
# LED Thread
# -----------------------------
def led_thread():
    global new_tag
    print("\nLED thread started")
    while running:
        with lock:
            card = tag_present
            detected = new_tag
            if detected:
                new_tag = False
        # ----------------------
        # Tag detected
        # ----------------------
        if detected:

            for _ in range(TAG_BLINK_COUNT):

                GPIO.output(
                    LED_PIN,
                    GPIO.HIGH
                )

                time.sleep(TAG_BLINK_DELAY)
                GPIO.output(
                    LED_PIN,
                    GPIO.LOW
                )
                time.sleep(TAG_BLINK_DELAY)
                
            GPIO.output(
                LED_PIN,
                GPIO.LOW
            )
        # ----------------------
        # No tag
        # ----------------------
        elif not card:
            GPIO.output(
                LED_PIN,
                GPIO.HIGH
            )

            time.sleep(IDLE_DELAY)
            GPIO.output(
                LED_PIN,
                GPIO.LOW
            )

            time.sleep(IDLE_DELAY)
        else:
            # Card remains on reader
            GPIO.output(
                LED_PIN,
                GPIO.LOW
            )

            time.sleep(0.1)
# -----------------------------
# Start threads
# -----------------------------
rfid = threading.Thread(
    target=rfid_thread
)
led = threading.Thread(
    target=led_thread
)
rfid.start()
led.start()
try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    print("\nStopping...")
    running = False
    rfid.join()
    led.join()
    GPIO.output(
        LED_PIN,
        GPIO.LOW
    )
    GPIO.cleanup()
    bus.close()
    print("Cleanup complete")