import 'dart:convert';

class TradingSessionDefinition {
  const TradingSessionDefinition({
    required this.id,
    required this.name,
    required this.timezoneId,
    required this.openHour,
    required this.openMinute,
    required this.closeHour,
    required this.closeMinute,
    required this.createdAt,
    required this.updatedAt,
    this.weekdays = const [1, 2, 3, 4, 5],
    this.isEnabled = true,
  });

  final String id;
  final String name;
  final String timezoneId;
  final int openHour;
  final int openMinute;
  final int closeHour;
  final int closeMinute;
  final List<int> weekdays;
  final bool isEnabled;
  final DateTime createdAt;
  final DateTime updatedAt;

  void validate() {
    if (id.trim().isEmpty || name.trim().isEmpty || timezoneId.trim().isEmpty) {
      throw ArgumentError('Session id, name and timezone are required.');
    }
    if (openHour < 0 || openHour > 23 ||
        closeHour < 0 || closeHour > 23 ||
        openMinute < 0 || openMinute > 59 ||
        closeMinute < 0 || closeMinute > 59) {
      throw ArgumentError('Session hours or minutes are outside valid ranges.');
    }
    if (openHour == closeHour && openMinute == closeMinute) {
      throw ArgumentError('A session cannot have identical open and close times.');
    }
    if (weekdays.isEmpty ||
        weekdays.any((day) => day < DateTime.monday || day > DateTime.sunday) ||
        weekdays.toSet().length != weekdays.length) {
      throw ArgumentError('Session weekdays must be unique values from 1 to 7.');
    }
  }

  Map<String, Object?> toMap() => {
        'id': id,
        'name': name,
        'timezone_id': timezoneId,
        'open_hour': openHour,
        'open_minute': openMinute,
        'close_hour': closeHour,
        'close_minute': closeMinute,
        'weekdays_json': jsonEncode(weekdays),
        'is_enabled': isEnabled ? 1 : 0,
        'created_at': createdAt.toUtc().toIso8601String(),
        'updated_at': updatedAt.toUtc().toIso8601String(),
      };

  factory TradingSessionDefinition.fromMap(Map<String, Object?> map) {
    final days = jsonDecode(map['weekdays_json'] as String? ?? '[1,2,3,4,5]')
        as List<dynamic>;
    return TradingSessionDefinition(
      id: map['id'] as String,
      name: map['name'] as String,
      timezoneId: map['timezone_id'] as String,
      openHour: (map['open_hour'] as num).toInt(),
      openMinute: (map['open_minute'] as num).toInt(),
      closeHour: (map['close_hour'] as num).toInt(),
      closeMinute: (map['close_minute'] as num).toInt(),
      weekdays: days.map((value) => (value as num).toInt()).toList(growable: false),
      isEnabled: (map['is_enabled'] as num? ?? 1) == 1,
      createdAt: DateTime.parse(map['created_at'] as String).toLocal(),
      updatedAt: DateTime.parse(map['updated_at'] as String).toLocal(),
    );
  }
}

class SessionOccurrence {
  const SessionOccurrence({
    required this.session,
    required this.opensAt,
    required this.closesAt,
  });

  final TradingSessionDefinition session;
  final DateTime opensAt;
  final DateTime closesAt;
}

class TradingSessionStatus {
  const TradingSessionStatus({
    required this.session,
    required this.isOpen,
    required this.currentOccurrence,
    required this.nextOccurrence,
  });

  final TradingSessionDefinition session;
  final bool isOpen;
  final SessionOccurrence? currentOccurrence;
  final SessionOccurrence? nextOccurrence;

  DateTime? get nextOpenAt =>
      isOpen ? currentOccurrence?.opensAt : nextOccurrence?.opensAt;
  DateTime? get nextCloseAt =>
      isOpen ? currentOccurrence?.closesAt : nextOccurrence?.closesAt;
}

class SessionOverlap {
  const SessionOverlap({
    required this.firstSessionId,
    required this.firstSessionName,
    required this.secondSessionId,
    required this.secondSessionName,
    required this.startsAt,
    required this.endsAt,
  });

  final String firstSessionId;
  final String firstSessionName;
  final String secondSessionId;
  final String secondSessionName;
  final DateTime startsAt;
  final DateTime endsAt;

  Duration get duration => endsAt.difference(startsAt);
}
