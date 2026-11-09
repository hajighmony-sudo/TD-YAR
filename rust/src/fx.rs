//! JNI-free effects maths: OKLab colour mixing (theme switch) and the ambient gold-dust simulation.
//! Kept separate from `lib.rs` so it can be unit-tested without the JNI crate.

/// Floats per particle: x, y (0..1), vx, vy (per second), phase, size (0..1), alpha (0..1).
pub const STRIDE: usize = 7;

// ---------- colour ----------

fn to_lin(c: f32) -> f32 {
    if c <= 0.04045 {
        c / 12.92
    } else {
        ((c + 0.055) / 1.055).powf(2.4)
    }
}

fn from_lin(c: f32) -> f32 {
    let c = c.clamp(0.0, 1.0);
    if c <= 0.003_130_8 {
        c * 12.92
    } else {
        1.055 * c.powf(1.0 / 2.4) - 0.055
    }
}

fn to_oklab(r: f32, g: f32, b: f32) -> [f32; 3] {
    let (r, g, b) = (to_lin(r), to_lin(g), to_lin(b));
    let l = (0.412_221_47 * r + 0.536_332_55 * g + 0.051_445_995 * b).cbrt();
    let m = (0.211_903_5 * r + 0.680_699_5 * g + 0.107_396_96 * b).cbrt();
    let s = (0.088_302_46 * r + 0.281_718_85 * g + 0.629_978_7 * b).cbrt();
    [
        0.210_454_26 * l + 0.793_617_8 * m - 0.004_072_047 * s,
        1.977_998_5 * l - 2.428_592_2 * m + 0.450_593_7 * s,
        0.025_904_037 * l + 0.782_771_77 * m - 0.808_675_77 * s,
    ]
}

fn from_oklab(lab: [f32; 3]) -> [f32; 3] {
    let l = lab[0] + 0.396_337_78 * lab[1] + 0.215_803_76 * lab[2];
    let m = lab[0] - 0.105_561_346 * lab[1] - 0.063_854_17 * lab[2];
    let s = lab[0] - 0.089_484_18 * lab[1] - 1.291_485_5 * lab[2];
    let (l, m, s) = (l * l * l, m * m * m, s * s * s);
    [
        from_lin(4.076_741_7 * l - 3.307_711_6 * m + 0.230_969_94 * s),
        from_lin(-1.268_438 * l + 2.609_757_4 * m - 0.341_319_4 * s),
        from_lin(-0.004_196_086_3 * l - 0.703_418_6 * m + 1.707_614_7 * s),
    ]
}

/// Mixes two packed ARGB colours (alpha linear, RGB in OKLab so gold -> teal never goes muddy).
pub fn mix_argb(a: u32, b: u32, t: f32) -> u32 {
    let t = t.clamp(0.0, 1.0);
    let ch = |c: u32, sh: u32| ((c >> sh) & 0xFF) as f32 / 255.0;
    let la = to_oklab(ch(a, 16), ch(a, 8), ch(a, 0));
    let lb = to_oklab(ch(b, 16), ch(b, 8), ch(b, 0));
    // weight colours by alpha so a translucent white never greys out an opaque colour mid-fade
    let (wa, wb) = ((1.0 - t) * ch(a, 24), t * ch(b, 24));
    let k = if wa + wb > 1e-6 { wb / (wa + wb) } else { t };
    let lab = [
        la[0] + (lb[0] - la[0]) * k,
        la[1] + (lb[1] - la[1]) * k,
        la[2] + (lb[2] - la[2]) * k,
    ];
    let rgb = from_oklab(lab);
    let alpha = ch(a, 24) + (ch(b, 24) - ch(a, 24)) * t;
    let q = |v: f32| (v.clamp(0.0, 1.0) * 255.0 + 0.5) as u32;
    (q(alpha) << 24) | (q(rgb[0]) << 16) | (q(rgb[1]) << 8) | q(rgb[2])
}

/// Element-wise [`mix_argb`] over equal-length palettes.
pub fn mix_palette(a: &[u32], b: &[u32], t: f32) -> Vec<u32> {
    a.iter().zip(b.iter()).map(|(x, y)| mix_argb(*x, *y, t)).collect()
}

// ---------- particles ----------

fn next(seed: &mut u32) -> f32 {
    // xorshift32 -> [0,1)
    let mut x = *seed;
    x ^= x << 13;
    x ^= x >> 17;
    x ^= x << 5;
    *seed = x;
    (x >> 8) as f32 / 16_777_216.0
}

fn spawn(p: &mut [f32], seed: &mut u32, at_bottom: bool) {
    p[0] = next(seed);
    p[1] = if at_bottom { 1.02 } else { next(seed) };
    p[2] = (next(seed) - 0.5) * 0.02;
    p[3] = -(0.012 + next(seed) * 0.05);
    p[4] = next(seed) * std::f32::consts::TAU;
    p[5] = 0.25 + next(seed) * 0.75;
    p[6] = 0.0;
}

/// Fills `state` (len multiple of [`STRIDE`]) with a fresh, scattered field.
pub fn particles_init(state: &mut [f32], seed: u32) {
    let mut s = seed.max(1);
    for p in state.chunks_exact_mut(STRIDE) {
        spawn(p, &mut s, false);
        p[6] = 0.5;
    }
}

/// Advances every particle by `dt` seconds. `t` is the running clock used for sway and twinkle.
/// Particles that leave the top are respawned at the bottom; x wraps around.
pub fn particles_step(state: &mut [f32], dt: f32, t: f32, seed: u32) {
    let dt = dt.clamp(0.0, 0.1);
    let mut s = seed.max(1) ^ (t * 1000.0) as u32 | 1;
    for p in state.chunks_exact_mut(STRIDE) {
        let sway = (t * 0.7 + p[4]).sin() * 0.012;
        p[0] += (p[2] + sway) * dt;
        p[1] += p[3] * dt;
        if p[0] < 0.0 {
            p[0] += 1.0;
        } else if p[0] > 1.0 {
            p[0] -= 1.0;
        }
        if p[1] < -0.03 {
            spawn(p, &mut s, true);
        }
        let tw = 0.5 + 0.5 * (t * 1.6 + p[4]).sin();
        let edge = ((p[1] + 0.03) / 0.15).clamp(0.0, 1.0) * ((1.05 - p[1]) / 0.15).clamp(0.0, 1.0);
        p[6] = (0.2 + 0.8 * tw) * edge;
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn mix_endpoints_are_exact() {
        let a = 0xFFF2B53D;
        let b = 0xFF22D3C5;
        let close = |x: u32, y: u32| (0..4).all(|i| ((x >> (i * 8)) & 0xFF).abs_diff((y >> (i * 8)) & 0xFF) <= 1);
        assert!(close(mix_argb(a, b, 0.0), a));
        assert!(close(mix_argb(a, b, 1.0), b));
    }

    #[test]
    fn mix_midpoint_is_between_and_keeps_alpha() {
        let m = mix_argb(0x00000000, 0xFFFFFFFF, 0.5);
        let alpha = m >> 24;
        assert!((126..=129).contains(&alpha), "alpha {alpha}");
        // the transparent endpoint contributes no colour, so the mix keeps the visible one
        let r = (m >> 16) & 0xFF;
        assert!(r >= 250, "r {r}");
        let mid = mix_argb(0xFF000000, 0xFFFFFFFF, 0.5);
        let g = (mid >> 8) & 0xFF;
        assert!(g > 90 && g < 190, "g {g}");
    }

    #[test]
    fn mix_palette_len() {
        let v = mix_palette(&[1, 2, 3], &[4, 5, 6], 0.3);
        assert_eq!(v.len(), 3);
    }

    #[test]
    fn particles_stay_in_range_and_finite() {
        let n = 64;
        let mut st = vec![0f32; n * STRIDE];
        particles_init(&mut st, 7);
        let mut t = 0.0;
        for _ in 0..2000 {
            t += 0.016;
            particles_step(&mut st, 0.016, t, 7);
            for p in st.chunks_exact(STRIDE) {
                assert!(p.iter().all(|v| v.is_finite()));
                assert!((0.0..=1.0).contains(&p[0]), "x {}", p[0]);
                assert!(p[1] >= -0.04 && p[1] <= 1.1, "y {}", p[1]);
                assert!((0.0..=1.0).contains(&p[6]), "a {}", p[6]);
            }
        }
    }

    #[test]
    fn particles_actually_move_up() {
        let mut st = vec![0f32; 8 * STRIDE];
        particles_init(&mut st, 3);
        let y0: Vec<f32> = st.chunks_exact(STRIDE).map(|p| p[1]).collect();
        particles_step(&mut st, 0.05, 1.0, 3);
        let moved = st.chunks_exact(STRIDE).zip(y0).filter(|(p, y)| p[1] < *y).count();
        assert!(moved >= 6);
    }
}
