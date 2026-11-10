#!/usr/bin/env python3
"""
VEML7700 + Simple Debug LED System
Clear LED feedback for continuous debugging

LED Modes:
1. HEARTBEAT - 1 slow blink every 2 seconds (system running)
2. DARK - Fast blinking (low light detected)
3. NORMAL - 1 blink every 5 seconds (normal environment)
4. FLASHLIGHT - Rapid bright pulses (flashlight/sunlight detected)
5. ERROR - Rapid blink (sensor error)
"""

import time
import board
import busio
import digitalio
import adafruit_veml7700
from datetime import datetime
import threading

# ===== Configuration =====
LED_PIN = board.D17
I2C_ADDRESS = 0x10
SAMPLE_INTERVAL = 0.2  # Fast sampling for responsive LED
LED_THRESHOLD_LOW = 5.0       # Below this = DARK mode
LED_THRESHOLD_HIGH = 50.0     # Above this = NORMAL mode
FLASHLIGHT_THRESHOLD = 300.0  # Flashlight detected above this
DEBUG_PRINT = True

# ===== LED Mode Definitions =====
class LEDMode:
    HEARTBEAT = "HEARTBEAT"      # 1 slow blink / 2 seconds
    DARK = "DARK"                # Fast continuous blinking
    NORMAL = "NORMAL"            # 1 blink / 5 seconds
    FLASHLIGHT = "FLASHLIGHT"    # Rapid bright pulses
    ERROR = "ERROR"              # Rapid blink - sensor error

# ===== Initialize Hardware =====
print("=" * 60)
print("VEML7700 + Simple Debug LED System")
print("=" * 60)

# Initialize I2C
print("Initializing I2C...")
i2c = busio.I2C(board.SCL, board.SDA)

# Scan for devices
print("Scanning I2C bus...")
devices = i2c.scan()
if devices:
    for addr in devices:
        print(f"  Found device: 0x{addr:02X}")
        if addr == I2C_ADDRESS:
            print(f"  ✓ VEML7700 detected at 0x{addr:02X}")
else:
    print("  ✗ No I2C devices found!")
    exit(1)

# Initialize VEML7700
print("\nInitializing VEML7700...")
try:
    veml7700 = adafruit_veml7700.VEML7700(i2c)
    print("  ✓ Sensor ready")
    
    # ✅ FIXED: Access gain and integration time using getter methods
    # These are properties that work in the library
    try:
        # The library uses these as properties
        print(f"  Gain: {veml7700.gain}")
        print(f"  Integration Time: {veml7700.integration_time}ms")
    except AttributeError:
        # Fallback for older versions
        print("  Note: Gain/IT properties not available in this version")
        print("  Using default settings")
        
except Exception as e:
    print(f"  ✗ Failed: {e}")
    exit(1)

# Initialize LED
print("Initializing LED...")
led = digitalio.DigitalInOut(LED_PIN)
led.direction = digitalio.Direction.OUTPUT
led.value = False
print("  ✓ LED ready\n")

# ===== LED Pattern Functions =====

def led_heartbeat():
    """
    1 slow blink every 2 seconds
    System is running in normal light
    """
    led.value = True
    time.sleep(0.1)
    led.value = False
    time.sleep(1.9)  # Total cycle = 2 seconds

def led_normal():
    """
    1 blink every 5 seconds
    Normal environment (healthy)
    """
    led.value = True
    time.sleep(0.2)
    led.value = False
    time.sleep(4.8)  # Total cycle = 5 seconds

def led_dark():
    """
    Fast continuous blinking
    Low light / dark condition detected
    """
    for _ in range(3):
        led.value = True
        time.sleep(0.05)
        led.value = False
        time.sleep(0.05)
    time.sleep(0.3)  # Pause between bursts

def led_flashlight():
    """
    Rapid bright pulses
    Flashlight or very bright light detected
    """
    for _ in range(8):
        led.value = True
        time.sleep(0.02)
        led.value = False
        time.sleep(0.02)
    time.sleep(0.1)  # Short pause

def led_error():
    """
    Rapid continuous blinking
    Sensor error or communication failure
    """
    for _ in range(5):
        led.value = True
        time.sleep(0.05)
        led.value = False
        time.sleep(0.05)
    time.sleep(0.3)  # Pause between bursts

# ===== LED Mode Dispatch =====
LED_HANDLERS = {
    LEDMode.HEARTBEAT: led_heartbeat,
    LEDMode.DARK: led_dark,
    LEDMode.NORMAL: led_normal,
    LEDMode.FLASHLIGHT: led_flashlight,
    LEDMode.ERROR: led_error,
}

def display_led_pattern(mode):
    """Display the appropriate LED pattern"""
    if mode in LED_HANDLERS:
        LED_HANDLERS[mode]()
    else:
        led_heartbeat()  # Default fallback

# ===== Sensor Reading =====
def get_light_level():
    """Read lux and ambient light from VEML7700"""
    try:
        lux = veml7700.lux
        ambient = veml7700.light
        return lux, ambient
    except Exception as e:
        return None, None

def determine_led_mode(lux):
    """Determine which LED mode to use based on lux reading"""
    if lux is None:
        return LEDMode.ERROR
    
    if lux > FLASHLIGHT_THRESHOLD:
        return LEDMode.FLASHLIGHT
    elif lux < LED_THRESHOLD_LOW:
        return LEDMode.DARK
    elif lux > LED_THRESHOLD_HIGH:
        return LEDMode.NORMAL
    else:
        return LEDMode.HEARTBEAT

# ===== LED Pattern Thread =====
class LEDController:
    """Manages LED patterns in a separate thread for non-blocking operation"""
    
    def __init__(self):
        self.current_mode = LEDMode.HEARTBEAT
        self.running = True
        self.lock = threading.Lock()
        self.thread = threading.Thread(target=self._run, daemon=True)
        self.thread.start()
    
    def _run(self):
        """Background thread to handle LED patterns"""
        while self.running:
            with self.lock:
                mode = self.current_mode
            
            # Display the current pattern
            display_led_pattern(mode)
            
            # Small delay between patterns
            time.sleep(0.02)
    
    def set_mode(self, mode):
        """Change the LED mode"""
        with self.lock:
            self.current_mode = mode
    
    def stop(self):
        """Stop the LED controller"""
        self.running = False
        led.value = False

# ===== Main Program =====
print("=" * 60)
print("LED Mode Legend:")
print("  💓 HEARTBEAT   - 1 blink every 2 seconds (system running)")
print("  🌙 DARK        - Fast blinking (low light / dark)")
print("  ☀️ NORMAL      - 1 blink every 5 seconds (normal light)")
print("  🔦 FLASHLIGHT  - Rapid bright pulses (flashlight detected)")
print("  ⚠️ ERROR       - Rapid blink (sensor error)")
print("=" * 60)
print("Press Ctrl+C to stop\n")

try:
    # Initialize LED controller
    led_controller = LEDController()
    
    # Initial LED test sequence
    print("Testing LED patterns...")
    test_modes = [
        (LEDMode.NORMAL, "NORMAL (1 blink / 5 sec)"),
        (LEDMode.HEARTBEAT, "HEARTBEAT (1 blink / 2 sec)"),
        (LEDMode.DARK, "DARK (fast blinking)"),
        (LEDMode.FLASHLIGHT, "FLASHLIGHT (rapid bright pulses)")
    ]
    
    for mode, description in test_modes:
        print(f"  Testing: {description}")
        led_controller.set_mode(mode)
        time.sleep(3)
    
    led_controller.set_mode(LEDMode.HEARTBEAT)
    print("✓ LED test complete\n")
    
    print("=" * 60)
    print("Now running - cover sensor with hand to see DARK mode")
    print("Shine flashlight to see FLASHLIGHT mode")
    print("=" * 60 + "\n")
    
    count = 0
    last_mode = None
    
    while True:
        count += 1
        
        # Read sensor
        lux, ambient = get_light_level()
        
        if lux is not None:
            # Determine LED mode
            mode = determine_led_mode(lux)
            
            # Update LED controller if mode changed
            if mode != last_mode:
                led_controller.set_mode(mode)
                last_mode = mode
                # Print mode change with visual indicator
                mode_icons = {
                    LEDMode.HEARTBEAT: "💓",
                    LEDMode.DARK: "🌙",
                    LEDMode.NORMAL: "☀️",
                    LEDMode.FLASHLIGHT: "🔦",
                    LEDMode.ERROR: "⚠️"
                }
                print(f"\n  🔄 Mode changed to: {mode_icons.get(mode, '')} {mode}")
            
            # Print debug info (every 5 readings)
            if DEBUG_PRINT and count % 5 == 0:
                timestamp = datetime.now().strftime("%H:%M:%S")
                icon = "🌙" if lux < 5 else "🌤️" if lux < 50 else "☀️" if lux < 300 else "🔦"
                print(f"[{timestamp}] #{count:4d} | {icon} Lux: {lux:8.2f} | Ambient: {ambient:8.0f} | Mode: {mode}")
            
            # Log data periodically
            if count % 100 == 0:
                with open("veml7700_debug_log.txt", "a") as f:
                    f.write(f"{datetime.now()}, {lux:.2f}, {ambient:.0f}, {mode}\n")
        else:
            print("  ⚠️ Failed to read sensor - entering ERROR mode")
            led_controller.set_mode(LEDMode.ERROR)
            time.sleep(0.5)
        
        time.sleep(SAMPLE_INTERVAL)
        
except KeyboardInterrupt:
    print("\n\nShutting down...")
    led_controller.stop()
    print("  ✓ LED turned OFF")
    print("Program terminated successfully")

finally:
    # Ensure LED is off
    try:
        led.value = False
    except:
        pass