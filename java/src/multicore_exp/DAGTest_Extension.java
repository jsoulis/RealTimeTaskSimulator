package multicore_exp;

import java.util.ArrayList;

import data.Task;
import data.TaskSet;
import generator.TaskGenerator;
import generator.TaskGenerator2;
import period_harmonizer.PeriodModifier;

public class DAGTest_Extension {

	public DAGTest_Extension(int numVertex)
	{		
		this.numVertex = numVertex;
	}
	
	/*
	 * This merges the task set into one task apparently, so we can look at peak density appropriately
	 * assume this is going to happen for every similar sequential Segment in our General DAG task model 
	 * as well
	*/
	public double getDensity(int seed, ArrayList<ArrayList<Integer>> nodeinfo)
	{
		// COMMENTED OUT THIS LINE BELOW
		//System.out.println(nodeinfo);
		/*********************/
		Param.NumProcessors = 8;
		Param.NumThreads_MAX = 8;
		/*********************/
		Param.scmin = 10;
		Param.scmax = 1000;
		Param.Period_MIN = 200;
		Param.Period_MAX = 1000;
		Param.NumSegments_MIN = numVertex;
		Param.NumSegments_MAX = numVertex;
		
		TaskGenerator generator = new TaskGenerator();
		generator.setRandomBeta(0.3, 2);
		generator.setFixedGamma(1.0);
		generator.setRandomAlpha(0, 0.5);
		generator.setFixedTaskNum(1);
		
		
		TaskSet taskSet = generator.GenerateTaskSet(seed, seed);
		TaskSet mergedTaskSet = new TaskSet();

		Task task = taskSet.get(0);
		
		Task mergedTask = new Task(nodeinfo.size(), task.getPeriod(), task.getDeadline());
		
		for (int i = 0; i < nodeinfo.size(); i++) //for each segment
		{
			for (int k = 0; k < task.getNumOptions(); k++) //for each thread
			{
				int maxExecutionTime = 0;
				for (int l = 0; l < k + 1; l++)
				{
					int executionSum = 0;
					for (int j = 0; j < nodeinfo.get(i).size(); j++)
					{
						int executionTime = task.getExecutionTimeOfThread(nodeinfo.get(i).get(j), k, l);
						if (executionTime > maxExecutionTime) maxExecutionTime = executionTime;

						executionSum += executionTime;
						//executionSum += task.getExecutionTimeOfThread(nodeinfo.get(i).get(j), k, l)* (1 + (j / 10.0));
					}
					mergedTask.setExecutionTime(i, k, l, executionSum);
				}
				mergedTask.segments[i].get(k).maxExecutionTime = maxExecutionTime;
			}
		}
		
		mergedTaskSet.add(mergedTask);
		
//		System.out.println(task);
//		System.out.println(mergedTask);
		
		// COMMENTED OUT
		//System.out.println(logic.RTAS.getPeakDensity(mergedTaskSet));		
		//System.out.println(logic.RTAS.getPeakDensity(taskSet));		
		
		
	
		return logic.RTAS.getPeakDensity(mergedTaskSet);		
		//return logic.NBG.getPeakDensity(mergedTaskSet, logic.NBG.Type.SINGLE);
		//return logic.NBG.getPeakDensity(mergedTaskSet, logic.NBG.Type.MAX);
	}
	
	public double getPeakDensity(int seed, ArrayList<ArrayList<Integer>> blocks)
	{
		
		/*
		 * TO DO:  For each individual task in the task set we will look at the optimal density. 
		 * The peak density this function returns should be the SUM of all the optimal densities calculated for
		 * each task. The optimal density for each task will be calculated using the approximate density graphs.
		 * The approximate density graphs will be used in the following way. We will add up the density graphs, in sequence or 
		 * in parallel. and we will have a final graph that represents the final DAG structure. WE will then plug in the task deadline to 
		 * this final graph and the final result will be the opStimal density for that task! This value is saved and then added to all the other values for the segments. 
		 */
		
		
		/*********************/
		Param.NumProcessors = 8;
		Param.NumThreads_MAX = 8;
		/*********************/
		int numVertex = 7;
		Param.scmin = 10;
		Param.scmax = 1000;
		Param.Period_MIN = 200;
		Param.Period_MAX = 1000;
		Param.NumSegments_MIN = numVertex;
		Param.NumSegments_MAX = numVertex;
		
		TaskGenerator generator = new TaskGenerator();
		generator.setRandomBeta(0.3, 2);
		generator.setFixedGamma(1.0);
		generator.setRandomAlpha(0, 0.5);
		generator.setFixedTaskNum(1);
		
		
		TaskSet taskSet = generator.GenerateTaskSet(seed, seed);
		Task task = taskSet.get(0);
		
		ArrayList<ArrayList<Double>> densityGraphs = new ArrayList<ArrayList<Double>>();
		densityGraphs = createApproximateDensityGraphs(task);
		
		ArrayList<ArrayList<ArrayList<Double>>> organizedGraphs = new ArrayList<ArrayList<ArrayList<Double>>>();
		organizedGraphs = organizeDensityGraphs(densityGraphs, blocks);
		
		ArrayList<ArrayList<Double>> intermediateVerticalSums = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> intermediateVerticalSum;
		ArrayList<Double> finalSum = new ArrayList<Double>();
		
		for(int i = 0; i<organizedGraphs.size(); i++)
		{
			intermediateVerticalSum = new ArrayList<Double>();
			intermediateVerticalSum = sumVerticalColumn(organizedGraphs.get(i));
			intermediateVerticalSums.add(intermediateVerticalSum);
		}
		
		finalSum = sumHorizontalRow(intermediateVerticalSums);
		
		return getYValueFromGraph(finalSum, task.getDeadline());
	}
	
	//tested
	public ArrayList<ArrayList<ArrayList<Double>>> organizeDensityGraphs(ArrayList<ArrayList<Double>> densityGraphs, ArrayList<ArrayList<Integer>> blocks )
	{
		ArrayList<ArrayList<ArrayList<Double>>> shaped = new ArrayList<ArrayList<ArrayList<Double>>>();
		
		ArrayList<ArrayList<Double>> group;
		ArrayList<Double> graph;
		int k = 0;
		for(int i = 0; i<blocks.size(); i++)
		{
			group = new ArrayList<ArrayList<Double>>();
			for (int j=0; j<blocks.get(i).size(); j++)
			{
				group.add(densityGraphs.get(k));
				k++;
			}
			
			shaped.add(group);
		}
		
		return shaped;
	}
	
	//tested
	public ArrayList<ArrayList<Double>> createApproximateDensityGraphs(Task task)
	{
		
		//TO DO: Tuesday - implement this function. I should be able to take information for each Segment from functions
		//outlined in Task.java
		
		ArrayList<ArrayList<Double>> densityGraphs = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> filler; 
		
		double densityLow = 0.0;
		double densityHigh = 0.0;
		double taskDeadline = 0.0;
		double shortestDeadline = 0.0;
	
		for (int i = 0; i < task.getNumSegments(); i++)
		{
			shortestDeadline = task.getMaxExecutionTimeOfSegment(i, 7);
			//System.out.println(shortestDeadline);
			densityHigh = task.getTotalExecutionTimeOfSegment(i, 7) / shortestDeadline;
			//System.out.println(densityHigh);
			//System.out.println("why is densityHigh always teh same value of 8");
			taskDeadline = task.getDeadline();
			densityLow = task.getTotalExecutionTimeOfSegment(i, 0) / taskDeadline;
			
			filler = new ArrayList<Double>();
			filler.add(shortestDeadline);
			filler.add(densityHigh);
			filler.add(taskDeadline);
			filler.add(densityLow);
			
			densityGraphs.add(filler);
		}
		
		return densityGraphs;
		
	}
	
	//tested
	public ArrayList<Double> sumVertically(ArrayList<Double> graph_one, ArrayList<Double> graph_two)
	{
		ArrayList<Double> sum = new ArrayList<Double>();
		sum.add(0.0);
		sum.add(0.0);
		sum.add(0.0);
		sum.add(0.0);
		
		double X1 = 0.0;
		double Y1 = 0.0;
		double X2 = 0.0;
		double Y2 = 0.0;
		
		//left x and y value of graph
		if(graph_one.get(0) > graph_two.get(0))
		{
			X1 = graph_one.get(0);
			Y1 = graph_one.get(1) + getYValueFromGraph(graph_two, X1);
		}
		else if(graph_one.get(0) < graph_two.get(0))
		{
			X1 = graph_two.get(0);
			Y1 = graph_two.get(1) + getYValueFromGraph(graph_one, X1);
		}
		else 
		{
			X1 = graph_one.get(0);
			Y1 = graph_one.get(1) + graph_two.get(1);
		}
		
		
		//right x and y value of graph 
		if(graph_one.get(2) > graph_two.get(2))
		{
			X2 = graph_two.get(2);
			Y2 = graph_two.get(3) + getYValueFromGraph(graph_one, X2);
		}
		else if(graph_one.get(2) < graph_two.get(2))
		{
			X2 = graph_one.get(2);
			Y2 = graph_one.get(3) + getYValueFromGraph(graph_two, X2);
		}
		else 
		{
			X2 = graph_one.get(2);
			Y2 = graph_one.get(3) + graph_two.get(3);
		}
		
		
		sum.set(0, X1);
		sum.set(1, Y1);
		sum.set(2, X2);
		sum.set(3, Y2);
		
		return sum;
	}
	
	//tested
	public ArrayList<Double> sumVerticalColumn(ArrayList<ArrayList<Double>> densityGraphs)
	{
		ArrayList<Double> interSumVert = new ArrayList<Double>();
		interSumVert = densityGraphs.get(0);
		for (int i = 0; i < densityGraphs.size()-1; i++)
		{
			
			interSumVert = sumVertically(interSumVert, densityGraphs.get(i+1));
			
		}
		
		return interSumVert;
	}
	
	//tested
	public ArrayList<Double> sumHorizontally(ArrayList<Double> graph_one, ArrayList<Double> graph_two)
	{
		ArrayList<Double> sum = new ArrayList<Double>();
		ArrayList<Double> inverseSum = new ArrayList<Double>();
		ArrayList<Double> inverseGraph_one = new ArrayList<Double>();
		ArrayList<Double> inverseGraph_two = new ArrayList<Double>();
		
		inverseGraph_one = getInverseGraph(graph_one);
		inverseGraph_two = getInverseGraph(graph_two);
		
		inverseSum = sumVertically(inverseGraph_one, inverseGraph_two);
		
		sum = getInverseGraph(inverseSum);
		
		return sum;
	}
	
	//tested
	public ArrayList<Double> sumHorizontalRow(ArrayList<ArrayList<Double>> densityGraphs)
	{
		ArrayList<Double> interSum = new ArrayList<Double>();
		interSum = densityGraphs.get(0);
		for (int i = 0; i < densityGraphs.size()-1; i++)
		{
			
			interSum = sumHorizontally(interSum, densityGraphs.get(i+1));
			
		}
		
		return interSum;
	}
	
	//tested
	public double getYValueFromGraph(ArrayList<Double> graph, double deadline)
	{
		double result = 0.0;
		double slope = 0.0;
		slope = ((graph.get(3) - graph.get(1)) / (graph.get(2) - graph.get(0)));
		result = slope*deadline - slope*graph.get(0) + graph.get(1);
		return result;
	}
	
	//tested
	public ArrayList<Double> getInverseGraph(ArrayList<Double> graph)
	{
		ArrayList<Double> inverseGraph = new ArrayList<Double>();
		inverseGraph.add(graph.get(1));
		inverseGraph.add(graph.get(0));
		inverseGraph.add(graph.get(3));
		inverseGraph.add(graph.get(2));
		
		return inverseGraph;
	}
	
	private int numVertex;
	
	public static void main(String args[])
	{
		/*********************/
		Param.NumProcessors = 8;
		Param.NumThreads_MAX = 8;
		/*********************/
		int numVertex = 7;
		Param.scmin = 10;
		Param.scmax = 1000;
		Param.Period_MIN = 200;
		Param.Period_MAX = 1000;
		Param.NumSegments_MIN = numVertex;
		Param.NumSegments_MAX = numVertex;
		
		TaskGenerator generator = new TaskGenerator();
		generator.setRandomBeta(0.3, 2);
		generator.setFixedGamma(1.0);
		generator.setRandomAlpha(0, 0.5);
		generator.setFixedTaskNum(1);
		
		int seed = 10;
		TaskSet taskSet = generator.GenerateTaskSet(seed, seed);
		Task task = taskSet.get(0);
		
		System.out.println(task.getDeadline());
		System.out.println(task.getNumOptions());
		System.out.println();
		System.out.println(task.getMaxExecutionTimeOfSegment(0, 7));
		System.out.println(task.getMaxExecutionTimeOfSegment(1, 7));
		System.out.println();
		System.out.println(task.getMaxExecutionTimeOfSegment(0, 6));
		System.out.println(task.getMaxExecutionTimeOfSegment(0, 5));
		System.out.println(task.getMaxExecutionTimeOfSegment(0, 4));
		System.out.println(task.getMaxExecutionTimeOfSegment(0, 3));
		System.out.println(task.getMaxExecutionTimeOfSegment(0, 2));
		System.out.println(task.getMaxExecutionTimeOfSegment(0, 1));
		System.out.println(task.getMaxExecutionTimeOfSegment(0, 0));
		System.out.println(task.getTotalExecutionTime(0, 7));
		
		
		System.out.println();
		System.out.println("End of Task Tests");
		
		DAGTest_Extension t = new DAGTest_Extension(numVertex);
		System.out.println("hello");
		ArrayList<Double> graph_one = new ArrayList<Double>();
		ArrayList<Double> graph_two = new ArrayList<Double>();
		graph_one.add(0.0);
		graph_one.add(1.0);
		graph_one.add(3.0);
		graph_one.add(5.0);
		
		graph_two.add(0.0);
		graph_two.add(0.0);
		graph_two.add(4.0);
		graph_two.add(5.0);
		
		ArrayList<Double> sum = new ArrayList<Double>();
		
		sum = t.sumHorizontally(graph_one, graph_two);
		
		for (int i = 0; i < sum.size(); i++)
		{
			System.out.println(sum.get(i));
		}
		
		ArrayList<ArrayList<Double>> densityGraphs = new ArrayList<ArrayList<Double>>();
		densityGraphs = t.createApproximateDensityGraphs(task);
		
		System.out.println(densityGraphs);
		
		ArrayList<Double> sumGraphs = new ArrayList<Double>();
		
		//code to sum entire task up horizontally
		ArrayList<Double> interSum = new ArrayList<Double>();
		interSum = densityGraphs.get(0);
		for (int i = 0; i < densityGraphs.size()-1; i++)
		{
			
			interSum = t.sumHorizontally(interSum, densityGraphs.get(i+1));
			
		}
		System.out.println();
		System.out.println(interSum);
		System.out.println(t.sumHorizontalRow(densityGraphs));
		System.out.println(t.getYValueFromGraph(interSum, task.getDeadline()));
		
		//code to sum entire task up vertically
		ArrayList<Double> interSumVert = new ArrayList<Double>();
		interSumVert = densityGraphs.get(0);
		for (int i = 0; i < densityGraphs.size()-1; i++)
		{
			
			interSumVert = t.sumVertically(interSumVert, densityGraphs.get(i+1));
			
		}
		System.out.println();
		System.out.println(interSumVert);
		System.out.println(t.sumVerticalColumn(densityGraphs));
		System.out.println(t.getYValueFromGraph(interSumVert, task.getDeadline()));
		
		ArrayList<ArrayList<ArrayList<Double>>> organizedDensityGraphs = new ArrayList<ArrayList<ArrayList<Double>>>();
		
		ArrayList<ArrayList<Integer>> blocks = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> block1 = new ArrayList<Integer>();
		ArrayList<Integer> block2 = new ArrayList<Integer>();
		ArrayList<Integer> block3 = new ArrayList<Integer>();
		ArrayList<Integer> block4 = new ArrayList<Integer>();
		block1.add(1);
		block2.add(3);
		block2.add(4);
		block2.add(5);
		block3.add(6);
		blocks.add(block1);
		blocks.add(block2);
		blocks.add(block3);
		
		
		
		ArrayList<ArrayList<Integer>> blocks2 = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> blockA = new ArrayList<Integer>();
		ArrayList<Integer> blockB = new ArrayList<Integer>();
		ArrayList<Integer> blockC = new ArrayList<Integer>();
		ArrayList<Integer> blockD = new ArrayList<Integer>();
		ArrayList<Integer> blockE = new ArrayList<Integer>();
		ArrayList<Integer> blockF = new ArrayList<Integer>();
		ArrayList<Integer> blockG = new ArrayList<Integer>();
		
		blockA.add(1);
		blockB.add(2);
		blockC.add(3);
		blockD.add(4);
		blockE.add(5);
		blockF.add(6);
		blockG.add(7);
		
		blocks2.add(blockA);
		blocks2.add(blockB);
		blocks2.add(blockC);
		blocks2.add(blockD);
		blocks2.add(blockE);
		blocks2.add(blockF);
		blocks2.add(blockG);
		
		
		ArrayList<ArrayList<Integer>> blocks3 = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> blockZ = new ArrayList<Integer>();
		blockZ.add(1);
		blockZ.add(1);
		blockZ.add(1);
		blockZ.add(1);
		blockZ.add(1);
		blockZ.add(1);
		blockZ.add(1);
		blocks3.add(blockZ);
		
		
		organizedDensityGraphs = t.organizeDensityGraphs(densityGraphs, blocks);
		System.out.println(organizedDensityGraphs);
		
		
		//These tests prove that getPeakDensity function is working properly.... *sigh*
		ArrayList<Double> temp = t.sumVerticalColumn(organizedDensityGraphs.get(1));
		System.out.println(temp);
		System.out.println(t.getYValueFromGraph(temp, task.getDeadline()));
		ArrayList<Double> temp2 = t.sumHorizontally(organizedDensityGraphs.get(0).get(0), temp);
		System.out.println(t.getYValueFromGraph(temp2, task.getDeadline()));
		ArrayList<Double> temp3 = t.sumHorizontally(organizedDensityGraphs.get(2).get(0), temp2);
		System.out.println(t.getYValueFromGraph(temp3, task.getDeadline()));
		
		
		System.out.println(t.getPeakDensity(seed, blocks));
		System.out.println(t.getPeakDensity(seed, blocks2));
		System.out.println(t.getPeakDensity(seed, blocks3));
		
	}

}