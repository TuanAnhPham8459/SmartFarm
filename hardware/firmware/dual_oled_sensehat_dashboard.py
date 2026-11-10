#!/usr/bin/env python3
"""Multithreaded SSD1305 OLED + Sense HAT dashboard for Raspberry Pi 4.

Thread 1 owns the Adafruit 2.23-inch 128x32 OLED.
Thread 2 owns the Sense HAT sensors, joystick, and 8x8 LED matrix.
Thread 3 owns the external PCB LED on GPIO17.

Joystick:
    Left/right  Change OLED page
    Up/down     Change Sense HAT LED brightness
    Middle      Toggle automatic page rotation
"""

import os
import signal
import socket
import threading
import time
from dataclasses import dataclass, field

import psutil


OLED_WIDTH = 128
OLED_HEIGHT = 32
OLED_ADDRESS = 0x3C
OLED_REFRESH_SECONDS = 0.5
SENSOR_REFRESH_SECONDS = 0.5
AUTO_PAGE_SECONDS = 5.0
PAGE_COUNT = 4
PCB_LED_PIN = 17
PCB_LED_INTERVAL_SECONDS = 1.0


@dataclass
class SharedState:
    lock: threading.Lock = field(default_factory=threading.Lock)
    page: int = 0
    auto_page: bool = True
    last_manual_page_change: float = field(default_factory=time.monotonic)
    led_brightness: int = 1
    temperature: float = 0.0
    humidity: float = 0.0
    pressure: float = 0.0
    pitch: float = 0.0
    roll: float = 0.0
    yaw: float = 0.0
    online: bool = False
    oled_error: str = ""
    sense_error: str = ""


def cpu_temperature():
    try:
        with open(
            "/sys/class/thermal/thermal_zone0/temp", "r", encoding="utf-8"
        ) as temperature_file:
            return int(temperature_file.read().strip()) / 1000.0
    except (OSError, ValueError):
        return None


def local_ip():
    connection = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    connection.settimeout(0.5)
    try:
        connection.connect(("1.1.1.1", 53))
        return connection.getsockname()[0]
    except OSError:
        return "No connection"
    finally:
        connection.close()


def internet_available():
    try:
        connection = socket.create_connection(("1.1.1.1", 53), timeout=0.5)
        connection.close()
        return True
    except OSError:
        return False


def readable_uptime():
    seconds = int(time.time() - psutil.boot_time())
    days, seconds = divmod(seconds, 86400)
    hours, seconds = divmod(seconds, 3600)
    minutes = seconds // 60
    return f"{days}d {hours}h" if days else f"{hours}h {minutes}m"


def compact(text, length=21):
    return str(text)[:length]


def oled_worker(state, stop_event):
    """Own all OLED setup, drawing, and updates in one thread."""
    oled = None
    try:
        import board
        import busio
        import digitalio
        import adafruit_ssd1305
        from PIL import Image, ImageDraw, ImageFont

        reset_pin = digitalio.DigitalInOut(board.D4)
        i2c = busio.I2C(board.SCL, board.SDA)
        oled = adafruit_ssd1305.SSD1305_I2C(
            OLED_WIDTH,
            OLED_HEIGHT,
            i2c,
            addr=OLED_ADDRESS,
            reset=reset_pin,
        )

        image = Image.new("1", (OLED_WIDTH, OLED_HEIGHT))
        draw = ImageDraw.Draw(image)
        font = ImageFont.load_default()

        def render(title, first_line, second_line):
            draw.rectangle((0, 0, OLED_WIDTH - 1, OLED_HEIGHT - 1), fill=0)
            draw.rectangle((0, 0, OLED_WIDTH - 1, 9), fill=1)
            draw.text((2, 0), compact(title), font=font, fill=0)
            draw.text((1, 10), compact(first_line), font=font, fill=1)
            draw.text((1, 20), compact(second_line), font=font, fill=1)
            oled.image(image)
            oled.show()

        oled.fill(0)
        oled.show()
        render("DUAL DASHBOARD", "OLED thread: READY", "Sense thread: START")
        stop_event.wait(1.5)

        last_auto_change = time.monotonic()

        while not stop_event.is_set():
            now = time.monotonic()

            with state.lock:
                if state.auto_page and now - last_auto_change >= AUTO_PAGE_SECONDS:
                    state.page = (state.page + 1) % PAGE_COUNT
                    last_auto_change = now

                page = state.page
                temperature = state.temperature
                humidity = state.humidity
                pressure = state.pressure
                pitch = state.pitch
                roll = state.roll
                yaw = state.yaw
                sense_error = state.sense_error

            online = internet_available()
            with state.lock:
                state.online = online

            if sense_error:
                render("SENSE HAT ERROR", compact(sense_error), "Check terminal output")
            elif page == 0:
                cpu = psutil.cpu_percent(interval=None)
                ram = psutil.virtual_memory().percent
                pi_temp = cpu_temperature()
                temp_text = "N/A" if pi_temp is None else f"{pi_temp:.1f}C"
                render("PI SYSTEM", f"CPU:{cpu:.0f}% RAM:{ram:.0f}%", f"TEMP:{temp_text} UP:{readable_uptime()}")
            elif page == 1:
                render(
                    "ENVIRONMENT",
                    f"TEMP:{temperature:.1f}C HUM:{humidity:.0f}%",
                    f"PRESS:{pressure:.1f} hPa",
                )
            elif page == 2:
                render(
                    "ORIENTATION",
                    f"P:{pitch:.0f} R:{roll:.0f}",
                    f"YAW:{yaw:.0f} degrees",
                )
            else:
                status = "ONLINE" if online else "OFFLINE"
                render("NETWORK", f"IP:{local_ip()}", f"NET:{status}")

            stop_event.wait(OLED_REFRESH_SECONDS)

    except Exception as error:
        with state.lock:
            state.oled_error = str(error)
        print(f"OLED thread error: {error}")
    finally:
        if oled is not None:
            try:
                oled.fill(0)
                oled.show()
            except Exception:
                pass


def sense_hat_worker(state, stop_event):
    """Own all Sense HAT sensor, joystick, and LED matrix operations."""
    sense = None
    try:
        from sense_hat import SenseHat

        sense = SenseHat()
        sense.low_light = True
        sense.clear()

        green_check = [
            (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0),
            (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 100, 0), (0, 0, 0), (0, 0, 0),
            (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 100, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0),
            (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 100, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0),
            (0, 0, 0), (0, 100, 0), (0, 0, 0), (0, 100, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0),
            (0, 0, 0), (0, 0, 0), (0, 100, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0),
            (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0),
            (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0),
        ]

        red_x = []
        for y in range(8):
            for x in range(8):
                red_x.append((120, 0, 0) if x == y or x == 7 - y else (0, 0, 0))

        last_icon = None
        last_brightness = None

        while not stop_event.is_set():
            raw_temperature = sense.get_temperature()
            humidity = sense.get_humidity()
            pressure = sense.get_pressure()
            orientation = sense.get_orientation_degrees()

            # Reduce the usual positive temperature bias caused by heat from the Pi.
            pi_temp = cpu_temperature()
            if pi_temp is None:
                adjusted_temperature = raw_temperature
            else:
                adjusted_temperature = raw_temperature - (
                    (pi_temp - raw_temperature) / 1.5
                )

            for event in sense.stick.get_events():
                if event.action != "pressed":
                    continue

                with state.lock:
                    if event.direction == "right":
                        state.page = (state.page + 1) % PAGE_COUNT
                        state.auto_page = False
                    elif event.direction == "left":
                        state.page = (state.page - 1) % PAGE_COUNT
                        state.auto_page = False
                    elif event.direction == "middle":
                        state.auto_page = not state.auto_page
                    elif event.direction == "up":
                        state.led_brightness = min(2, state.led_brightness + 1)
                    elif event.direction == "down":
                        state.led_brightness = max(0, state.led_brightness - 1)

                    state.last_manual_page_change = time.monotonic()

            with state.lock:
                state.temperature = adjusted_temperature
                state.humidity = humidity
                state.pressure = pressure
                state.pitch = orientation["pitch"]
                state.roll = orientation["roll"]
                state.yaw = orientation["yaw"]
                online = state.online
                brightness = state.led_brightness

            sense.low_light = brightness < 2
            icon = "ok" if online else "error"

            if brightness == 0:
                if last_brightness != 0:
                    sense.clear()
            elif icon != last_icon or brightness != last_brightness:
                sense.set_pixels(green_check if icon == "ok" else red_x)

            last_icon = icon
            last_brightness = brightness
            stop_event.wait(SENSOR_REFRESH_SECONDS)

    except Exception as error:
        with state.lock:
            state.sense_error = str(error)
        print(f"Sense HAT thread error: {error}")
    finally:
        if sense is not None:
            try:
                sense.clear()
            except Exception:
                pass


def pcb_led_worker(stop_event):
    """Blink the transistor-driven PCB LED without blocking other hardware."""
    import RPi.GPIO as GPIO

    GPIO.setwarnings(False)
    GPIO.setmode(GPIO.BCM)
    GPIO.setup(PCB_LED_PIN, GPIO.OUT, initial=GPIO.LOW)

    try:
        print(
            f"PCB LED thread started on GPIO{PCB_LED_PIN} "
            "(physical pin 11)."
        )

        while not stop_event.is_set():
            GPIO.output(PCB_LED_PIN, GPIO.HIGH)
            print("PCB LED: ON")

            if stop_event.wait(PCB_LED_INTERVAL_SECONDS):
                break

            GPIO.output(PCB_LED_PIN, GPIO.LOW)
            print("PCB LED: OFF")

            if stop_event.wait(PCB_LED_INTERVAL_SECONDS):
                break

    except Exception as error:
        print(f"PCB LED thread error: {error}")
    finally:
        GPIO.output(PCB_LED_PIN, GPIO.LOW)
        GPIO.cleanup(PCB_LED_PIN)
        print("PCB LED turned off and GPIO17 released.")


def main():
    state = SharedState()
    stop_event = threading.Event()

    def request_stop(_signal_number=None, _frame=None):
        stop_event.set()

    signal.signal(signal.SIGINT, request_stop)
    signal.signal(signal.SIGTERM, request_stop)

    oled_thread = threading.Thread(
        target=oled_worker,
        args=(state, stop_event),
        name="OLED-Thread",
    )
    sense_thread = threading.Thread(
        target=sense_hat_worker,
        args=(state, stop_event),
        name="SenseHAT-Thread",
    )
    pcb_led_thread = threading.Thread(
        target=pcb_led_worker,
        args=(stop_event,),
        name="PCB-LED-Thread",
    )

    oled_thread.start()
    
    time.sleep(2)
    sense_thread.start()
    pcb_led_thread.start()

    print("OLED, Sense HAT, and PCB LED threads started. Press Ctrl+C to stop.")

    try:
        workers = (oled_thread, sense_thread, pcb_led_thread)
        while any(worker.is_alive() for worker in workers):
            stop_event.wait(0.5)
    finally:
        stop_event.set()
        oled_thread.join(timeout=3)
        sense_thread.join(timeout=3)
        pcb_led_thread.join(timeout=3)
        print("Dashboard stopped cleanly.")


if __name__ == "__main__":
    main()