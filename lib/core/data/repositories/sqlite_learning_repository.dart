import '../../domain/models/learning_resource.dart';
import '../local/app_database.dart';
import 'learning_repository.dart';
class SqliteLearningRepository implements LearningRepository {
 SqliteLearningRepository({AppDatabase? database}):_db=database??AppDatabase.instance;final AppDatabase _db;
 @override Future<void> create(LearningResource r) async=>(await _db.database).insert('learning_resources',r.toMap());
 @override Future<void> update(LearningResource r) async {final n=await (await _db.database).update('learning_resources',r.toMap(),where:'id = ?',whereArgs:[r.id]);if(n==0)throw StateError('Learning resource not found: ${r.id}');}
 @override Future<LearningResource?> getById(String id) async {final rows=await (await _db.database).query('learning_resources',where:'id = ?',whereArgs:[id],limit:1);return rows.isEmpty?null:LearningResource.fromMap(rows.first);}
 @override Future<List<LearningResource>> list({String? query,String? category,LearningContentType? type,List<String>? tags,bool? favoritesOnly}) async {final w=<String>[];final a=<Object?>[];if(query!=null&&query.trim().isNotEmpty){w.add('(title LIKE ? OR body LIKE ? OR caption LIKE ?)');final q='%${query.trim()}%';a.addAll([q,q,q]);}if(category!=null&&category.trim().isNotEmpty){w.add('category = ?');a.add(category.trim());}if(type!=null){w.add('type = ?');a.add(type.name);}if(favoritesOnly==true)w.add('is_favorite = 1');final rows=await (await _db.database).query('learning_resources',where:w.isEmpty?null:w.join(' AND '),whereArgs:a,orderBy:'updated_at DESC');final values=rows.map(LearningResource.fromMap).toList();return tags==null||tags.isEmpty?values:values.where((r)=>tags.every(r.tags.contains)).toList();}
 @override Future<void> delete(String id) async=>(await _db.database).delete('learning_resources',where:'id = ?',whereArgs:[id]);
}