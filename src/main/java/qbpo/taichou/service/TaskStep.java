package qbpo.taichou.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import qbpo.taichou.Constants;
import qbpo.taichou.repo.FileDefinition;
import qbpo.taichou.repo.Task;
import qbpo.taichou.repo.FileDefinition.Type;

public class TaskStep implements Tasklet {

	private Log log = LogFactory.getLog(TaskStep.class);
	
	Task task; 

	public Task getTask() {
		return task;
	}

	public TaskStep setTask(Task task) {
		this.task = task;
		return this;
	}

	private String getExtension(FileDefinition fileDefinition) {
		String answer = null;
		if (fileDefinition.getType() == Type.SPARSE_SVM)
			answer = ".svm";
		else if (fileDefinition.getType() == Type.DENSE_VECTOR)
			answer = ".csv";
		else if (fileDefinition.getType() == Type.DENSE_EXCEL_CSV)
			answer = ".csv";

		return answer;
	}

	public List<String> getFilePaths(String fileDatasetPath, List<FileDefinition> fileDefinitions) {
		List<String> answer = new ArrayList<>(fileDefinitions.size());

		for (FileDefinition fileDefinition : fileDefinitions) {

			String fileExtension = getExtension(fileDefinition);

			String filePath = String.join("", 
					fileDatasetPath, "/", fileDefinition.getName(), fileExtension);

			answer.add(filePath);
		}
		return answer;
	}
	
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

		// get the data from step context 
		
		StepContext stepContext = chunkContext.getStepContext();

		if (!stepContext.getJobExecutionContext().containsKey(Constants.BATCH_KEY_FILE_DATASET_PATH)) {
			throw Utils.createAndLogError(log, "File dataset path not found in job execution context.");
		}

		String fileDatasetPath = stepContext.getJobExecutionContext().get(Constants.BATCH_KEY_FILE_DATASET_PATH).toString();

		// arrange data to be passed to Task
		
		List<String> inputFilePaths = getFilePaths(fileDatasetPath, task.getInputFileDefinitions());

		List<String> outputFilePaths = getFilePaths(fileDatasetPath, task.getOutputFileDefinitions());

		// finish task; get output
		
		String output = task.execute(inputFilePaths, outputFilePaths);

		// put output to step context
		
		stepContext.getStepExecution().getExecutionContext().putString(Constants.BATCH_KEY_STEP_OUTPUT, output);
		
		//stepContext.getStepExecutionContext().put(Constants.STEP_OUTPUT, output);
		
		return null;
	}



}
