package com.matthey.pmm.toms.service.mock.testdata;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.DatabaseFileTo;
import com.matthey.pmm.toms.transport.ImmutableDatabaseFileTo;

public enum TestDatabaseFile {
		TEST_DATABASE_FILE_1 (100000l, "FileName1", "/email/attachments/", DefaultReference.FILE_TYPE_TXT, 
				DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, "File Content Line 2 Line 3  Line 4",
				"2000-01-01 08:00:00", TestUser.JENS_WAECHTER, "2000-01-01 08:00:00", TestUser.JENS_WAECHTER),
		TEST_DATABASE_FILE_2 (100001l, "FileName2", "/email/attachments/", DefaultReference.FILE_TYPE_TXT, 
				DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, "Another File Content",
				"2000-01-01 08:00:00", TestUser.JENS_WAECHTER, "2000-01-01 08:00:00", TestUser.JENS_WAECHTER),
		TEST_DATABASE_FILE_3 (100002l, "FileName3", "/email/attachments/", DefaultReference.FILE_TYPE_TXT, 
				DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE,"More files, more attachments, more content",
				"2000-01-01 08:00:00", TestUser.JENS_WAECHTER, "2000-01-01 08:00:00", TestUser.JENS_WAECHTER),
		TEST_DATABASE_FILE_4 (100003l, "FileName4", "/email/attachments/", DefaultReference.FILE_TYPE_TXT, 
				DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, "More file content",
				"2000-01-01 08:00:00", TestUser.JENS_WAECHTER, "2000-01-01 08:00:00", TestUser.JENS_WAECHTER),
		TEST_DATABASE_FILE_5 (100004l, "FileName5", "/other/", DefaultReference.FILE_TYPE_PDF, 
				DefaultReference.LIFECYCLE_STATUS_AUTHORISED_ACTIVE, "File Content Line 2Line 3 Line 4",
				"2000-01-01 08:00:00", TestUser.JENS_WAECHTER, "2000-01-01 08:00:00", TestUser.JENS_WAECHTER),
		
	;
	
	private final DatabaseFileTo databaseFile;
	
	private TestDatabaseFile (long id, String name, String path,
			DefaultReference fileType, DefaultReference lifecycleStatus,  
			String fileContent,
			String createdAt, TestUser createdBy,
			String updatedAt, TestUser updatedBy) {
		try {
			this.databaseFile = ImmutableDatabaseFileTo.builder()
					.id(id)
					.fileContent(Base64.getEncoder().encodeToString(fileContent.getBytes("UTF-8")))
					.idFileType(fileType.getEntity().id())
					.name(name)
					.path(path)
					.idLifecycle(lifecycleStatus.getEntity().id())
					.lastUpdate(updatedAt)
					.createdAt(createdAt)
					.idUpdatedByUser(updatedBy.getEntity().id())
					.idCreatedByUser(createdBy.getEntity().id())
					.build();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException (e);
		}
	}
	
	public DatabaseFileTo getEntity () {
		return databaseFile;
	}
	
	public static List<DatabaseFileTo> asList () {
		return Arrays.asList(TestDatabaseFile.values())
				.stream().map(TestDatabaseFile::getEntity).collect(Collectors.toList());
	}
	
	public static List<DatabaseFileTo> asListByIds (List<Long> ids) {
		return Arrays.asList(TestDatabaseFile.values())
				.stream()
				.map(TestDatabaseFile::getEntity)
				.filter(x -> ids.contains(x.id()))
				.collect(Collectors.toList());
	}
}