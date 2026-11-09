import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/domain/models/trading_session.dart';
import 'package:td_yar/core/domain/services/trading_sessions.dart';

void main() {
  final created = DateTime.utc(2026, 1, 1);

  TradingSessionDefinition session({
    required String id,
    required String name,
    required String timezoneId,
    required int openHour,
    required int closeHour,
    List<int> weekdays = const [1, 2, 3, 4, 5],
  }) => TradingSessionDefinition(
        id: id,
        name: name,
        timezoneId: timezoneId,
        openHour: openHour,
        openMinute: 0,
        closeHour: closeHour,
        closeMinute: 0,
        weekdays: weekdays,
        createdAt: created,
        updatedAt: created,
      );

  test('session status follows each market timezone and daylight saving', () {
    final engine = TradingSessions();
    final london = session(
      id: 'london', name: 'London',
      timezoneId: 'Europe/London', openHour: 8, closeHour: 17,
    );
    final newYork = session(
      id: 'new-york', name: 'New York',
      timezoneId: 'America/New_York', openHour: 8, closeHour: 17,
    );

    final winter = engine.statusesAt(
      instant: DateTime.utc(2026, 1, 5, 14),
      sessions: [london, newYork],
    );
    expect(winter.firstWhere((item) => item.session.id == 'london').isOpen, isTrue);
    expect(winter.firstWhere((item) => item.session.id == 'new-york').isOpen, isTrue);

    final summer = engine.statusesAt(
      instant: DateTime.utc(2026, 7, 6, 7, 30),
      sessions: [london, newYork],
    );
    expect(summer.firstWhere((item) => item.session.id == 'london').isOpen, isTrue);
    expect(summer.firstWhere((item) => item.session.id == 'new-york').isOpen, isFalse);
  });

  test('London and New York overlap is calculated in UTC', () {
    final engine = TradingSessions();
    final overlaps = engine.overlapsBetween(
      from: DateTime.utc(2026, 1, 5),
      to: DateTime.utc(2026, 1, 6),
      sessions: [
        session(
          id: 'london', name: 'London',
          timezoneId: 'Europe/London', openHour: 8, closeHour: 17,
        ),
        session(
          id: 'new-york', name: 'New York',
          timezoneId: 'America/New_York', openHour: 8, closeHour: 17,
        ),
      ],
    );

    expect(overlaps, hasLength(1));
    expect(overlaps.single.startsAt, DateTime.utc(2026, 1, 5, 13));
    expect(overlaps.single.endsAt, DateTime.utc(2026, 1, 5, 17));
    expect(overlaps.single.duration, const Duration(hours: 4));
  });

  test('overnight sessions remain open after midnight local time', () {
    final engine = TradingSessions();
    final overnight = session(
      id: 'overnight', name: 'Overnight',
      timezoneId: 'UTC', openHour: 22, closeHour: 6,
      weekdays: const [1],
    );
    final status = engine.statusesAt(
      instant: DateTime.utc(2026, 1, 6, 2),
      sessions: [overnight],
    ).single;
    expect(status.isOpen, isTrue);
    expect(status.currentOpenAt, DateTime.utc(2026, 1, 5, 22));
    expect(status.currentCloseAt, DateTime.utc(2026, 1, 6, 6));
  });

  test('invalid session definitions are rejected', () {
    final invalid = session(
      id: 'bad', name: 'Bad',
      timezoneId: 'UTC', openHour: 8, closeHour: 8,
    );
    expect(invalid.validate, throwsArgumentError);
    expect(
      () => TradingSessions().validateDefinition(
        session(
          id: 'bad-zone', name: 'Bad zone',
          timezoneId: 'Not/AZone', openHour: 8, closeHour: 17,
        ),
      ),
      throwsArgumentError,
    );
  });

  test('default sessions include Sydney Tokyo London and New York', () {
    final values = TradingSessions.defaultSessions(timestamp: created);
    expect(values.map((item) => item.id).toSet(),
        {'sydney', 'tokyo', 'london', 'new-york'});
  });
}
