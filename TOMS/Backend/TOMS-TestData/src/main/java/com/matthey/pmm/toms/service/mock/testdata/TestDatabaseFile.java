package com.matthey.pmm.toms.service.mock.testdata;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import com.matthey.pmm.toms.enums.v1.DefaultReference;
import com.matthey.pmm.toms.transport.DatabaseFileTo;
import com.matthey.pmm.toms.transport.ImmutableDatabaseFileTo;

public enum TestDatabaseFile {
		TEST_DATABASE_FILE_1 (1000000l, "FileName1", "/email/attachments/", DefaultReference.FILE_TYPE_TXT, "File Content\n Line 2\nLine 3\n Line 4",
				"2000-01-01 08:00:00", TestUser.JENS_WAECHTER, "2000-01-01 08:00:00", TestUser.JENS_WAECHTER),
		TEST_DATABASE_FILE_2 (1000001l, "FileName2", "/email/attachments/", DefaultReference.FILE_TYPE_TXT, "Another File Content\n\n",
				"2000-01-01 08:00:00", TestUser.JENS_WAECHTER, "2000-01-01 08:00:00", TestUser.JENS_WAECHTER),
		TEST_DATABASE_FILE_3 (1000002l, "FileName3", "/email/attachments/", DefaultReference.FILE_TYPE_TXT, "More files, more attachments, more content",
				"2000-01-01 08:00:00", TestUser.JENS_WAECHTER, "2000-01-01 08:00:00", TestUser.JENS_WAECHTER),
		TEST_DATABASE_FILE_4 (1000003l, "FileName4", "/email/attachments/", DefaultReference.FILE_TYPE_TXT, "More file content",
				"2000-01-01 08:00:00", TestUser.JENS_WAECHTER, "2000-01-01 08:00:00", TestUser.JENS_WAECHTER),
		TEST_DATABASE_FILE_5 (1000004l, "FileName5", "/other/", DefaultReference.FILE_TYPE_PDF, "File Content\n Line 2\nLine 3\n Line 4",
				"2000-01-01 08:00:00", TestUser.JENS_WAECHTER, "2000-01-01 08:00:00", TestUser.JENS_WAECHTER),
	;
	
	private final DatabaseFileTo databaseFile;
	
	private TestDatabaseFile (long id, String name, String path,
			DefaultReference fileType,  String fileContent,
			String createdAt, TestUser createdBy,
			String updatedAt, TestUser updatedBy) {
		this.databaseFile = ImmutableDatabaseFileTo.builder()
				.id(id)
				.fileContent(Base64.getEncoder().encodeToString(fileContent.getBytes(Charset.forName("UTF-8"))))
				.idFileType(fileType.getEntity().id())
				.name(name)
				.path(path)
				.lastUpdate(updatedAt)
				.createdAt(createdAt)
				.idUpdatedByUser(updatedBy.getEntity().id())
				.idCreatedByUser(createdBy.getEntity().id())
				.build();
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