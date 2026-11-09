import 'dart:io';
import 'dart:typed_data';
import '../../data/local/app_database.dart';
class DatabaseBackupService {
 DatabaseBackupService({AppDatabase? database}):_db=database??AppDatabase.instance;final AppDatabase _db;
 Future<File> createBackup(String destinationPath) async {if(!destinationPath.toLowerCase().endsWith('.db'))throw ArgumentError('Backup path must end with .db.');final source=File(await _db.databaseFile);await _db.database;await _db.close();final target=File(destinationPath);await target.parent.create(recursive:true);return source.copy(target.path);}
 Future<void> restoreBackup(String backupPath) async {final source=File(backupPath);if(!await source.exists())throw FileSystemException('Backup file not found',backupPath);final h=await source.open();late Uint8List header;try{header=await h.read(16);}finally{await h.close();}const sig=[83,81,76,105,116,101,32,102,111,114,109,97,116,32,51,0];if(header.length!=16||List<int>.generate(16,(i)=>header[i]).toString()!=sig.toString())throw const FormatException('Invalid SQLite database backup.');await _db.close();final target=File(await _db.databaseFile);await source.copy(target.path);await _db.close();await _db.database;}
}