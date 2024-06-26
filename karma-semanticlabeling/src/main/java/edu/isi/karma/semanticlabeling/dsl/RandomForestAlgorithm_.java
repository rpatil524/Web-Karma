package edu.isi.karma.semanticlabeling.dsl;

import java.io.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.trees.RandomForest;
import java.io.Serializable;


/** RandomForest Classification
* @author rutujarane, Bidisha Das Baksi (bidisha.bksh@gmail.com)
*/

public class RandomForestAlgorithm_ implements Serializable{
	
	String trainingDatasetNamef = "train_data_set.arff";
	Evaluation eval;
	public RandomForestAlgorithm_() throws Exception{
		Instances trainDatasetf = getDataSet(trainingDatasetNamef);
		this.eval = new Evaluation(trainDatasetf);
	}


	static Logger logger = LogManager.getLogger(RandomForestAlgorithm_.class.getName());
	public RandomForest RandomForestAlgorithm_create() throws Exception{
		
		logger.info("Welcome to Algorithm");
		
		String trainingDatasetName = "train_data_set.arff";
		String testingDatasetName = "libsvm_test.arff";
		// Load and parse the data file, converting it to a DataFrame.
		Instances trainDataset = getDataSet(trainingDatasetName);
		Instances testDataset = getDataSet(trainingDatasetName);
		logger.info("Loaded both the datasets");
		RandomForest forest=new RandomForest();
		forest.setNumIterations(200);
		logger.info("Created object");
		forest.setPrintClassifiers(true);
		forest.buildClassifier(trainDataset);
		logger.info("Built classifier");
		Evaluation eval = new Evaluation(trainDataset);
		eval.evaluateModel(forest, testDataset);

		logger.info("Evaluated model");

		logger.info("** Decision Tress Evaluation with Datasets **");
		logger.info(eval.toSummaryString());
		System.out.print(" the expression for the input data as per alogorithm is ");
		logger.info("Storing to file:");
		logger.info("Successfully wrote to the file.");
		logger.info("matrix:"+eval.toMatrixString());
		logger.info(eval.toClassDetailsString());

		return forest;
	
	}	

	public Instances getDataSet(String fileName) throws Exception{
		DataSource source = new DataSource(fileName);
		Instances dataset = source.getDataSet();
		dataset.setClassIndex(dataset.numAttributes() - 1);
		logger.info("Set class index of dataset");
		return dataset;
	}

	public RandomForest testModel(String testFile, String modelFile) throws Exception{

		String trainingDatasetName = "libsvm_.arff";
		// Load and parse the data file, converting it to a DataFrame.
		Instances trainDataset = getDataSet(trainingDatasetName);
		Instances testDataset = getDataSet(testFile);
		logger.info("Got test dataset");
		FileInputStream fileIS = new FileInputStream(modelFile);
		ObjectInputStream file = new ObjectInputStream(fileIS);
		RandomForest rf = (RandomForest) (file).readObject();
		logger.info("Read the model file");

		Evaluation eval = new Evaluation(trainDataset);
		eval.evaluateModel(rf, testDataset);
		logger.info("Evaluated model");

		logger.info("** Decision Tress Evaluation with Datasets **");
		logger.info(eval.toSummaryString());
		System.out.print(" the expression for the input data as per alogorithm is ");
		logger.info(rf);
		logger.info("matrix:"+eval.toMatrixString());
		logger.info(eval.toClassDetailsString());
		return rf;

	}
}