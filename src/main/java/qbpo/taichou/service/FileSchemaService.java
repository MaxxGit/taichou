package qbpo.taichou.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import qbpo.taichou.repo.FileDataset;
import qbpo.taichou.repo.FileDatasetRepo;
import qbpo.taichou.repo.FileDefinition;
import qbpo.taichou.repo.FileDefinitionRepo;
import qbpo.taichou.repo.FileSchema;
import qbpo.taichou.repo.FileSchemaRepo;

@Service
public class FileSchemaService {

	private static final Log log = LogFactory.getLog(FileSchemaService.class);

	@Autowired
	FileSchemaRepo fileSchemaRepo;

	@Autowired
	FileDefinitionRepo fileDefinitionRepo;

	@Autowired
	FileDatasetRepo fileDatasetRepo;
	
	@Transactional(readOnly = true)
	public List<FileSchema> getFileSchemas () {
		List<FileSchema> answer = null;

		try {
			answer = fileSchemaRepo.findAll();
		} catch (Exception e) {
			Utils.logError(log, e, "Unable to get FileSchemas.");
			throw e;
		}

		return answer;
	}

	public FileSchema createNewFileSchema() {
		return FileSchema.newInstance();
	}

	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public FileSchema insertFileSchema(FileSchema fileSchema) {

		try {
			fileSchema = fileSchemaRepo.saveAndFlush(fileSchema);
		} catch (Exception e) {
			Utils.logError(log, e, "Unable to insert (new) FileSchema.");
			throw e;
		}

		return fileSchema;
	}
	
	@Transactional(readOnly = true)
	public FileSchema getFileSchema(FileSchema fileSchema) {
		if (fileSchema.getId() == null)
			return null;

		fileSchema = fileSchemaRepo.findOne(fileSchema.getId());
		return fileSchema;
	}

	public FileDefinition createNewFileDefinition(FileSchema fileSchema) {
		return FileDefinition.newInstance(fileSchema);
	}

	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public FileDefinition insertFileDefinition(FileDefinition fileDefinition) throws Exception  {
		if (fileDefinition.getId() != null
				&& fileDefinitionRepo.exists(fileDefinition.getId())) {
			Exception e = Utils.createAndLogError(log, "File Definition already exists.");
			throw e;
		}
		
		try {
			
			FileSchema fileSchema = fileDefinition.getFileSchema(); 

			fileSchema = fileSchemaRepo.findOne(fileSchema.getId());

			fileDefinition = fileDefinitionRepo.save(fileDefinition);

			Set<FileDefinition> fileDefinitions = fileSchema.getFileDefinitions();

			if (fileDefinitions.contains(fileDefinition)) {
				Exception e = Utils.createAndLogError(log, "File Schema already contains File Definition.");
				throw e;
			}

			fileDefinitions.add(fileDefinition);

			fileSchema = fileSchemaRepo.save(fileSchema);

			fileDatasetRepo.flush();
			fileSchemaRepo.flush();

		} catch (Exception e) {
			Utils.logError(log, e, "Unable to insert File Definition");
			throw e;
		}

		return fileDefinition;
	}

	@Transactional(rollbackFor=Exception.class)
	public FileDefinition updateFileDefinition(FileDefinition fileDefinition) throws Exception {
		if (!fileDefinitionRepo.exists(fileDefinition.getId())) {
			Exception e = Utils.createAndLogError(log, "File Schema does not contains File Definition.");
			throw e;
		}
		try {

			fileDefinition = fileDefinitionRepo.save(fileDefinition);

			fileDatasetRepo.flush();

		} catch (Exception e) {
			Utils.logError(log, e, "Unable to insert File Definition");
			throw e;
		}

		return fileDefinition;
	}
	
	@Transactional(readOnly = true)
	public FileDefinition getFileDefinition(FileDefinition fileDefinition) {
		if (fileDefinition.getId() == null)
			return null;
		
		fileDefinition = fileDefinitionRepo.findOne(fileDefinition.getId());
		
		return fileDefinition;
	}
	
	@Transactional(readOnly = true)
	public FileDefinition getFileDefinitionByName(String name) {
		if (name == null)
			return null;
		
		FileDefinition fileDefinition = fileDefinitionRepo.findByName(name);
		
		return fileDefinition;
	}
	
	@Transactional(readOnly = true)
	public FileDefinition getFileDefinitionByNameAndSchema(String name, FileSchema schema) {
		if (name == null)
			return null;
		
		FileDefinition fileDefinition = fileDefinitionRepo.findByNameAndFileSchema(name, schema);
		
		return fileDefinition;
	}

	public FileDataset createNewFileDataset(FileSchema fileSchema) {
		return FileDataset.newInstance(fileSchema);
	}

	@Transactional(readOnly = false)
	public FileDataset insertFileDataset(FileDataset fileDataset) throws Exception {
		if (fileDataset.getId() != null
				&& fileDatasetRepo.exists(fileDataset.getId())) {
			Exception e = Utils.createAndLogError(log, "File Dataset already exists.");
			throw e;
		}
		
		try {

			FileSchema fileSchema = fileDataset.getFileSchema(); 

			fileSchema = fileSchemaRepo.findOne(fileSchema.getId());

			fileDataset = fileDatasetRepo.save(fileDataset);

			Set<FileDataset> fileDatasets = fileSchema.getFileDatasets();

			if (fileDatasets.contains(fileDataset)) {
				Exception e = Utils.createAndLogError(log, "File Schema already contains File Dataset.");
				throw e;
			}

			fileDatasets.add(fileDataset);

			fileSchema = fileSchemaRepo.save(fileSchema);

			fileDatasetRepo.flush();
			fileSchemaRepo.flush();

		} catch (Exception e) {
			Utils.logError(log, e, "Unable to insert File Dataset");
			throw e;
		}

		return fileDataset;
	}

	@Transactional(readOnly = false)
	public FileDataset updateFileDataset(FileDataset fileDataset) throws Exception {
		if (!fileDatasetRepo.exists(fileDataset.getId())) {
			Exception e = Utils.createAndLogError(log, "File Schema does not contains File Dataset.");
			throw e;
		}
		try {

			fileDataset = fileDatasetRepo.save(fileDataset);

			fileDatasetRepo.flush();

			fileDataset = fileDatasetRepo.findOne(fileDataset.getId());

		} catch (Exception e) {
			Utils.logError(log, e, "Unable to insert File Dataset");
			throw e;
		}

		return fileDataset;
	}

	@Transactional(readOnly = true)
	public FileDataset getFileDataset(FileDataset fileDataset) {
		if (fileDataset.getId() == null)
			return null;
		
		fileDataset = fileDatasetRepo.findOne(fileDataset.getId());
		
		return fileDataset;
	}
	
	@Autowired
	ObjectMapper jacksonObjectMapper;
	
	@Transactional(readOnly = true)
	public void backup(String fileName) throws Exception{
		List<FileSchema> fileSchemas = this.getFileSchemas();
		
		try {
			jacksonObjectMapper.writeValue(new File(fileName), fileSchemas);
		} catch (IOException e) {
			Utils.logError(log, e, "Something wrong with backup.");
			throw e;
		}
	}
}
