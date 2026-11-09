import 'package:flutter_test/flutter_test.dart';
import 'package:td_yar/core/domain/services/json_archive_import_service.dart';

import 'package:td_yar/core/data/repositories/learning_repository.dart';
import 'package:td_yar/core/domain/models/learning_resource.dart';
import 'package:td_yar/core/domain/services/learning_library_service.dart';
import 'package:td_yar/core/domain/services/report_export_service.dart';
class _MemoryLearningRepository implements LearningRepository {
 final values=<String,LearningResource>{};
 @override Future<void> create(LearningResource r) async{values[r.id]=r;}
 @override Future<void> update(LearningResource r) async{values[r.id]=r;}
 @override Future<LearningResource?> getById(String id) async=>values[id];
 @override Future<List<LearningResource>> list({String? query,String? category,LearningContentType? type,List<String>? tags,bool? favoritesOnly}) async=>values.values.toList();
 @override Future<void> delete(String id) async{values.remove(id);}
}
void main(){
 test('learning library rejects empty resources',() async {final s=LearningLibraryService(_MemoryLearningRepository());final d=DateTime.utc(2026);final r=LearningResource(id:'1',title:' ',type:LearningContentType.text,createdAt:d,updatedAt:d);await expectLater(s.save(r),throwsArgumentError);});
 test('learning library persists text resources',() async {final repo=_MemoryLearningRepository();final s=LearningLibraryService(repo);final d=DateTime.utc(2026);await s.save(LearningResource(id:'1',title:'Basics',type:LearningContentType.text,body:'Lesson',createdAt:d,updatedAt:d));expect(repo.values['1']?.body,'Lesson');});
 test('CSV escaping handles quotes and commas',(){expect(ReportExportService.csvCell('EUR,"USD"'),'"EUR,""USD"""');});
}