class TradingAccount {
 const TradingAccount({required this.id,required this.name,required this.currency,required this.createdAt,required this.updatedAt,this.initialBalance=0,this.isArchived=false,this.notes=''});
 final String id,name,currency,notes;final double initialBalance;final bool isArchived;final DateTime createdAt,updatedAt;
 Map<String,Object?> toMap()=>{'id':id,'name':name,'currency':currency,'initial_balance':initialBalance,'is_archived':isArchived?1:0,'notes':notes,'created_at':createdAt.toUtc().toIso8601String(),'updated_at':updatedAt.toUtc().toIso8601String()};
 factory TradingAccount.fromMap(Map<String,Object?> m)=>TradingAccount(id:m['id'] as String,name:m['name'] as String,currency:m['currency'] as String,initialBalance:(m['initial_balance'] as num).toDouble(),isArchived:(m['is_archived'] as num)==1,notes:m['notes'] as String? ?? '',createdAt:DateTime.parse(m['created_at'] as String).toLocal(),updatedAt:DateTime.parse(m['updated_at'] as String).toLocal());
}
class AuditLogEntry {const AuditLogEntry({required this.id,required this.eventType,required this.entityType,required this.entityId,required this.occurredAt,this.detailsJson='{}'});final String id,eventType,entityType,entityId,detailsJson;final DateTime occurredAt;}
abstract interface class AppInfrastructureRepository {
 Future<void> saveAccount(TradingAccount account);Future<TradingAccount?> getAccount(String id);Future<List<TradingAccount>> listAccounts({bool includeArchived=false});Future<void> archiveAccount(String id,bool archived,DateTime updatedAt);Future<void> deleteAccount(String id);
 Future<String?> getSetting(String key);Future<void> setSetting(String key,String value,DateTime updatedAt);Future<Map<String,String>> getSettings();Future<void> deleteSetting(String key);
 Future<void> appendAuditEvent(AuditLogEntry event);Future<List<AuditLogEntry>> getAuditEvents({DateTime? from,DateTime? to,String? entityType,int limit=200});
}