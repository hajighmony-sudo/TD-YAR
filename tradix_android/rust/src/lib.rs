//! TRADENEX native core: session maths and trade statistics.
//! Exposed to Kotlin through JNI (see app/.../core/NativeCore.kt).
//! Every function has a pure-Kotlin fallback, so the app still runs without this library.
//! `fx` holds the theme-switch colour mixing and the gold-dust particle simulation.

pub mod fx;

use jni::objects::{JDoubleArray, JFloatArray, JIntArray, JObject};
use jni::sys::{jfloat, jint};
use jni::JNIEnv;

/// Session table in UTC minutes of the day: (start, end).
/// Index: 0 Sydney, 1 Tokyo, 2 London, 3 New York.
const SESSIONS: [(i32, i32); 4] = [(1320, 420), (0, 540), (480, 1020), (780, 1320)];

fn is_open(minute: i32, start: i32, end: i32) -> bool {
    if start < end {
        minute >= start && minute < end
    } else {
        minute >= start || minute < end
    }
}

/// Bit i is set when session i is open.
pub fn session_mask(minute: i32) -> i32 {
    let m = minute.rem_euclid(1440);
    let mut mask = 0;
    for (i, (s, e)) in SESSIONS.iter().enumerate() {
        if is_open(m, *s, *e) {
            mask |= 1 << i;
        }
    }
    mask
}

/// Minutes during which both sessions are open in one day.
pub fn overlap_minutes(a: usize, b: usize) -> i32 {
    if a >= 4 || b >= 4 {
        return 0;
    }
    (0..1440)
        .filter(|m| {
            is_open(*m, SESSIONS[a].0, SESSIONS[a].1) && is_open(*m, SESSIONS[b].0, SESSIONS[b].1)
        })
        .count() as i32
}

/// Minutes until the given session flips between open and closed.
pub fn minutes_to_change(minute: i32, session: usize) -> i32 {
    if session >= 4 {
        return 0;
    }
    let (s, e) = SESSIONS[session];
    let now = minute.rem_euclid(1440);
    let open = is_open(now, s, e);
    for step in 1..=1440 {
        let m = (now + step) % 1440;
        if is_open(m, s, e) != open {
            return step;
        }
    }
    0
}

/// Deterministic performance metrics derived only from realized trade P&L.
/// Output: [net, win_rate, profit_factor, max_drawdown, expectancy, avg_win, avg_loss_abs,
/// recovery_factor, largest_win_share, largest_loss_share].
/// Ratios with undefined denominators are returned as 0; callers must display sample counts.
pub fn performance_metrics(pnls: &[f64]) -> [f64; 10] {
    if pnls.is_empty() {
        return [0.0; 10];
    }
    let mut net = 0.0;
    let mut gross_win = 0.0;
    let mut gross_loss = 0.0;
    let mut wins = 0usize;
    let mut win_sum = 0.0;
    let mut loss_sum = 0.0;
    let mut largest_win: f64 = 0.0;
    let mut largest_loss: f64 = 0.0;
    let mut equity = 0.0;
    let mut peak: f64 = 0.0;
    let mut max_dd: f64 = 0.0;

    for value in pnls.iter().copied().filter(|v| v.is_finite()) {
        net += value;
        if value > 0.0 {
            wins += 1;
            win_sum += value;
            gross_win += value;
            largest_win = largest_win.max(value);
        } else if value < 0.0 {
            loss_sum += value.abs();
            gross_loss += value.abs();
            largest_loss = largest_loss.max(value.abs());
        }
        equity += value;
        peak = peak.max(equity);
        max_dd = max_dd.min(equity - peak);
    }
    let count = pnls.iter().filter(|v| v.is_finite()).count();
    if count == 0 { return [0.0; 10]; }
    let pf = if gross_loss > 0.0 { gross_win / gross_loss } else if gross_win > 0.0 { 99.0 } else { 0.0 };
    let avg_win = if wins > 0 { win_sum / wins as f64 } else { 0.0 };
    let losses = count.saturating_sub(wins + pnls.iter().filter(|v| **v == 0.0).count());
    let avg_loss = if losses > 0 { loss_sum / losses as f64 } else { 0.0 };
    let recovery = if max_dd < 0.0 { net / max_dd.abs() } else { 0.0 };
    [
        net,
        wins as f64 / count as f64,
        pf,
        max_dd,
        net / count as f64,
        avg_win,
        avg_loss,
        recovery,
        if gross_win > 0.0 { largest_win / gross_win } else { 0.0 },
        if gross_loss > 0.0 { largest_loss / gross_loss } else { 0.0 },
    ]
}

/// Explainable revenge-pattern detector. Classification codes:
/// 0 = no event, 1 = loss-sequence-only, 2 = possible, 3 = strong evidence,
/// 4 = explicitly tagged by the user. Evidence is not psychological probability.
pub fn revenge_evidence(
    gap_minutes: Option<i64>,
    previous_was_loss: bool,
    previous_risk: f64,
    current_risk: f64,
    previous_lot: f64,
    current_lot: f64,
    plan_violation: bool,
    revenge_tag: bool,
    emotional_signal: bool,
    prior_loss_streak: u32,
) -> [i32; 3] {
    if !previous_was_loss { return [0, 0, 0]; }
    let rapid = gap_minutes.map(|g| g >= 0 && g <= 15).unwrap_or(false);
    let risk_up = previous_risk.is_finite() && current_risk.is_finite()
        && previous_risk > 0.0 && current_risk > previous_risk * 1.25;
    let size_up = previous_lot.is_finite() && current_lot.is_finite()
        && previous_lot > 0.0 && current_lot > previous_lot * 1.25;
    let loss_sequence = prior_loss_streak >= 2;
    let behavioral = risk_up || size_up || plan_violation || revenge_tag || emotional_signal || loss_sequence;
    if !behavioral { return [0, 0, 0]; }

    let flags = (rapid as i32)
        | ((risk_up as i32) << 1)
        | ((size_up as i32) << 2)
        | ((plan_violation as i32) << 3)
        | ((revenge_tag as i32) << 4)
        | ((emotional_signal as i32) << 5)
        | ((loss_sequence as i32) << 6);
    let score = ((rapid as i32) * 15 + (risk_up as i32) * 20 + (size_up as i32) * 10
        + (plan_violation as i32) * 20 + (revenge_tag as i32) * 30
        + (emotional_signal as i32) * 10 + (loss_sequence as i32) * 10).min(100);
    let class = if revenge_tag { 4 } else if score >= 45 { 3 } else if score >= 15 { 2 } else { 1 };
    [score, flags, class]
}

/// C ABI: output = [score, evidence_flags, classification]. Boolean inputs are 0/1.
#[no_mangle]
pub unsafe extern "C" fn tx_ai_revenge_evidence(
    gap_minutes: i64,
    has_gap: i32,
    previous_was_loss: i32,
    previous_risk: f64,
    current_risk: f64,
    previous_lot: f64,
    current_lot: f64,
    plan_violation: i32,
    revenge_tag: i32,
    emotional_signal: i32,
    prior_loss_streak: u32,
    output_ptr: *mut i32,
    output_len: usize,
) -> i32 {
    if output_ptr.is_null() || output_len < 3 { return -1; }
    let result = revenge_evidence(
        if has_gap != 0 { Some(gap_minutes) } else { None },
        previous_was_loss != 0, previous_risk, current_risk, previous_lot, current_lot,
        plan_violation != 0, revenge_tag != 0, emotional_signal != 0, prior_loss_streak,
    );
    std::ptr::copy_nonoverlapping(result.as_ptr(), output_ptr, result.len());
    0
}

/// C ABI for Dart FFI. Returns 0 on success, -1 for invalid pointers/length.
/// The caller owns both buffers and guarantees they are valid for the supplied lengths.
#[no_mangle]
pub unsafe extern "C" fn tx_ai_performance_metrics(
    pnls_ptr: *const f64,
    pnls_len: usize,
    output_ptr: *mut f64,
    output_len: usize,
) -> i32 {
    if output_ptr.is_null() || output_len < 10 || (pnls_len > 0 && pnls_ptr.is_null()) {
        return -1;
    }
    let pnls = if pnls_len == 0 {
        &[]
    } else {
        std::slice::from_raw_parts(pnls_ptr, pnls_len)
    };
    let metrics = performance_metrics(pnls);
    std::ptr::copy_nonoverlapping(metrics.as_ptr(), output_ptr, metrics.len());
    0
}

/// Deterministic counterfactual metrics. A zero mask entry excludes that trade;
/// scale_factor is applied to every included realized P&L (e.g. 0.5 for half risk).
pub fn scenario_metrics(pnls: &[f64], include_mask: Option<&[u8]>, scale_factor: f64) -> Option<[f64; 10]> {
    if !scale_factor.is_finite() || scale_factor < 0.0 {
        return None;
    }
    if let Some(mask) = include_mask {
        if mask.len() != pnls.len() {
            return None;
        }
    }
    let selected: Vec<f64> = pnls.iter().enumerate().filter_map(|(index, value)| {
        let included = include_mask.map(|mask| mask[index] != 0).unwrap_or(true);
        if included && value.is_finite() { Some(*value * scale_factor) } else { None }
    }).collect();
    Some(performance_metrics(&selected))
}

/// C ABI for deterministic counterfactual calculations. mask_len=0 means include all trades.
/// Returns -1 for invalid buffers/lengths and -2 for an invalid scale factor.
#[no_mangle]
pub unsafe extern "C" fn tx_ai_scenario_metrics(
    pnls_ptr: *const f64,
    pnls_len: usize,
    include_mask_ptr: *const u8,
    mask_len: usize,
    scale_factor: f64,
    output_ptr: *mut f64,
    output_len: usize,
) -> i32 {
    if output_ptr.is_null() || output_len < 10 || (pnls_len > 0 && pnls_ptr.is_null()) {
        return -1;
    }
    if mask_len != 0 && (mask_len != pnls_len || include_mask_ptr.is_null()) {
        return -1;
    }
    if !scale_factor.is_finite() || scale_factor < 0.0 {
        return -2;
    }
    let pnls = if pnls_len == 0 { &[] } else { std::slice::from_raw_parts(pnls_ptr, pnls_len) };
    let mask = if mask_len == 0 { None } else { Some(std::slice::from_raw_parts(include_mask_ptr, mask_len)) };
    let metrics = match scenario_metrics(pnls, mask, scale_factor) {
        Some(value) => value,
        None => return -1,
    };
    std::ptr::copy_nonoverlapping(metrics.as_ptr(), output_ptr, metrics.len());
    0
}

/// [net, win_rate (0..1), profit_factor, max_drawdown (<= 0)]
pub fn stats(pnls: &[f64]) -> [f64; 4] {
    if pnls.is_empty() {
        return [0.0, 0.0, 0.0, 0.0];
    }
    let mut net = 0.0;
    let mut gain = 0.0;
    let mut loss = 0.0;
    let mut wins = 0usize;
    let mut equity = 0.0;
    let mut peak = 0.0;
    let mut dd = 0.0;
    for p in pnls {
        net += p;
        if *p > 0.0 {
            gain += p;
            wins += 1;
        } else {
            loss += -p;
        }
        equity += p;
        if equity > peak {
            peak = equity;
        }
        if equity - peak < dd {
            dd = equity - peak;
        }
    }
    let pf = if loss > 0.0 {
        gain / loss
    } else if gain > 0.0 {
        99.0
    } else {
        0.0
    };
    [net, wins as f64 / pnls.len() as f64, pf, dd]
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nSessionMask<'l>(
    _env: JNIEnv<'l>,
    _this: JObject<'l>,
    minute: jint,
) -> jint {
    session_mask(minute)
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nOverlapMinutes<'l>(
    _env: JNIEnv<'l>,
    _this: JObject<'l>,
    a: jint,
    b: jint,
) -> jint {
    overlap_minutes(a.max(0) as usize, b.max(0) as usize)
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nMinutesToChange<'l>(
    _env: JNIEnv<'l>,
    _this: JObject<'l>,
    minute: jint,
    session: jint,
) -> jint {
    minutes_to_change(minute, session.max(0) as usize)
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nStats<'l>(
    mut env: JNIEnv<'l>,
    _this: JObject<'l>,
    pnls: JDoubleArray<'l>,
) -> JDoubleArray<'l> {
    let len = env.get_array_length(&pnls).unwrap_or(0) as usize;
    let mut buf = vec![0f64; len];
    let _ = env.get_double_array_region(&pnls, 0, &mut buf);
    let s = stats(&buf);
    match env.new_double_array(4) {
        Ok(out) => {
            let _ = env.set_double_array_region(&out, 0, &s);
            out
        }
        Err(_) => unsafe { JDoubleArray::from_raw(std::ptr::null_mut()) },
    }
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nPerformanceMetrics<'l>(
    mut env: JNIEnv<'l>,
    _this: JObject<'l>,
    pnls: JDoubleArray<'l>,
) -> JDoubleArray<'l> {
    let len = env.get_array_length(&pnls).unwrap_or(0) as usize;
    let mut values = vec![0.0f64; len];
    let _ = env.get_double_array_region(&pnls, 0, &mut values);
    let metrics = performance_metrics(&values);
    match env.new_double_array(metrics.len() as i32) {
        Ok(output) => {
            let _ = env.set_double_array_region(&output, 0, &metrics);
            output
        }
        Err(_) => unsafe { JDoubleArray::from_raw(std::ptr::null_mut()) },
    }
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nMixColors<'l>(
    mut env: JNIEnv<'l>,
    _this: JObject<'l>,
    a: JIntArray<'l>,
    b: JIntArray<'l>,
    t: jfloat,
) -> JIntArray<'l> {
    let la = env.get_array_length(&a).unwrap_or(0) as usize;
    let lb = env.get_array_length(&b).unwrap_or(0) as usize;
    let n = la.min(lb);
    let mut ba = vec![0i32; la];
    let mut bb = vec![0i32; lb];
    let _ = env.get_int_array_region(&a, 0, &mut ba);
    let _ = env.get_int_array_region(&b, 0, &mut bb);
    let ua: Vec<u32> = ba.iter().take(n).map(|v| *v as u32).collect();
    let ub: Vec<u32> = bb.iter().take(n).map(|v| *v as u32).collect();
    let out: Vec<i32> = fx::mix_palette(&ua, &ub, t).into_iter().map(|v| v as i32).collect();
    match env.new_int_array(n as i32) {
        Ok(arr) => {
            let _ = env.set_int_array_region(&arr, 0, &out);
            arr
        }
        Err(_) => unsafe { JIntArray::from_raw(std::ptr::null_mut()) },
    }
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nParticlesInit<'l>(
    mut env: JNIEnv<'l>,
    _this: JObject<'l>,
    state: JFloatArray<'l>,
    seed: jint,
) {
    let len = env.get_array_length(&state).unwrap_or(0) as usize;
    let mut buf = vec![0f32; len];
    fx::particles_init(&mut buf, seed as u32);
    let _ = env.set_float_array_region(&state, 0, &buf);
}

#[no_mangle]
pub extern "system" fn Java_com_tradenex_app_core_NativeCore_nParticlesStep<'l>(
    mut env: JNIEnv<'l>,
    _this: JObject<'l>,
    state: JFloatArray<'l>,
    dt: jfloat,
    t: jfloat,
    seed: jint,
) {
    let len = env.get_array_length(&state).unwrap_or(0) as usize;
    let mut buf = vec![0f32; len];
    if env.get_float_array_region(&state, 0, &mut buf).is_err() {
        return;
    }
    fx::particles_step(&mut buf, dt, t, seed as u32);
    let _ = env.set_float_array_region(&state, 0, &buf);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn london_ny_overlap_is_four_hours() {
        assert_eq!(overlap_minutes(2, 3), 240);
    }

    #[test]
    fn sydney_wraps_midnight() {
        assert_eq!(session_mask(23 * 60) & 1, 1);
        assert_eq!(session_mask(8 * 60) & 1, 0);
    }

    #[test]
    fn performance_metrics_are_deterministic_and_explainable() {
        let m = performance_metrics(&[100.0, -50.0, 150.0, -100.0]);
        assert_eq!(m[0], 100.0);
        assert_eq!(m[1], 0.5);
        assert!((m[2] - 250.0 / 150.0).abs() < 1e-9);
        assert_eq!(m[3], -100.0);
        assert_eq!(m[4], 25.0);
        assert_eq!(m[5], 125.0);
        assert_eq!(m[6], 75.0);
        assert_eq!(m[7], 1.0);
    }

    #[test]
    fn performance_metrics_ignore_non_finite_values() {
        let m = performance_metrics(&[10.0, f64::NAN, f64::INFINITY, -5.0]);
        assert_eq!(m[0], 5.0);
        assert_eq!(m[1], 0.5);
        assert_eq!(m[4], 2.5);
    }

    #[test]
    fn empty_performance_metrics_are_zero() {
        assert_eq!(performance_metrics(&[]), [0.0; 10]);
    }

    #[test]
    fn planned_quick_reentry_is_not_revenge_evidence() {
        assert_eq!(revenge_evidence(Some(5), true, 100.0, 100.0, 0.1, 0.1, false, false, false, 1), [0, 0, 0]);
    }

    #[test]
    fn revenge_evidence_combines_quick_reentry_and_risk_increase() {
        let result = revenge_evidence(Some(5), true, 100.0, 140.0, 0.1, 0.1, false, false, false, 1);
        assert_eq!(result[0], 35);
        assert_eq!(result[1] & 0b11, 0b11);
        assert_eq!(result[2], 2);
    }

    #[test]
    fn revenge_tag_is_separate_user_confirmed_classification() {
        let result = revenge_evidence(None, true, 0.0, 0.0, 0.1, 0.1, false, true, false, 0);
        assert_eq!(result[2], 4);
    }

    #[test]
    fn c_abi_returns_structured_metrics() {
        let input = [10.0, -4.0, 6.0];
        let mut output = [0.0; 10];
        let status = unsafe {
            tx_ai_performance_metrics(input.as_ptr(), input.len(), output.as_mut_ptr(), output.len())
        };
        assert_eq!(status, 0);
        assert_eq!(output[0], 12.0);
        assert!((output[1] - 2.0 / 3.0).abs() < 1e-9);
    }

    #[test]
    fn c_abi_rejects_invalid_output_buffer() {
        let input = [10.0];
        let mut output = [0.0; 4];
        let status = unsafe {
            tx_ai_performance_metrics(input.as_ptr(), input.len(), output.as_mut_ptr(), output.len())
        };
        assert_eq!(status, -1);
    }

    #[test]
    fn scenario_metrics_excludes_off_plan_and_scales_risk() {
        let pnls = [100.0, -40.0, 60.0];
        let include = [1u8, 0u8, 1u8];
        let filtered = scenario_metrics(&pnls, Some(&include), 1.0).unwrap();
        assert_eq!(filtered[0], 160.0);
        assert_eq!(filtered[1], 1.0);
        let half = scenario_metrics(&pnls, None, 0.5).unwrap();
        assert_eq!(half[0], 60.0);
        assert_eq!(half[4], 20.0);
    }

    #[test]
    fn scenario_metrics_rejects_invalid_masks_and_scale() {
        assert!(scenario_metrics(&[1.0, 2.0], Some(&[1u8]), 1.0).is_none());
        assert!(scenario_metrics(&[1.0], None, f64::NAN).is_none());
    }

    #[test]
    fn c_abi_scenario_metrics_returns_filtered_result() {
        let pnls = [10.0, -5.0, 20.0];
        let mask = [1u8, 0u8, 1u8];
        let mut output = [0.0; 10];
        let status = unsafe {
            tx_ai_scenario_metrics(
                pnls.as_ptr(), pnls.len(), mask.as_ptr(), mask.len(),
                0.5, output.as_mut_ptr(), output.len(),
            )
        };
        assert_eq!(status, 0);
        assert_eq!(output[0], 15.0);
        assert_eq!(output[1], 1.0);
    }

    #[test]
    fn stats_basic() {
        let s = stats(&[100.0, -50.0, 150.0, -100.0]);
        assert_eq!(s[0], 100.0);
        assert_eq!(s[1], 0.5);
        assert!((s[2] - 250.0 / 150.0).abs() < 1e-9);
        assert_eq!(s[3], -100.0);
    }
}
