package com.matthey.pmm.toms.service.conversion;

import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.matthey.pmm.toms.model.DatabaseFile;
import com.matthey.pmm.toms.model.Reference;
import com.matthey.pmm.toms.model.User;
import com.matthey.pmm.toms.repository.DatabaseFileRepository;
import com.matthey.pmm.toms.repository.ReferenceRepository;
import com.matthey.pmm.toms.repository.UserRepository;
import com.matthey.pmm.toms.transport.DatabaseFileTo;
import com.matthey.pmm.toms.transport.ImmutableDatabaseFileTo;

@Service
@Transactional
public class DatabaseFileConverter extends EntityToConverter<DatabaseFile, DatabaseFileTo>{
	@Autowired
	private UserRepository userRepo;

	@Autowired
	private ReferenceRepository refRepo;
	
	@Autowired
	private DatabaseFileRepository entityRepo;
	
	@Override
	public UserRepository userRepo() {
		return userRepo;
	}
	
	@Override
	public ReferenceRepository refRepo() {
		return refRepo;
	}
	
	@Override
	public DatabaseFileTo toTo (DatabaseFile entity) {
		return ImmutableDatabaseFileTo.builder()
				.id(entity.getId())
				.fileContent(Base64.getEncoder().encodeToString(entity.getFileContent()))
				.idFileType(entity.getFileType().getId())
				.name(entity.getName())
				.path(entity.getPath())
				.createdAt(formatDateTime(entity.getCreatedAt()))
				.idCreatedByUser(entity.getCreatedByUser().getId())
				.lastUpdate(formatDateTime(entity.getLastUpdate()))
				.idUpdatedByUser(entity.getUpdatedByUser().getId())
				.idLifecycle(entity.getLifecycleStatus().getId())
				.build();
	}
	
	@Override
	@Transactional
	public DatabaseFile toManagedEntity (DatabaseFileTo to) {
		Date createdAt = parseDateTime(to, to.createdAt());
		Date lastUpdate = parseDateTime(to, to.lastUpdate());
		byte[] fileContent = Base64.getDecoder().decode(to.fileContent());
		User createdBy = loadUser (to, to.idCreatedByUser());
		User updatedBy = loadUser (to, to.idUpdatedByUser());
		Reference fileType = loadRef(to, to.idFileType());
		Reference lifecycleStatus = loadRef(to, to.idLifecycle());
		
		Optional<DatabaseFile> existingEntity = entityRepo.findById(to.id());
		if (existingEntity.isPresent()) {
			existingEntity.get().setCreatedAt(createdAt);
			existingEntity.get().setCreatedByUser(createdBy);
			existingEntity.get().setFileContent(fileContent);
			existingEntity.get().setFileType(fileType);
			existingEntity.get().setLastUpdate(lastUpdate);
			existingEntity.get().setUpdatedByUser(updatedBy);
			existingEntity.get().setPath(to.path());
			existingEntity.get().setName(to.name());	
			existingEntity.get().setLifecycleStatus(lifecycleStatus);
			return existingEntity.get();
		}
		DatabaseFile newEntity = new DatabaseFile(to.name(), to.path(), fileType, lifecycleStatus, fileContent, createdAt, createdBy, lastUpdate, updatedBy);
		newEntity.setId(to.id());
		newEntity = entityRepo.save(newEntity);
		return newEntity;
	}
}
