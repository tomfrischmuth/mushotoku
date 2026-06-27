#!/usr/bin/env python3
"""Render a high-quality Tibetan singing-bowl one-shot to a stereo 16-bit WAV.

Pure stdlib. Models the bowl as a set of inharmonic partials, each split into
two slightly detuned oscillators (the characteristic slow "beating"/shimmer of
a struck bowl), panned across the stereo field, plus a short mallet-strike
transient. Pre-rendered so playback never glitches.

Usage (run from the repository root):
    python3 tools/gen_bowl.py
Writes app/src/main/res/raw/singing_bowl.wav, the sample MeditationService plays.
Tweak F0 / PARTIALS / DUR below to reshape the bowl, then re-run.
"""
import math, struct, wave, random

SR = 44100
DUR = 7.0                      # seconds (includes long natural tail)
N = int(SR * DUR)

# Inharmonic partial ratios typical of a singing bowl, with per-partial
# amplitude, decay time (s) and beat rate (Hz). Higher partials decay faster.
F0 = 280.0
PARTIALS = [
    # ratio, amp,  tau,  beat,  pan(-1..1)
    (1.000,  1.00, 6.5,  0.6,  -0.15),
    (2.710,  0.55, 4.0,  0.9,   0.20),
    (5.180,  0.28, 2.6,  1.3,  -0.30),
    (8.160,  0.14, 1.7,  1.7,   0.35),
    (11.66,  0.07, 1.1,  2.1,  -0.40),
    (15.69,  0.035,0.7,  2.6,   0.40),
]
ATTACK = 0.006                 # 6 ms onset ramp (no click)

random.seed(7)

left = [0.0] * N
right = [0.0] * N

# --- Partials --------------------------------------------------------------
for ratio, amp, tau, beat, pan in PARTIALS:
    f = F0 * ratio
    # two detuned components -> beating; opposite micro-pan for shimmer
    f_a = f - beat / 2.0
    f_b = f + beat / 2.0
    ph_a = random.uniform(0, 2 * math.pi)
    ph_b = random.uniform(0, 2 * math.pi)
    # equal-power pan
    lp = math.cos((pan * 0.5 + 0.5) * math.pi / 2)
    rp = math.sin((pan * 0.5 + 0.5) * math.pi / 2)
    wa = 2 * math.pi * f_a / SR
    wb = 2 * math.pi * f_b / SR
    for i in range(N):
        t = i / SR
        env = amp * math.exp(-t / tau)
        if t < ATTACK:
            env *= t / ATTACK
        s = env * (math.sin(wa * i + ph_a) + math.sin(wb * i + ph_b)) * 0.5
        left[i] += s * lp
        right[i] += s * rp

# --- Mallet strike transient (short, filtered noise) -----------------------
strike_len = int(SR * 0.05)
lp_state_l = lp_state_r = 0.0
for i in range(strike_len):
    t = i / SR
    env = 0.18 * math.exp(-t / 0.012)
    nl = random.uniform(-1, 1)
    nr = random.uniform(-1, 1)
    # one-pole low-pass to soften the noise burst
    lp_state_l += 0.35 * (nl - lp_state_l)
    lp_state_r += 0.35 * (nr - lp_state_r)
    left[i] += lp_state_l * env
    right[i] += lp_state_r * env

# --- Normalize to -1 dBFS --------------------------------------------------
peak = max(1e-9, max(max(abs(x) for x in left), max(abs(x) for x in right)))
gain = 0.89 / peak
# tiny global fade-out over last 40 ms guarantees a clean end
fade = int(SR * 0.04)
frames = bytearray()
for i in range(N):
    g = gain
    if i > N - fade:
        g *= (N - i) / fade
    l = int(max(-1.0, min(1.0, left[i] * g)) * 32767)
    r = int(max(-1.0, min(1.0, right[i] * g)) * 32767)
    frames += struct.pack('<hh', l, r)

out = "app/src/main/res/raw/singing_bowl.wav"
with wave.open(out, 'wb') as w:
    w.setnchannels(2)
    w.setsampwidth(2)
    w.setframerate(SR)
    w.writeframes(bytes(frames))
print("wrote", out, "frames", N, "bytes", len(frames))
