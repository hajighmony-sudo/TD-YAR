import 'package:timezone/data/latest.dart' as timezone_data;
import 'package:timezone/timezone.dart' as tz;

import '../models/trading_session.dart';

/// Timezone-aware session clock. Uses the IANA timezone database so DST
/// transitions are calculated from the session's own local timezone.
class TradingSessions {
  TradingSessions() {
    _ensureTimeZones();
  }

  static bool _initialized = false;

  static void _ensureTimeZones() {
    if (_initialized) return;
    timezone_data.initializeTimeZones();
    _initialized = true;
  }

  void validateDefinition(TradingSessionDefinition definition) {
    definition.validate();
    try {
      tz.getLocation(definition.timezoneId);
    } on Exception {
      throw ArgumentError('Unknown IANA timezone: ${definition.timezoneId}');
    }
  }

  List<TradingSessionStatus> statusesAt({
    required DateTime instant,
    required List<TradingSessionDefinition> sessions,
  }) {
    final utcInstant = instant.toUtc();
    final result = <TradingSessionStatus>[];
    for (final session in sessions.where((item) => item.isEnabled)) {
      validateDefinition(session);
      final windows = _occurrences(
        session,
        utcInstant.subtract(const Duration(days: 2)),
        utcInstant.add(const Duration(days: 9)),
      );
      SessionOccurrence? current;
      SessionOccurrence? next;
      for (final window in windows) {
        if (!utcInstant.isBefore(window.opensAt) &&
            utcInstant.isBefore(window.closesAt)) {
          current = window;
        } else if (window.opensAt.isAfter(utcInstant) &&
            (next == null || window.opensAt.isBefore(next.opensAt))) {
          next = window;
        }
      }
      result.add(TradingSessionStatus(
        session: session,
        isOpen: current != null,
        currentOccurrence: current,
        nextOccurrence: next,
      ));
    }
    result.sort((a, b) => a.session.name.compareTo(b.session.name));
    return List.unmodifiable(result);
  }

  List<SessionOverlap> overlapsBetween({
    required DateTime from,
    required DateTime to,
    required List<TradingSessionDefinition> sessions,
  }) {
    if (to.isBefore(from)) {
      throw ArgumentError('Overlap range end must not precede start.');
    }
    final enabled = sessions.where((item) => item.isEnabled).toList();
    for (final session in enabled) {
      validateDefinition(session);
    }

    final windows = enabled
        .map((session) => _occurrences(
              session,
              from.toUtc().subtract(const Duration(days: 2)),
              to.toUtc().add(const Duration(days: 2)),
            ))
        .toList(growable: false);
    final overlaps = <SessionOverlap>[];

    for (var firstIndex = 0; firstIndex < enabled.length; firstIndex++) {
      for (var secondIndex = firstIndex + 1;
          secondIndex < enabled.length;
          secondIndex++) {
        for (final first in windows[firstIndex]) {
          for (final second in windows[secondIndex]) {
            final startsAt = first.opensAt.isAfter(second.opensAt)
                ? first.opensAt : second.opensAt;
            final endsAt = first.closesAt.isBefore(second.closesAt)
                ? first.closesAt : second.closesAt;
            if (!endsAt.isAfter(startsAt) ||
                !endsAt.isAfter(from.toUtc()) ||
                !startsAt.isBefore(to.toUtc())) {
              continue;
            }
            final clippedStart = startsAt.isBefore(from.toUtc())
                ? from.toUtc() : startsAt;
            final clippedEnd = endsAt.isAfter(to.toUtc())
                ? to.toUtc() : endsAt;
            if (!clippedEnd.isAfter(clippedStart)) continue;
            overlaps.add(SessionOverlap(
              firstSessionId: enabled[firstIndex].id,
              firstSessionName: enabled[firstIndex].name,
              secondSessionId: enabled[secondIndex].id,
              secondSessionName: enabled[secondIndex].name,
              startsAt: clippedStart,
              endsAt: clippedEnd,
            ));
          }
        }
      }
    }

    overlaps.sort((a, b) {
      final byStart = a.startsAt.compareTo(b.startsAt);
      if (byStart != 0) return byStart;
      final byFirst = a.firstSessionId.compareTo(b.firstSessionId);
      return byFirst != 0
          ? byFirst : a.secondSessionId.compareTo(b.secondSessionId);
    });
    return List.unmodifiable(overlaps);
  }

  List<SessionOccurrence> _occurrences(
    TradingSessionDefinition session,
    DateTime from,
    DateTime to,
  ) {
    final location = tz.getLocation(session.timezoneId);
    final localFrom = tz.TZDateTime.from(from, location);
    final localTo = tz.TZDateTime.from(to, location);
    var date = DateTime.utc(
      localFrom.year, localFrom.month, localFrom.day - 2,
    );
    final lastDate = DateTime.utc(
      localTo.year, localTo.month, localTo.day + 2,
    );
    final result = <SessionOccurrence>[];
    final openMinuteOfDay = session.openHour * 60 + session.openMinute;
    final closeMinuteOfDay = session.closeHour * 60 + session.closeMinute;
    final closesNextDay = closeMinuteOfDay <= openMinuteOfDay;

    while (!date.isAfter(lastDate)) {
      if (session.weekdays.contains(date.weekday)) {
        final open = tz.TZDateTime(
          location,
          date.year,
          date.month,
          date.day,
          session.openHour,
          session.openMinute,
        );
        final closeDate = closesNextDay
            ? DateTime.utc(date.year, date.month, date.day + 1)
            : date;
        final close = tz.TZDateTime(
          location,
          closeDate.year,
          closeDate.month,
          closeDate.day,
          session.closeHour,
          session.closeMinute,
        );
        final opensAt = open.toUtc();
        final closesAt = close.toUtc();
        if (closesAt.isAfter(from) && opensAt.isBefore(to)) {
          result.add(SessionOccurrence(
            session: session,
            opensAt: opensAt,
            closesAt: closesAt,
          ));
        }
      }
      date = DateTime.utc(date.year, date.month, date.day + 1);
    }
    result.sort((a, b) => a.opensAt.compareTo(b.opensAt));
    return result;
  }

  static List<TradingSessionDefinition> defaultSessions({
    DateTime? timestamp,
  }) {
    final now = timestamp ?? DateTime.now();
    const weekdays = [1, 2, 3, 4, 5];
    return [
      TradingSessionDefinition(
        id: 'sydney',
        name: 'Sydney',
        timezoneId: 'Australia/Sydney',
        openHour: 8,
        openMinute: 0,
        closeHour: 17,
        closeMinute: 0,
        weekdays: weekdays,
        createdAt: now,
        updatedAt: now,
      ),
      TradingSessionDefinition(
        id: 'tokyo',
        name: 'Tokyo',
        timezoneId: 'Asia/Tokyo',
        openHour: 9,
        openMinute: 0,
        closeHour: 18,
        closeMinute: 0,
        weekdays: weekdays,
        createdAt: now,
        updatedAt: now,
      ),
      TradingSessionDefinition(
        id: 'london',
        name: 'London',
        timezoneId: 'Europe/London',
        openHour: 8,
        openMinute: 0,
        closeHour: 17,
        closeMinute: 0,
        weekdays: weekdays,
        createdAt: now,
        updatedAt: now,
      ),
      TradingSessionDefinition(
        id: 'new-york',
        name: 'New York',
        timezoneId: 'America/New_York',
        openHour: 8,
        openMinute: 0,
        closeHour: 17,
        closeMinute: 0,
        weekdays: weekdays,
        createdAt: now,
        updatedAt: now,
      ),
    ];
  }
}
