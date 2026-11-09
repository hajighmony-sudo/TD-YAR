import '../../domain/models/playbook.dart';

abstract interface class PlaybookRepository {
  Future<void> createPlaybook(Playbook playbook);

  Future<void> updatePlaybook(Playbook playbook);

  Future<Playbook?> getPlaybook(String id);

  Future<List<Playbook>> getPlaybooks();

  Future<void> deletePlaybook(String id);
}
