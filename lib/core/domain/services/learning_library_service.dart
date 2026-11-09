import '../../data/repositories/learning_repository.dart';
import '../models/learning_resource.dart';
class LearningLibraryService {
 const LearningLibraryService(this._repo);final LearningRepository _repo;
 Future<void> save(LearningResource r) async {if(r.id.trim().isEmpty)throw ArgumentError('Resource id is required.');if(r.title.trim().isEmpty)throw ArgumentError('Resource title is required.');final text=r.body.trim().isNotEmpty,media=r.mediaPath?.trim().isNotEmpty??false;if(!text&&!media)throw ArgumentError('Resource must contain text or media.');if((r.type==LearningContentType.image||r.type==LearningContentType.video)&&!media)throw ArgumentError('Image/video resources require a media reference.');if(r.type==LearningContentType.text&&!text)throw ArgumentError('Text resources require body content.');if(await _repo.getById(r.id)==null){await _repo.create(r);}else{await _repo.update(r);}}
 Future<List<LearningResource>> search({String? query,String? category,LearningContentType? type,List<String>? tags,bool favoritesOnly=false})=>_repo.list(query:query,category:category,type:type,tags:tags,favoritesOnly:favoritesOnly?true:null);
 Future<void> delete(String id)=>_repo.delete(id);
}